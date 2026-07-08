from sqlalchemy import Column, Integer, String, ForeignKey, BigInteger
from sqlalchemy.orm import relationship
from database import Base

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    username = Column(String, nullable=False)
    email = Column(String, unique=True, index=True, nullable=False)
    password_hash = Column(String, nullable=False)
    created_at = Column(BigInteger, nullable=False)

    buckets = relationship("Bucket", back_populates="owner")

class Bucket(Base):
    __tablename__ = "buckets"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=False)
    owner_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    created_at = Column(BigInteger, nullable=False)

    owner = relationship("User", back_populates="buckets")
    files = relationship("File", back_populates="bucket")
    shares = relationship("Share", back_populates="bucket")

class File(Base):
    __tablename__ = "files"

    id = Column(Integer, primary_key=True, index=True)
    bucket_id = Column(Integer, ForeignKey("buckets.id"), nullable=False)
    uploaded_by = Column(Integer, ForeignKey("users.id"), nullable=True)
    filename = Column(String, nullable=False)
    filepath = Column(String, nullable=False)
    cloudinary_public_id = Column(String, nullable=True)
    size = Column(Integer, nullable=False)
    uploaded_at = Column(BigInteger, nullable=False)

    bucket = relationship("Bucket", back_populates="files")
    uploader = relationship("User", foreign_keys=[uploaded_by])

class Share(Base):
    __tablename__ = "shares"

    id = Column(Integer, primary_key=True, index=True)
    bucket_id = Column(Integer, ForeignKey("buckets.id"), nullable=False)
    share_code = Column(String, unique=True, index=True, nullable=False)
    expiry_time = Column(BigInteger, nullable=False)

    bucket = relationship("Bucket", back_populates="shares")
    access_logs = relationship("SharedAccessLog", back_populates="share")

class SharedAccessLog(Base):
    __tablename__ = "shared_access_logs"

    id = Column(Integer, primary_key=True, index=True)
    share_id = Column(Integer, ForeignKey("shares.id"), nullable=False)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    accessed_at = Column(BigInteger, nullable=False)

    share = relationship("Share", back_populates="access_logs")
    user = relationship("User")

class Friendship(Base):
    __tablename__ = "friendships"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    friend_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    created_at = Column(BigInteger, nullable=False)

    user = relationship("User", foreign_keys=[user_id])
    friend = relationship("User", foreign_keys=[friend_id])

class CodeMessage(Base):
    __tablename__ = "code_messages"

    id = Column(Integer, primary_key=True, index=True)
    sender_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    receiver_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    share_code = Column(String, nullable=False)
    bucket_name = Column(String, nullable=False)
    expiry_time = Column(BigInteger, nullable=False)
    sent_at = Column(BigInteger, nullable=False)

    sender = relationship("User", foreign_keys=[sender_id])
    receiver = relationship("User", foreign_keys=[receiver_id])
