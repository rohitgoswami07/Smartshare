package com.rohit.smartshare.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rohit.smartshare.api.BucketResponse
import com.rohit.smartshare.api.CodeMessageResponse
import com.rohit.smartshare.api.FriendResponse
import com.rohit.smartshare.api.RetrofitClient
import com.rohit.smartshare.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FriendsViewModel(private val context: Context) : ViewModel() {

    private val _friends = MutableStateFlow<List<FriendResponse>>(emptyList())
    val friends: StateFlow<List<FriendResponse>> = _friends

    private val _messages = MutableStateFlow<List<CodeMessageResponse>>(emptyList())
    val messages: StateFlow<List<CodeMessageResponse>> = _messages

    private val _buckets = MutableStateFlow<List<BucketResponse>>(emptyList())
    val buckets: StateFlow<List<BucketResponse>> = _buckets

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message

    private val _error = MutableStateFlow("")
    val error: StateFlow<String> = _error

    val currentUserId: Int get() = SessionManager.getSession(context)?.first ?: 0

    init { loadFriends() }

    fun loadFriends() {
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken(context)
                val r = RetrofitClient.api.getFriends(token)
                if (r.isSuccessful) _friends.value = r.body() ?: emptyList()
            } catch (e: Exception) { /* silent fail, keep existing list */ }
        }
    }

    fun addFriend(username: String) {
        viewModelScope.launch {
            _error.value = ""
            try {
                val token = SessionManager.getToken(context)
                val r = RetrofitClient.api.addFriend(token, username)
                if (r.isSuccessful) { loadFriends(); _message.value = "@$username added!" }
                else _error.value = when (r.code()) {
                    404 -> "User not found"
                    400 -> "Already friends or can't add yourself"
                    else -> "Failed to add friend"
                }
            } catch (e: Exception) { _error.value = "Cannot connect to server" }
        }
    }

    fun removeFriend(friendId: Int) {
        viewModelScope.launch {
            _error.value = ""
            try {
                val token = SessionManager.getToken(context)
                RetrofitClient.api.removeFriend(token, friendId)
                loadFriends()
                _message.value = "Friend removed"
            } catch (e: Exception) { _error.value = "Failed to remove friend" }
        }
    }

    fun loadMessages(friendId: Int) {
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken(context)
                val r = RetrofitClient.api.getMessages(token, friendId)
                if (r.isSuccessful) _messages.value = r.body() ?: emptyList()
            } catch (e: Exception) { _error.value = "Failed to load messages" }
        }
    }

    fun loadBuckets() {
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken(context)
                val r = RetrofitClient.api.getBuckets(token)
                if (r.isSuccessful) _buckets.value = r.body() ?: emptyList()
            } catch (e: Exception) {}
        }
    }

    fun sendCode(friendId: Int, bucketId: Int) {
        viewModelScope.launch {
            _error.value = ""
            try {
                val token = SessionManager.getToken(context)
                val r = RetrofitClient.api.sendCode(token, friendId, bucketId)
                if (r.isSuccessful) { loadMessages(friendId); _message.value = "Code sent!" }
                else _error.value = "Failed to send code"
            } catch (e: Exception) { _error.value = "Cannot connect to server" }
        }
    }

    fun consumeMessage() { _message.value = "" }
    fun consumeError() { _error.value = "" }
}

class FriendsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return FriendsViewModel(context) as T
    }
}
