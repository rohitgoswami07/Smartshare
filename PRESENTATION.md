# SmartShare — Project Presentation Document
### MSc Project | Full-Stack Android File Sharing Platform

---

## 1. PROJECT OVERVIEW

**SmartShare** is a full-stack mobile application that allows users to organize and share files through a concept called **buckets**. Instead of sharing files one by one, users group files into named buckets (e.g., "CS Assignment 3", "Team Project", "Lecture Notes") and share the entire bucket with friends using a time-limited share code.

**Live Backend URL:** `https://smartshare-txa0.onrender.com`  
**GitHub Repository:** `https://github.com/rohitgoswami07/Smartshare`

---

## 2. TECH STACK

### Android (Frontend)
| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Kotlin | 2.1.20 |
| UI Framework | Jetpack Compose | BOM 2024.12.01 |
| Architecture | MVVM + Repository Pattern | — |
| Navigation | Jetpack Navigation Compose | 2.8.5 |
| Networking | Retrofit2 + OkHttp3 | 2.11.0 / 4.12.0 |
| Local Cache | Room Database | 2.7.1 |
| Image Loading | Coil | 2.6.0 |
| Video Playback | ExoPlayer (Media3) | 1.3.1 |
| Min SDK | Android 8.0 (API 26) | — |

### Backend
| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Python | 3.10+ |
| Framework | FastAPI | 0.137.2 |
| ORM | SQLAlchemy | 2.0.51 |
| Schema Validation | Pydantic | 2.13.4 |
| Authentication | JWT (python-jose) | 3.5.0 |
| Password Hashing | SHA-256 (hashlib) | — |
| File Storage | Cloudinary | 1.41.0 |
| Database | PostgreSQL | — |
| Server | Uvicorn | 0.49.0 |
| Deployment | Render | — |

---

## 3. SYSTEM ARCHITECTURE

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

### Architecture Pattern — MVVM
- **View (Compose UI):** Screens observe state from ViewModel using StateFlow
- **ViewModel:** Holds UI state, calls Repository, survives configuration changes
- **Repository:** Single source of truth — fetches from API or Room cache
- **Model:** Room entities (local) + API response data classes (remote)

---

## 4. DATABASE SCHEMA

```
users
  id, username, email, password_hash, created_at

buckets
  id, name, owner_id (FK → users), created_at

files
  id, bucket_id (FK → buckets), uploaded_by (FK → users),
  filename, filepath (Cloudinary URL), cloudinary_public_id,
  size, uploaded_at

shares
  id, bucket_id (FK → buckets), share_code (unique), expiry_time

shared_access_logs
  id, share_id (FK → shares), user_id (FK → users), accessed_at

friendships
  id, user_id (FK → users), friend_id (FK → users), created_at

code_messages
  id, sender_id (FK → users), receiver_id (FK → users),
  share_code, bucket_name, expiry_time, sent_at
```

---

## 5. FEATURES — SCREEN BY SCREEN

### 5.1 Login Screen
- Email + password login with JWT token returned from backend
- Password visibility toggle
- "Forgot Password" link
- Success banner shown after registration
- Token and session saved locally via SessionManager

### 5.2 Register Screen
- Username, email, password fields
- On success, redirects to Login with success banner

### 5.3 Forgot Password Screen
- Enter registered email + new password
- Directly resets password on backend (no OTP — simplified for MSc scope)

### 5.4 Home Screen
- Welcome banner showing username and bucket count (tappable → Profile)
- "Join via Share Code" quick action card
- Full list of user's own buckets with rename and delete options
- "Recently Accessed via Share Code" section showing shared buckets accessed in last 24 hours
- FAB to create a new bucket
- Buckets reload automatically every time screen comes back into focus (ON_RESUME lifecycle)
- Supports receiving files shared from other apps (Android Share Intent)

### 5.5 Bucket Detail Screen
- Lists all files inside a bucket
- Upload files (single or multiple) using file picker
- Pull-to-refresh to see latest uploads from other users
- Generate share code (24-hour expiry) — owner only
- Copy share code to clipboard
- Download files directly to device Downloads folder
- Rename and delete files (owner or uploader only)
- In-app image preview (tap thumbnail)
- In-app video preview (tap play icon)
- Shows "Shared view" subtitle when accessed as a guest

