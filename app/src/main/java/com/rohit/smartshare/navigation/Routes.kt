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

    const val FRIENDS = "friends"
    const val CHAT = "chat/{friendId}/{friendUsername}"
    const val CHAT_JOIN = "chat_join/{shareCode}"

    fun chat(friendId: Int, friendUsername: String) = "chat/$friendId/$friendUsername"
    fun chatJoin(shareCode: String) = "chat_join/$shareCode"

    fun bucketDetail(bucketId: Int, isOwner: Boolean = true) = "bucket_detail/$bucketId/$isOwner"

    const val MEDIA_PREVIEW = "media_preview/{encodedUrl}/{isVideo}"
    fun mediaPreview(url: String, isVideo: Boolean): String {
        val encoded = java.net.URLEncoder.encode(url, "UTF-8")
        return "media_preview/$encoded/$isVideo"
    }
}
