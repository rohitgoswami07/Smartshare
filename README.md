# SmartShare — Bucket-Based File Sharing Platform

> A full-stack Android application enabling organized, secure file sharing through a bucket-based architecture. Designed for academic, project, and team collaboration use cases.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Features](#features)
- [Project Structure](#project-structure)
- [API Reference](#api-reference)
- [Setup & Installation](#setup--installation)
  - [Backend](#backend-setup)
  - [Android](#android-setup)
- [Deployment](#deployment)
- [Security Considerations](#security-considerations)

---

## Overview

SmartShare is a mobile-first file sharing platform built for Android. Instead of sharing files individually, users organize files into **buckets** — named containers for a specific purpose (e.g., "CS Assignment 3", "Team Project", "Lecture Notes"). Buckets can be shared with friends via time-limited share codes, enabling controlled collaborative access.

The backend is a RESTful API built with **FastAPI** (Python), backed by **PostgreSQL**, and deployed on **Render**. File storage is handled via **Cloudinary**. The Android client is built with **Kotlin + Jetpack Compose** following the **MVVM** architecture pattern.

---

## Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Android Frontend | Kotlin, Jetpack Compose | Kotlin 2.1.20 |
| Architecture Pattern | MVVM + Repository | — |
| Backend API | FastAPI (Python) | 0.137.2 |
| Database | PostgreSQL | — |
| ORM | SQLAlchemy | 2.0.51 |
| Schema Validation | Pydantic | 2.13.4 |
| Authentication | JWT (python-jose) + SHA-256 | — |
| File Storage | Cloudinary | 1.41.0 |
| Networking (Android) | Retrofit2 + OkHttp3 | 2.11.0 / 4.12.0 |
| Local Cache (Android) | Room Database | 2.7.1 |
| Image Loading | Coil | 2.6.0 |
| Media Playback | ExoPlayer (Media3) | 1.3.1 |
| Deployment | Render (backend) | — |

---

## Architecture

```
┌─────────────────────────────────────────────┐
│              Android Client                  │
│                                             │
│  Compose UI  ──►  ViewModel  ──►  Repository│
│                                      │      │
│              Room DB (cache) ◄───────┘      │
│                      │                      │
│              Retrofit2 / OkHttp3            │
└──────────────────────┬──────────────────────┘
                       │ HTTPS
┌──────────────────────▼──────────────────────┐
│              FastAPI Backend                 │
│                                             │
│  Routes  ──►  Auth (JWT)  ──►  SQLAlchemy   │
│                                      │      │
│                              PostgreSQL DB  │
│                                             │
│              Cloudinary (File Storage)      │
└─────────────────────────────────────────────┘
```

---

## Features

### Authentication
- User registration with email and password
- JWT-based login with 24-hour token expiry
- Password reset via registered email
- Change password (authenticated)
- Update display name

### Bucket Management
- Create, rename, and delete buckets
- Each bucket is owned by the creating user
- Deleting a bucket removes all associated files from Cloudinary and the database

### File Management
- Upload any file type to a bucket (stored on Cloudinary)
- Download files directly to device Downloads folder
- Rename and delete individual files
- File ownership tracked per upload
- In-app media preview for images and videos

### Sharing
- Generate a time-limited share code (24-hour expiry) for any bucket
- Share codes can be sent to friends via in-app messaging
- Recipients can access the shared bucket while the code is valid
- Access log maintained per share code usage

### Friends & Messaging
- Add friends by username
- Remove friends
- Send bucket share codes directly to friends via in-app chat
- Messages expire alongside their share codes

---

## Project Structure

```
SmartShare/
├── app/                                        # Android application
│   └── src/main/java/com/rohit/smartshare/
│       ├── api/                                # Retrofit service, API models
│       │   ├── ApiModels.kt
│       │   ├── ApiService.kt
│       │   └── RetrofitClient.kt
│       ├── data/                               # Room DB entities, DAOs
│       ├── navigation/                         # Compose navigation graph
│       ├── repository/                         # Data layer (API + Room)
│       ├── screens/                            # All Compose UI screens
│       ├── utils/                              # SessionManager, PasswordUtils
│       └── viewmodel/                          # ViewModels (MVVM)
│
└── backend/                                    # FastAPI backend
    ├── main.py                                 # App entry point, DB migrations
    ├── routes.py                               # All API route handlers
    ├── models.py                               # SQLAlchemy ORM models
    ├── schemas.py                              # Pydantic request/response schemas
    ├── auth.py                                 # JWT creation & verification
    ├── database.py                             # DB engine & session
    ├── render.yaml                             # Render deployment config
    └── requirements.txt                        # Python dependencies
```

---

## API Reference

### Authentication

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/register` | No | Register a new user |
| POST | `/login` | No | Login, returns JWT token |
| POST | `/forgot-password` | No | Reset password by email |
| PUT | `/profile` | Yes | Update display name |
| POST | `/change-password` | Yes | Change password |

### Buckets

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/buckets` | Yes | Get all buckets owned by user |
| POST | `/bucket` | Yes | Create a new bucket |
| PUT | `/bucket/{id}` | Yes | Rename a bucket |
| DELETE | `/bucket/{id}` | Yes | Delete bucket and all its files |

### Files

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/files/{bucketId}` | Yes | List files in a bucket |
| POST | `/upload/{bucketId}` | Yes | Upload a file to a bucket |
| PUT | `/file/{id}` | Yes | Rename a file |
| DELETE | `/file/{id}` | Yes | Delete a file |
| GET | `/download/{id}` | No | Download/redirect to file URL |

### Sharing

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/share/{bucketId}` | Yes | Generate a share code (24hr expiry) |
| GET | `/share/{code}` | Yes | Lookup and validate a share code |
| GET | `/shared-buckets` | Yes | Get buckets accessed via share code |

### Friends & Messages

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/friends` | Yes | Get friend list |
| POST | `/friends/{username}` | Yes | Add a friend by username |
| DELETE | `/friends/{friendId}` | Yes | Remove a friend |
| POST | `/messages/{friendId}/{bucketId}` | Yes | Send share code to a friend |
| GET | `/messages/{friendId}` | Yes | Get message history with a friend |

> All authenticated endpoints require `Authorization: Bearer <token>` header.

---

## Setup & Installation

### Backend Setup

#### Prerequisites
- Python 3.10+
- PostgreSQL database
- Cloudinary account

#### 1. Clone and navigate
```bash
git clone https://github.com/rohitgoswami07/Smartshare.git
cd SmartShare/backend
```

#### 2. Create virtual environment
```bash
python3 -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
```

#### 3. Install dependencies
```bash
pip install -r requirements.txt
```

#### 4. Configure environment variables
```bash
cp .env.example .env
```

Edit `.env` with your values:
```env
DATABASE_URL=postgresql://user:password@localhost:5432/smartshare
SECRET_KEY=your-secret-key
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
```

#### 5. Set up PostgreSQL
```bash
sudo -u postgres psql -c "CREATE DATABASE smartshare;"
sudo -u postgres psql -c "CREATE USER smartshare_user WITH PASSWORD 'yourpassword';"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE smartshare TO smartshare_user;"
sudo -u postgres psql -c "ALTER DATABASE smartshare OWNER TO smartshare_user;"
```

#### 6. Run the server
```bash
python3 main.py
```

API documentation available at: `http://localhost:8000/docs`

---

### Android Setup

#### Prerequisites
- Android Studio (latest stable)
- Android device or emulator (API 26+)

#### Steps
1. Open the project root in Android Studio
2. Wait for Gradle sync to complete
3. Update `BASE_URL` in `RetrofitClient.kt` if running a local backend:
   ```kotlin
   const val BASE_URL = "http://<your-local-ip>:8000/"
   ```
4. Build and run on a device or emulator

---

## Deployment

The backend is deployed on **Render** using the configuration in `backend/render.yaml`.

| Service | Platform | URL |
|---------|----------|-----|
| Backend API | Render | `https://smartshare-txa0.onrender.com` |
| File Storage | Cloudinary | Managed via Cloudinary dashboard |
| Database | Render PostgreSQL | Internal to Render |

Environment variables (`DATABASE_URL`, `SECRET_KEY`, `CLOUDINARY_*`) are configured directly in the Render service dashboard and are never committed to the repository.

---

## Security Considerations

| Area | Implementation |
|------|---------------|
| Authentication | JWT tokens with 24-hour expiry |
| Password Storage | SHA-256 hashing |
| Secrets Management | Environment variables via `.env` (never committed) |
| HTTP Logging | Disabled in release builds |
| File Access | Ownership and share-code validation on every request |
| Share Expiry | All share codes expire after 24 hours |
| Access Logs | Every share code usage is logged with user ID and timestamp |

---

## Database Schema

```
users           — id, username, email, password_hash, created_at
buckets         — id, name, owner_id, created_at
files           — id, bucket_id, uploaded_by, filename, filepath, cloudinary_public_id, size, uploaded_at
shares          — id, bucket_id, share_code, expiry_time
shared_access_logs — id, share_id, user_id, accessed_at
friendships     — id, user_id, friend_id, created_at
code_messages   — id, sender_id, receiver_id, share_code, bucket_name, expiry_time, sent_at
```

---

*SmartShare — MSc Project | Built with FastAPI, PostgreSQL, Kotlin & Jetpack Compose*
