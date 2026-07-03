from pydantic import BaseModel, EmailStr
from typing import Optional

# Auth
class RegisterRequest(BaseModel):
    username: str
    email: EmailStr
    password: str

class LoginRequest(BaseModel):
    email: EmailStr
    password: str

class TokenResponse(BaseModel):
    token: str
    user_id: int
    username: str
    email: str

class ForgotPasswordRequest(BaseModel):
    email: EmailStr
    new_password: str

class ChangePasswordRequest(BaseModel):
    current_password: str
    new_password: str

class UpdateProfileRequest(BaseModel):
    username: str

# Bucket
class BucketCreate(BaseModel):
    name: str

class BucketResponse(BaseModel):
    id: int
    name: str
    owner_id: int
    created_at: int

    class Config:
        from_attributes = True

# File
class FileRenameRequest(BaseModel):
    filename: str

class FileResponse(BaseModel):
    id: int
    bucket_id: int
    filename: str
    size: int
    uploaded_at: int
    share_link: str
    uploaded_by: Optional[int] = None
    is_mine: Optional[bool] = True

    class Config:
        from_attributes = True

# Share
class ShareResponse(BaseModel):
    share_code: str
    bucket_id: int
    expiry_time: int

    class Config:
        from_attributes = True

class ShareLookupResponse(BaseModel):
    bucket_id: int
    bucket_name: str
    share_code: str
    owner_username: str

class SharedAccessedBucketResponse(BaseModel):
    bucket_id: int
    bucket_name: str
    share_code: str
    owner_username: str
    accessed_at: int

# Friends
class FriendResponse(BaseModel):
    user_id: int
    username: str

    class Config:
        from_attributes = True

class CodeMessageResponse(BaseModel):
    id: int
    sender_id: int
    sender_username: str
    receiver_id: int
    share_code: str
    bucket_name: str
    expiry_time: int
    sent_at: int

    class Config:
        from_attributes = True