### 5.6 Share Screen
- Enter a share code manually to access a shared bucket
- Auto-lookup if code is pre-filled from chat
- Shows bucket name and owner on success
- "Open Bucket" button navigates directly to the bucket

### 5.7 Friends Screen
- Search and add friends by username
- View full friends list
- Remove friends
- Tap a friend to open chat

### 5.8 Chat Screen
- Conversation view between two users
- Messages are share codes sent for specific buckets
- Tap a message to go directly to that bucket
- Only non-expired messages are shown
- Send a bucket share code directly to a friend

### 5.9 Profile Screen
- View current username and email
- Edit username
- Change password (requires current password)
- Logout

### 5.10 Media Preview Screen
- Full-screen image viewer
- Full-screen video player using ExoPlayer (Media3)

---

## 6. API REFERENCE

> All authenticated endpoints require: `Authorization: Bearer <token>`

### Authentication
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/register` | No | Register new user |
| POST | `/login` | No | Login, returns JWT |
| POST | `/forgot-password` | No | Reset password by email |
| PUT | `/profile` | Yes | Update username |
| POST | `/change-password` | Yes | Change password |

### Buckets
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/buckets` | Yes | Get all owned buckets |
| POST | `/bucket` | Yes | Create a bucket |
| PUT | `/bucket/{id}` | Yes | Rename a bucket |
| DELETE | `/bucket/{id}` | Yes | Delete bucket + all files |

### Files
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/files/{bucketId}` | Yes | List files in bucket |
| POST | `/upload/{bucketId}` | Yes | Upload file to bucket |
| PUT | `/file/{id}` | Yes | Rename a file |
| DELETE | `/file/{id}` | Yes | Delete a file |
| GET | `/download/{id}` | No | Redirect to file URL |

### Sharing
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/share/{bucketId}` | Yes | Generate share code (24hr) |
| GET | `/share/{code}` | Yes | Validate and lookup share code |
| GET | `/shared-buckets` | Yes | Get recently accessed shared buckets |

### Friends & Messages
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/friends` | Yes | Get friend list |
| POST | `/friends/{username}` | Yes | Add friend by username |
| DELETE | `/friends/{friendId}` | Yes | Remove a friend |
| POST | `/messages/{friendId}/{bucketId}` | Yes | Send share code to friend |
| GET | `/messages/{friendId}` | Yes | Get message history |

---

## 7. KEY TECHNICAL DECISIONS

### Why Buckets?
Traditional file sharing apps share files individually. Buckets allow grouping related files (e.g., all files for one assignment) and sharing them as a unit — reducing friction for academic and team use cases.

### Why FastAPI?
FastAPI provides automatic OpenAPI documentation (`/docs`), async support, and Pydantic validation out of the box — making it ideal for rapid API development with strong type safety.

### Why Cloudinary?
Cloudinary handles file storage, CDN delivery, and supports all file types including images, videos, and raw files. It removes the need to manage file storage infrastructure manually.

### Why Room Database?
Room provides a local cache on the Android device so the app can show previously loaded data instantly while fresh data is being fetched from the server.

### Why JWT?
JWT tokens are stateless — the server doesn't need to store sessions. Each token contains the user ID and expires after 24 hours, making it simple and scalable.

### Share Code Access Logic
A user can access a shared bucket in two ways:
1. **Via Friends Chat** — a friend sends them a share code through the in-app chat
2. **Via Manual Entry** — they type the share code directly in the Share screen

Both paths are validated on the backend before granting file access.

---

## 8. PROJECT STRUCTURE

```
SmartShare/
├── app/src/main/java/com/rohit/smartshare/
│   ├── api/
│   │   ├── ApiModels.kt          — All request/response data classes
│   │   ├── ApiService.kt         — Retrofit interface with all endpoints
│   │   └── RetrofitClient.kt     — OkHttp + Retrofit singleton
│   ├── data/
│   │   ├── AppDatabase.kt        — Room database definition
│   │   ├── BucketDao.kt          — Bucket DB queries
│   │   ├── FileDao.kt            — File DB queries
│   │   ├── ShareDao.kt           — Share DB queries
│   │   └── UserDao.kt            — User DB queries
│   ├── navigation/
│   │   ├── AppNavigation.kt      — NavHost with all routes
│   │   └── Routes.kt             — Route constants and builders
│   ├── repository/
│   │   ├── BucketRepository.kt   — Bucket data operations
│   │   ├── FileRepository.kt     — File data operations
│   │   ├── ShareRepository.kt    — Share data operations
│   │   └── UserRepository.kt     — User data operations
│   ├── screens/
│   │   ├── LoginScreen.kt
│   │   ├── RegisterScreen.kt
│   │   ├── ForgotPasswordScreen.kt
│   │   ├── HomeScreen.kt
│   │   ├── BucketDetailScreen.kt
│   │   ├── ShareScreen.kt
│   │   ├── FriendsScreen.kt
│   │   ├── ChatScreen.kt
│   │   ├── ProfileScreen.kt
│   │   └── MediaPreviewScreen.kt
│   ├── utils/
│   │   ├── SessionManager.kt     — SharedPreferences token/session storage
│   │   └── PasswordUtils.kt      — Password utility functions
│   └── viewmodel/
│       ├── LoginViewModel.kt
│       ├── RegisterViewModel.kt
│       ├── HomeViewModel.kt
│       ├── BucketViewModel.kt
│       ├── FileViewModel.kt
│       ├── ShareViewModel.kt
│       ├── FriendsViewModel.kt
│       ├── ProfileViewModel.kt
│       └── SharedInboxViewModel.kt
│
└── backend/
    ├── main.py          — App entry point, DB migrations
    ├── routes.py        — All API route handlers
    ├── models.py        — SQLAlchemy ORM models
    ├── schemas.py       — Pydantic request/response schemas
    ├── auth.py          — JWT creation and verification
    ├── database.py      — DB engine and session management
    ├── render.yaml      — Render deployment configuration
    └── requirements.txt — Python dependencies
