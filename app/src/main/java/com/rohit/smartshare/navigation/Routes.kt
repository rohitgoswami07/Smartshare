package com.rohit.smartshare.navigation

object Routes {
    const val LOGIN = "login"
    const val LOGIN_REGISTERED = "login?registered=true"
    const val REGISTER = "register"
    const val HOME = "home"
    const val JOIN = "join"
    const val BUCKET = "bucket"
    const val BUCKET_DETAIL = "bucket_detail/{bucketId}"
    const val SHARE = "share"
    const val FORGOT_PASSWORD = "forgot_password"

    fun bucketDetail(bucketId: Int) = "bucket_detail/$bucketId"
}