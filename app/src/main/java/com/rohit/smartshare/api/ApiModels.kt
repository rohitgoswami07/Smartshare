package com.rohit.smartshare.api

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class TokenResponse(
    val token: String,
    val user_id: Int,
    val username: String,
    val email: String
)

data class BucketRequest(
    val name: String
)

data class BucketResponse(
    val id: Int,
    val name: String,
    val owner_id: Int,
    val created_at: Long
)

data class FileResponse(
    val id: Int,
    val bucket_id: Int,
    val filename: String,
    val size: Int,
    val uploaded_at: Long,
    val share_link: String,
    val uploaded_by: Int?,
    val is_mine: Boolean
)

data class ShareResponse(
    val share_code: String,
    val bucket_id: Int,
    val expiry_time: Long
)

data class ShareLookupResponse(
    val bucket_id: Int,
    val bucket_name: String,
    val share_code: String,
    val owner_username: String
)

data class SharedAccessedBucket(
    val bucket_id: Int,
    val bucket_name: String,
    val share_code: String,
    val owner_username: String,
    val accessed_at: Long
)

data class ForgotPasswordRequest(
    val email: String,
    val new_password: String
)

data class FileRenameRequest(
    val filename: String
)

data class ChangePasswordRequest(
    val current_password: String,
    val new_password: String
)

data class UpdateProfileRequest(
    val username: String
)

data class MessageResponse(
    val message: String
)
