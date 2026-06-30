from dotenv import load_dotenv
from fastapi import APIRouter, Depends, HTTPException, UploadFile, File, status
from sqlalchemy.orm import Session
from database import get_db
from auth import hash_password, verify_password, create_token, get_current_user
import models
import schemas
import time
import random
import string
import os
import aiofiles

load_dotenv()

UPLOAD_DIR = "uploads"
BASE_URL = os.getenv("BASE_URL", "http://localhost:8000")

router = APIRouter()

# ─── Helpers ─────────────────────────────────────────────────────────────────

def make_file_response(f: models.File, current_user_id: int) -> schemas.FileResponse:
    is_mine = (f.uploaded_by == current_user_id) or (f.uploaded_by is None)
    return schemas.FileResponse(
        id=f.id,
        bucket_id=f.bucket_id,
        filename=f.filename,
        size=f.size,
        uploaded_at=f.uploaded_at,
        share_link=f"{BASE_URL}/download/{f.id}",
        uploaded_by=f.uploaded_by,
        is_mine=is_mine
    )

def get_valid_share(bucket_id: int, db: Session) -> models.Share | None:
    return db.query(models.Share).filter(
        models.Share.bucket_id == bucket_id,
        models.Share.expiry_time > int(time.time() * 1000)
    ).first()

# ─── Auth ─────────────────────────────────────────────────────────────────────

@router.post("/register", response_model=dict)
def register(req: schemas.RegisterRequest, db: Session = Depends(get_db)):
    if db.query(models.User).filter(models.User.email == req.email).first():
        raise HTTPException(status_code=400, detail="Email already registered")
    user = models.User(
        username=req.username,
        email=req.email,
        password_hash=hash_password(req.password),
        created_at=int(time.time() * 1000)
    )
    db.add(user)
    db.commit()
    return {"message": "Registration successful"}

