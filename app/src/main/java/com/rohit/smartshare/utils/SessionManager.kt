package com.rohit.smartshare.utils

import android.content.Context

object SessionManager {

    private const val PREF_NAME = "smartshare_session"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_EMAIL = "email"
    private const val KEY_USERNAME = "username"
    private const val KEY_TOKEN = "token"

    fun saveSession(context: Context, userId: Int, email: String, username: String = "", token: String = "") {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_EMAIL, email)
            .putString(KEY_USERNAME, username)
            .putString(KEY_TOKEN, token)
            .apply()
    }

    fun getSession(context: Context): Pair<Int, String>? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val userId = prefs.getInt(KEY_USER_ID, -1)
        val email = prefs.getString(KEY_EMAIL, null)
        return if (userId != -1 && email != null) Pair(userId, email) else null
    }

    fun getToken(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return "Bearer ${prefs.getString(KEY_TOKEN, "") ?: ""}"
    }

    fun getUsername(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USERNAME, "") ?: ""
    }

    fun clearSession(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
