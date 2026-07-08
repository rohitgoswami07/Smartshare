import os
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import text
from database import engine
import models
import routes

def run_migrations():
    with engine.connect() as conn:
        conn.execute(text("ALTER TABLE files ADD COLUMN IF NOT EXISTS cloudinary_public_id VARCHAR;"))
        conn.execute(text("ALTER TABLE files ADD COLUMN IF NOT EXISTS uploaded_by INTEGER REFERENCES users(id);"))
        conn.execute(text("""
            CREATE TABLE IF NOT EXISTS friendships (
                id SERIAL PRIMARY KEY,
                user_id INTEGER NOT NULL REFERENCES users(id),
                friend_id INTEGER NOT NULL REFERENCES users(id),
                created_at BIGINT NOT NULL
            );
        """))
        conn.execute(text("""
            CREATE TABLE IF NOT EXISTS code_messages (
                id SERIAL PRIMARY KEY,
                sender_id INTEGER NOT NULL REFERENCES users(id),
                receiver_id INTEGER NOT NULL REFERENCES users(id),
                share_code VARCHAR NOT NULL,
                bucket_name VARCHAR NOT NULL,
                expiry_time BIGINT NOT NULL,
                sent_at BIGINT NOT NULL
            );
        """))
        conn.execute(text("""
            CREATE TABLE IF NOT EXISTS shared_access_logs (
                id SERIAL PRIMARY KEY,
                share_id INTEGER NOT NULL REFERENCES shares(id),
                user_id INTEGER NOT NULL REFERENCES users(id),
                accessed_at BIGINT NOT NULL
            );
        """))
        conn.commit()

models.Base.metadata.create_all(bind=engine)
run_migrations()

app = FastAPI(
    title="SmartShare API",
    description="Backend API for SmartShare - Bucket-Based File Sharing Platform",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(routes.router)

if __name__ == "__main__":
    import uvicorn
    port = int(os.environ.get("PORT", 8000))
    uvicorn.run("main:app", host="0.0.0.0", port=port, reload=False)