@router.put("/profile", response_model=dict)
def update_profile(
    req: schemas.UpdateProfileRequest,
    current_user: models.User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    current_user.username = req.username
    db.commit()
    return {"message": "Profile updated", "username": current_user.username}

@router.post("/change-password", response_model=dict)
def change_password(
    req: schemas.ChangePasswordRequest,
    current_user: models.User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    if not verify_password(req.current_password, current_user.password_hash):
        raise HTTPException(status_code=400, detail="Current password is incorrect")
    current_user.password_hash = hash_password(req.new_password)
    db.commit()
    return {"message": "Password changed successfully"}

@router.post("/forgot-password", response_model=dict)
def forgot_password(req: schemas.ForgotPasswordRequest, db: Session = Depends(get_db)):
    user = db.query(models.User).filter(models.User.email == req.email).first()
    if not user:
        raise HTTPException(status_code=404, detail="No account found with this email")
    user.password_hash = hash_password(req.new_password)
    db.commit()
    return {"message": "Password updated successfully"}

@router.post("/login", response_model=schemas.TokenResponse)
def login(req: schemas.LoginRequest, db: Session = Depends(get_db)):
    user = db.query(models.User).filter(models.User.email == req.email).first()
    if not user or not verify_password(req.password, user.password_hash):
        raise HTTPException(status_code=401, detail="Invalid email or password")
    token = create_token(user.id, user.email)
    return schemas.TokenResponse(
        token=token,
        user_id=user.id,
        username=user.username,
        email=user.email
    )

# ─── Buckets ──────────────────────────────────────────────────────────────────

@router.get("/buckets", response_model=list[schemas.BucketResponse])
def get_buckets(
    current_user: models.User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    return db.query(models.Bucket).filter(
        models.Bucket.owner_id == current_user.id
    ).all()

@router.post("/bucket", response_model=schemas.BucketResponse)
def create_bucket(
    req: schemas.BucketCreate,
    current_user: models.User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    bucket = models.Bucket(
        name=req.name,
        owner_id=current_user.id,
        created_at=int(time.time() * 1000)
    )
    db.add(bucket)
    db.commit()
    db.refresh(bucket)
    return bucket

@router.put("/bucket/{bucket_id}", response_model=schemas.BucketResponse)
def rename_bucket(
    bucket_id: int,
    req: schemas.BucketCreate,
    current_user: models.User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    bucket = db.query(models.Bucket).filter(
        models.Bucket.id == bucket_id,
        models.Bucket.owner_id == current_user.id
    ).first()
    if not bucket:
        raise HTTPException(status_code=404, detail="Bucket not found")
    bucket.name = req.name
    db.commit()
    db.refresh(bucket)
    return bucket

@router.delete("/bucket/{bucket_id}", response_model=dict)
def delete_bucket(
    bucket_id: int,
    current_user: models.User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    bucket = db.query(models.Bucket).filter(
        models.Bucket.id == bucket_id,
        models.Bucket.owner_id == current_user.id
    ).first()
    if not bucket:
        raise HTTPException(status_code=404, detail="Bucket not found")

    files = db.query(models.File).filter(models.File.bucket_id == bucket_id).all()
    for f in files:
        if os.path.exists(f.filepath):
            os.remove(f.filepath)
        db.delete(f)

    # delete access logs first, then shares
    shares = db.query(models.Share).filter(models.Share.bucket_id == bucket_id).all()
    for s in shares:
        db.query(models.SharedAccessLog).filter(models.SharedAccessLog.share_id == s.id).delete()
        db.delete(s)

    db.delete(bucket)
    db.commit()
    return {"message": "Bucket deleted"}

# ─── Files ────────────────────────────────────────────────────────────────────

@router.get("/files/{bucket_id}", response_model=list[schemas.FileResponse])
def get_files(
    bucket_id: int,
    current_user: models.User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    # Allow owner OR anyone with a valid share code for this bucket
    bucket = db.query(models.Bucket).filter(models.Bucket.id == bucket_id).first()
    if not bucket:
        raise HTTPException(status_code=404, detail="Bucket not found")

    is_owner = bucket.owner_id == current_user.id
    has_share = get_valid_share(bucket_id, db) is not None

    if not is_owner and not has_share:
        raise HTTPException(status_code=403, detail="Access denied")

    files = db.query(models.File).filter(models.File.bucket_id == bucket_id).all()
    return [make_file_response(f, current_user.id) for f in files]

@router.post("/upload/{bucket_id}", response_model=schemas.FileResponse)
async def upload_file(
    bucket_id: int,
    file: UploadFile = File(...),
    current_user: models.User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    # Allow owner OR shared user with valid share code
    bucket = db.query(models.Bucket).filter(models.Bucket.id == bucket_id).first()
    if not bucket:
        raise HTTPException(status_code=404, detail="Bucket not found")

    is_owner = bucket.owner_id == current_user.id
    has_share = get_valid_share(bucket_id, db) is not None

    if not is_owner and not has_share:
        raise HTTPException(status_code=403, detail="Access denied")

    os.makedirs(UPLOAD_DIR, exist_ok=True)
    filepath = os.path.join(UPLOAD_DIR, f"{int(time.time())}_{file.filename}")

    async with aiofiles.open(filepath, "wb") as f:
        content = await file.read()
        await f.write(content)

    db_file = models.File(
        bucket_id=bucket_id,
        uploaded_by=current_user.id,
        filename=file.filename,
        filepath=filepath,
        size=len(content),
        uploaded_at=int(time.time() * 1000)
    )
    db.add(db_file)
    db.commit()
    db.refresh(db_file)
    return make_file_response(db_file, current_user.id)

@router.put("/file/{file_id}", response_model=schemas.FileResponse)
def rename_file(
    file_id: int,
    req: schemas.FileRenameRequest,
    current_user: models.User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    # Only the uploader or bucket owner can rename
    db_file = db.query(models.File).join(models.Bucket).filter(
        models.File.id == file_id
    ).first()
    if not db_file:
        raise HTTPException(status_code=404, detail="File not found")

    is_bucket_owner = db_file.bucket.owner_id == current_user.id
    is_uploader = db_file.uploaded_by == current_user.id

    if not is_bucket_owner and not is_uploader:
        raise HTTPException(status_code=403, detail="Only the uploader or bucket owner can rename this file")

    db_file.filename = req.filename
    db.commit()
    db.refresh(db_file)
    return make_file_response(db_file, current_user.id)

@router.delete("/file/{file_id}", response_model=dict)
def delete_file(
    file_id: int,
    current_user: models.User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    db_file = db.query(models.File).join(models.Bucket).filter(
        models.File.id == file_id
    ).first()
    if not db_file:
        raise HTTPException(status_code=404, detail="File not found")

    is_bucket_owner = db_file.bucket.owner_id == current_user.id
    is_uploader = db_file.uploaded_by == current_user.id

    if not is_bucket_owner and not is_uploader:
        raise HTTPException(status_code=403, detail="You can only delete files you uploaded")

    if os.path.exists(db_file.filepath):
        os.remove(db_file.filepath)
    db.delete(db_file)
    db.commit()
    return {"message": "File deleted"}

@router.get("/download/{file_id}")
def download_file(file_id: int, db: Session = Depends(get_db)):
    from fastapi.responses import FileResponse as FastAPIFileResponse
    db_file = db.query(models.File).filter(models.File.id == file_id).first()
    if not db_file or not os.path.exists(db_file.filepath):
        raise HTTPException(status_code=404, detail="File not found")
    return FastAPIFileResponse(
        db_file.filepath,
        filename=db_file.filename,
        media_type="application/octet-stream"
    )

# ─── Sharing ──────────────────────────────────────────────────────────────────

@router.post("/share/{bucket_id}", response_model=schemas.ShareResponse)
def create_share(
    bucket_id: int,
    current_user: models.User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    bucket = db.query(models.Bucket).filter(
        models.Bucket.id == bucket_id,
        models.Bucket.owner_id == current_user.id
    ).first()
    if not bucket:
        raise HTTPException(status_code=404, detail="Bucket not found")

    existing = db.query(models.Share).filter(
        models.Share.bucket_id == bucket_id,
        models.Share.expiry_time > int(time.time() * 1000)
    ).first()
    if existing:
        return existing

    code = ''.join(random.choices(string.ascii_uppercase + string.digits, k=8))
    expiry = int(time.time() * 1000) + (24 * 60 * 60 * 1000)
    share = models.Share(
        bucket_id=bucket_id,
        share_code=code,
        expiry_time=expiry
    )
    db.add(share)
    db.commit()
    db.refresh(share)
    return share

@router.get("/share/{code}", response_model=schemas.ShareLookupResponse)
def lookup_share(
    code: str,
    current_user: models.User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    share = db.query(models.Share).filter(
        models.Share.share_code == code
    ).first()
    if not share:
        raise HTTPException(status_code=404, detail="Invalid share code")
    if int(time.time() * 1000) > share.expiry_time:
        raise HTTPException(status_code=410, detail="Share code has expired")

    # Log this access
    log = models.SharedAccessLog(
        share_id=share.id,
        user_id=current_user.id,
        accessed_at=int(time.time() * 1000)
    )
    db.add(log)
    db.commit()

    return schemas.ShareLookupResponse(
        bucket_id=share.bucket_id,
        bucket_name=share.bucket.name,
        share_code=share.share_code,
        owner_username=share.bucket.owner.username
    )

@router.get("/shared-buckets", response_model=list[schemas.SharedAccessedBucketResponse])
def get_shared_buckets(
    current_user: models.User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Returns buckets this user accessed via share code in the last 24 hours."""
    since = int(time.time() * 1000) - (24 * 60 * 60 * 1000)
    logs = db.query(models.SharedAccessLog).filter(
        models.SharedAccessLog.user_id == current_user.id,
        models.SharedAccessLog.accessed_at >= since
    ).order_by(models.SharedAccessLog.accessed_at.desc()).all()

    seen = set()
    result = []
    for log in logs:
        share = log.share
        if share.bucket_id in seen:
            continue
        seen.add(share.bucket_id)
        # Only include if share is still valid
        if share.expiry_time > int(time.time() * 1000):
            result.append(schemas.SharedAccessedBucketResponse(
                bucket_id=share.bucket_id,
                bucket_name=share.bucket.name,
                share_code=share.share_code,
                owner_username=share.bucket.owner.username,
                accessed_at=log.accessed_at
            ))
    return result
