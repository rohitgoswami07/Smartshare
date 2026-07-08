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
import re
import cloudinary
import cloudinary.uploader

load_dotenv()

cloudinary.config(
    cloud_name=os.getenv("CLOUDINARY_CLOUD_NAME"),
    api_key=os.getenv("CLOUDINARY_API_KEY"),
    api_secret=os.getenv("CLOUDINARY_API_SECRET"),
    secure=True
)

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
        share_link=f.filepath,  # filepath now stores the Cloudinary URL
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
        try:
            if f.cloudinary_public_id:
                ext = f.filename.rsplit('.', 1)[-1].lower() if '.' in f.filename else ''
                res_type = 'video' if ext in ('mp4','mkv','mov','avi','webm') else ('image' if ext in ('jpg','jpeg','png','gif','webp','bmp','svg') else 'raw')
                cloudinary.uploader.destroy(f.cloudinary_public_id, resource_type=res_type)
        except Exception:
            pass
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
    bucket = db.query(models.Bucket).filter(models.Bucket.id == bucket_id).first()
    if not bucket:
        raise HTTPException(status_code=404, detail="Bucket not found")

    is_owner = bucket.owner_id == current_user.id

    # Check valid share code exists for this bucket
    has_share = get_valid_share(bucket_id, db) is not None

    # Also allow if user received a code message for this bucket that hasn't expired
    has_message_access = db.query(models.CodeMessage).filter(
        models.CodeMessage.receiver_id == current_user.id,
        models.CodeMessage.expiry_time > int(time.time() * 1000)
    ).join(models.Share, models.CodeMessage.share_code == models.Share.share_code).filter(
        models.Share.bucket_id == bucket_id
    ).first() is not None

    if not is_owner and not has_share and not has_message_access:
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
    bucket = db.query(models.Bucket).filter(models.Bucket.id == bucket_id).first()
    if not bucket:
        raise HTTPException(status_code=404, detail="Bucket not found")

    is_owner = bucket.owner_id == current_user.id
    has_share = get_valid_share(bucket_id, db) is not None
    has_message_access = db.query(models.CodeMessage).filter(
        models.CodeMessage.receiver_id == current_user.id,
        models.CodeMessage.expiry_time > int(time.time() * 1000)
    ).join(models.Share, models.CodeMessage.share_code == models.Share.share_code).filter(
        models.Share.bucket_id == bucket_id
    ).first() is not None

    if not is_owner and not has_share and not has_message_access:
        raise HTTPException(status_code=403, detail="Access denied")

    # Sanitize filename for Cloudinary public_id
    original_filename = file.filename or "unnamed"
    safe_name = re.sub(r'[^\w\-.]', '_', original_filename)
    content = await file.read()
    if not content:
        raise HTTPException(status_code=400, detail="Empty file")
    ext = safe_name.rsplit('.', 1)[-1].lower() if '.' in safe_name else ''
    is_video = ext in ('mp4', 'mkv', 'mov', 'avi', 'webm')
    resource_type = 'video' if is_video else 'auto'

    try:
        result = cloudinary.uploader.upload(
            content,
            folder=f"smartshare/bucket_{bucket_id}",
            public_id=f"{int(time.time())}_{safe_name}",
            resource_type=resource_type,
            overwrite=False
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Cloudinary upload failed: {str(e)}")
    cloudinary_url = result["secure_url"]
    cloudinary_public_id = result["public_id"]

    db_file = models.File(
        bucket_id=bucket_id,
        uploaded_by=current_user.id,
        filename=original_filename,
        filepath=cloudinary_url,
        cloudinary_public_id=cloudinary_public_id,
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

    try:
        if db_file.cloudinary_public_id:
            ext = db_file.filename.rsplit('.', 1)[-1].lower() if '.' in db_file.filename else ''
            res_type = 'video' if ext in ('mp4','mkv','mov','avi','webm') else ('image' if ext in ('jpg','jpeg','png','gif','webp','bmp','svg') else 'raw')
            cloudinary.uploader.destroy(db_file.cloudinary_public_id, resource_type=res_type)
    except Exception:
        pass

    db.delete(db_file)
    db.commit()
    return {"message": "File deleted"}

@router.get("/download/{file_id}")
def download_file(file_id: int, db: Session = Depends(get_db)):
    from fastapi.responses import RedirectResponse
    db_file = db.query(models.File).filter(models.File.id == file_id).first()
    if not db_file:
        raise HTTPException(status_code=404, detail="File not found")
    return RedirectResponse(url=db_file.filepath)

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

# ─── Friends ──────────────────────────────────────────────────────────────────

@router.post("/friends/{username}", response_model=schemas.FriendResponse)
def add_friend(
    username: str,
    current_user: models.User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    target = db.query(models.User).filter(
        models.User.username.ilike(username)
    ).first()
    if not target:
        raise HTTPException(status_code=404, detail="User not found")
    if target.id == current_user.id:
        raise HTTPException(status_code=400, detail="Cannot add yourself")
    existing = db.query(models.Friendship).filter(
        models.Friendship.user_id == current_user.id,
        models.Friendship.friend_id == target.id
    ).first()
    if existing:
        raise HTTPException(status_code=400, detail="Already friends")
    # Add both directions for easy querying
    db.add(models.Friendship(user_id=current_user.id, friend_id=target.id, created_at=int(time.time() * 1000)))
    db.add(models.Friendship(user_id=target.id, friend_id=current_user.id, created_at=int(time.time() * 1000)))
    db.commit()
    return schemas.FriendResponse(user_id=target.id, username=target.username)

@router.delete("/friends/{friend_id}", response_model=dict)
def remove_friend(
    friend_id: int,
    current_user: models.User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    db.query(models.Friendship).filter(
        ((models.Friendship.user_id == current_user.id) & (models.Friendship.friend_id == friend_id)) |
        ((models.Friendship.user_id == friend_id) & (models.Friendship.friend_id == current_user.id))
    ).delete()
    db.commit()
    return {"message": "Friend removed"}

@router.get("/friends", response_model=list[schemas.FriendResponse])
def get_friends(
    current_user: models.User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    friendships = db.query(models.Friendship).filter(
        models.Friendship.user_id == current_user.id
    ).all()
    return [schemas.FriendResponse(user_id=f.friend.id, username=f.friend.username) for f in friendships]

@router.post("/messages/{friend_id}/{bucket_id}", response_model=schemas.CodeMessageResponse)
def send_code(
    friend_id: int,
    bucket_id: int,
    current_user: models.User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    # Must be friends
    friendship = db.query(models.Friendship).filter(
        models.Friendship.user_id == current_user.id,
        models.Friendship.friend_id == friend_id
    ).first()
    if not friendship:
        raise HTTPException(status_code=403, detail="Not friends")

    # Get or create a valid share code for this bucket
    share = get_valid_share(bucket_id, db)
    if not share:
        bucket = db.query(models.Bucket).filter(
            models.Bucket.id == bucket_id,
            models.Bucket.owner_id == current_user.id
        ).first()
        if not bucket:
            raise HTTPException(status_code=404, detail="Bucket not found or not yours")
        code = ''.join(random.choices(string.ascii_uppercase + string.digits, k=8))
        expiry = int(time.time() * 1000) + (24 * 60 * 60 * 1000)
        share = models.Share(bucket_id=bucket_id, share_code=code, expiry_time=expiry)
        db.add(share)
        db.commit()
        db.refresh(share)

    msg = models.CodeMessage(
        sender_id=current_user.id,
        receiver_id=friend_id,
        share_code=share.share_code,
        bucket_name=share.bucket.name,
        expiry_time=share.expiry_time,
        sent_at=int(time.time() * 1000)
    )
    db.add(msg)
    db.commit()
    db.refresh(msg)
    return schemas.CodeMessageResponse(
        id=msg.id,
        sender_id=msg.sender_id,
        sender_username=current_user.username,
        receiver_id=msg.receiver_id,
        share_code=msg.share_code,
        bucket_name=msg.bucket_name,
        expiry_time=msg.expiry_time,
        sent_at=msg.sent_at
    )

@router.get("/messages/{friend_id}", response_model=list[schemas.CodeMessageResponse])
def get_messages(
    friend_id: int,
    current_user: models.User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    now = int(time.time() * 1000)
    msgs = db.query(models.CodeMessage).filter(
        ((models.CodeMessage.sender_id == current_user.id) & (models.CodeMessage.receiver_id == friend_id)) |
        ((models.CodeMessage.sender_id == friend_id) & (models.CodeMessage.receiver_id == current_user.id))
    ).filter(
        models.CodeMessage.expiry_time > now  # only return non-expired codes
    ).order_by(models.CodeMessage.sent_at.asc()).all()

    return [
        schemas.CodeMessageResponse(
            id=m.id,
            sender_id=m.sender_id,
            sender_username=m.sender.username,
            receiver_id=m.receiver_id,
            share_code=m.share_code,
            bucket_name=m.bucket_name,
            expiry_time=m.expiry_time,
            sent_at=m.sent_at
        ) for m in msgs
    ]
