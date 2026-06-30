package com.rohit.smartshare.navigation

object Routes {
    const val LOGIN = "login"
    const val LOGIN_REGISTERED = "login?registered=true"
    const val REGISTER = "register"
    const val HOME = "home"
    const val JOIN = "join"
    const val BUCKET = "bucket"
    const val BUCKET_DETAIL = "bucket_detail/{bucketId}/{isOwner}"
    const val SHARE = "share"
    const val PROFILE = "profile"
    const val FORGOT_PASSWORD = "forgot_password"

    fun bucketDetail(bucketId: Int, isOwner: Boolean = true) = "bucket_detail/$bucketId/$isOwner"

    const val MEDIA_PREVIEW = "media_preview/{encodedUrl}/{isVideo}"
    fun mediaPreview(url: String, isVideo: Boolean): String {
        val encoded = java.net.URLEncoder.encode(url, "UTF-8")
        return "media_preview/$encoded/$isVideo"
    }
}