```

---

## 9. SECURITY

| Area | Implementation |
|------|---------------|
| Authentication | JWT tokens, 24-hour expiry |
| Password Storage | SHA-256 hashing |
| Secrets | Environment variables via `.env`, never committed to Git |
| HTTP Logging | Disabled in release builds (debug only) |
| File Access | Ownership + share code validation on every API request |
| Share Expiry | All share codes expire after 24 hours automatically |
| Access Logs | Every share code usage logged with user ID and timestamp |
| CORS | Configured on backend (currently open for development) |

---

## 10. DEPLOYMENT

| Service | Platform | Details |
|---------|----------|---------|
| Backend API | Render (Web Service) | Auto-deploys from GitHub `main` branch |
| Database | Render PostgreSQL | Internal to Render, not publicly accessible |
| File Storage | Cloudinary | Files stored with folder structure `smartshare/bucket_{id}/` |

### Environment Variables on Render
```
DATABASE_URL        — PostgreSQL connection string
SECRET_KEY          — JWT signing secret
CLOUDINARY_CLOUD_NAME
CLOUDINARY_API_KEY
CLOUDINARY_API_SECRET
```

---

## 11. HOW THE SHARING FLOW WORKS

```
User A (Owner)                    User B (Guest)
─────────────────                 ─────────────────
1. Creates a bucket
2. Uploads files
3. Taps "Generate Share Code"
   → Backend creates 8-char code
   → Expires in 24 hours

Option A — Via Friends:
4. Opens Friends → Chat with B
5. Sends bucket share code
   → Code message stored in DB    6. Receives message in chat
                                  7. Taps message → Share Screen
                                  8. Code auto-filled, taps Join
                                  9. Sees bucket + files ✓

Option B — Manual:
4. Copies code, sends via         6. Opens Share Screen
   WhatsApp/SMS etc.              7. Types code manually, taps Join
                                  8. Sees bucket + files ✓

Both users can now upload files to the same bucket.
Pull down to refresh and see each other's uploads.
```

---

## 12. KNOWN LIMITATIONS & FUTURE IMPROVEMENTS

| Limitation | Future Improvement |
|------------|-------------------|
| Password reset has no email OTP | Add email verification via SMTP |
| SHA-256 password hashing | Upgrade to bcrypt or Argon2 |
| No file size limit on upload | Add max file size validation (e.g., 50MB) |
| Share code access is open (any valid code = access) | Restrict to specific invited users only |
| No push notifications | Add FCM for real-time upload notifications |
| Token stored in plain SharedPreferences | Migrate to EncryptedSharedPreferences |
| No pagination on file lists | Add pagination for large buckets |

---

*SmartShare — MSc Project | Kotlin + Jetpack Compose + FastAPI + PostgreSQL + Cloudinary*
