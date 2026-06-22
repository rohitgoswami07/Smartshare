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

# ─── Auth ────────────────────────────────────────────────────────────────────

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

# ─── Buckets ─────────────────────────────────────────────────────────────────

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

    # Delete all files from disk first
    files = db.query(models.File).filter(models.File.bucket_id == bucket_id).all()
    for f in files:
        if os.path.exists(f.filepath):
            os.remove(f.filepath)
        db.delete(f)

    # Delete all shares for this bucket
    db.query(models.Share).filter(models.Share.bucket_id == bucket_id).delete()

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
    files = db.query(models.File).filter(
        models.File.bucket_id == bucket_id
    ).all()
    return [
        schemas.FileResponse(
            id=f.id,
            bucket_id=f.bucket_id,
            filename=f.filename,
            size=f.size,
            uploaded_at=f.uploaded_at,
            share_link=f"{BASE_URL}/download/{f.id}"
        )
        for f in files
    ]

@router.post("/upload/{bucket_id}", response_model=schemas.FileResponse)
async def upload_file(
    bucket_id: int,
    file: UploadFile = File(...),
    current_user: models.User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    bucket = db.query(models.Bucket).filter(
        models.Bucket.id == bucket_id,
        models.Bucket.owner_id == current_user.id
    ).first()
    if not bucket:
        raise HTTPException(status_code=404, detail="Bucket not found")

    os.makedirs(UPLOAD_DIR, exist_ok=True)
    filepath = os.path.join(UPLOAD_DIR, f"{int(time.time())}_{file.filename}")

    async with aiofiles.open(filepath, "wb") as f:
        content = await file.read()
        await f.write(content)

    db_file = models.File(
        bucket_id=bucket_id,
        filename=file.filename,
        filepath=filepath,
        size=len(content),
        uploaded_at=int(time.time() * 1000)
    )
    db.add(db_file)
    db.commit()
    db.refresh(db_file)

    return schemas.FileResponse(
        id=db_file.id,
        bucket_id=db_file.bucket_id,
        filename=db_file.filename,
        size=db_file.size,
        uploaded_at=db_file.uploaded_at,
        share_link=f"{BASE_URL}/download/{db_file.id}"
    )

@router.put("/file/{file_id}", response_model=schemas.FileResponse)
def rename_file(
    file_id: int,
    req: schemas.FileRenameRequest,
    current_user: models.User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    db_file = db.query(models.File).join(models.Bucket).filter(
        models.File.id == file_id,
        models.Bucket.owner_id == current_user.id
    ).first()
    if not db_file:
        raise HTTPException(status_code=404, detail="File not found")
    db_file.filename = req.filename
    db.commit()
    db.refresh(db_file)
    return schemas.FileResponse(
        id=db_file.id,
        bucket_id=db_file.bucket_id,
        filename=db_file.filename,
        size=db_file.size,
        uploaded_at=db_file.uploaded_at,
        share_link=f"{BASE_URL}/download/{db_file.id}"
    )

@router.delete("/file/{file_id}", response_model=dict)
def delete_file(
    file_id: int,
    current_user: models.User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    db_file = db.query(models.File).filter(models.File.id == file_id).first()
    if not db_file:
        raise HTTPException(status_code=404, detail="File not found")
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

    # Return existing valid share code if one exists for this bucket
    existing = db.query(models.Share).filter(
        models.Share.bucket_id == bucket_id,
        models.Share.expiry_time > int(time.time() * 1000)
    ).first()
    if existing:
        return existing

    # Create new share code only if none exists or all expired
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
def lookup_share(code: str, db: Session = Depends(get_db)):
    share = db.query(models.Share).filter(
        models.Share.share_code == code
    ).first()
    if not share:
        raise HTTPException(status_code=404, detail="Invalid share code")
    if int(time.time() * 1000) > share.expiry_time:
        raise HTTPException(status_code=410, detail="Share code has expired")
    return schemas.ShareLookupResponse(
        bucket_id=share.bucket_id,
        bucket_name=share.bucket.name,
        share_code=share.share_code
    )
