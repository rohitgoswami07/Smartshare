# SmartShare — Bucket-Based File Sharing Platform

An Android application for organized file sharing using a bucket-based approach. Users create buckets for different purposes (academics, projects, teams) and manage files within them.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Android Frontend | Kotlin, Jetpack Compose, MVVM |
| Backend API | FastAPI (Python) |
| Database | PostgreSQL |
| Auth | JWT Tokens + SHA-256 password hashing |
| Networking | Retrofit2, OkHttp3 |

## Features

- User registration and login with JWT authentication
- Bucket creation, renaming and deletion
- File upload and download
- Share buckets via generated share codes (24hr expiry)
- Join shared buckets via share code

## Project Structure

```
SmartShare/
├── app/                    # Android application
│   └── src/main/java/com/rohit/smartshare/
│       ├── api/            # Retrofit API service and models
│       ├── data/           # Room DB (local cache)
│       ├── navigation/     # Navigation routes
│       ├── repository/     # Data repositories
│       ├── screens/        # Compose UI screens
│       ├── utils/          # SessionManager, PasswordUtils
│       └── viewmodel/      # ViewModels
└── backend/                # FastAPI backend
    ├── main.py             # Entry point
    ├── routes.py           # API endpoints
    ├── models.py           # SQLAlchemy models
    ├── schemas.py          # Pydantic schemas
    ├── auth.py             # JWT + password hashing
    ├── database.py         # DB connection
    └── requirements.txt    # Python dependencies
```

## Backend Setup

### 1. PostgreSQL
```bash
sudo -u postgres psql -c "CREATE DATABASE smartshare;"
sudo -u postgres psql -c "CREATE USER smartshare_user WITH PASSWORD 'yourpassword';"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE smartshare TO smartshare_user;"
sudo -u postgres psql -c "ALTER DATABASE smartshare OWNER TO smartshare_user;"
```

### 2. Environment Variables
```bash
cd backend
cp .env.example .env
# Edit .env with your values
```

### 3. Install Dependencies
```bash
pip3 install -r requirements.txt
```

### 4. Run
```bash
python3 main.py
```

API docs available at: `http://localhost:8000/docs`

## Android Setup

1. Open project in Android Studio
2. Update `BASE_URL` in `app/src/main/java/com/rohit/smartshare/api/RetrofitClient.kt` to point to your backend
3. Build and run on device or emulator

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/register` | Register new user |
| POST | `/login` | Login and get JWT token |
| POST | `/forgot-password` | Reset password |
| GET | `/buckets` | Get all user buckets |
| POST | `/bucket` | Create bucket |
| PUT | `/bucket/{id}` | Rename bucket |
| DELETE | `/bucket/{id}` | Delete bucket + files |
| GET | `/files/{bucketId}` | Get files in bucket |
| POST | `/upload/{bucketId}` | Upload file to bucket |
| DELETE | `/file/{id}` | Delete file |
| GET | `/download/{id}` | Download file |
| POST | `/share/{bucketId}` | Generate share code |
| GET | `/share/{code}` | Lookup share code |
