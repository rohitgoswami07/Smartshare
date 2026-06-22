package com.rohit.smartshare.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rohit.smartshare.api.BucketRequest
import com.rohit.smartshare.api.BucketResponse
import com.rohit.smartshare.api.RetrofitClient
import com.rohit.smartshare.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val context: Context) : ViewModel() {

    private val _buckets = MutableStateFlow<List<BucketResponse>>(emptyList())
    val buckets: StateFlow<List<BucketResponse>> = _buckets

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        _username.value = SessionManager.getUsername(context)
        loadBuckets()
    }

    fun loadBuckets() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = SessionManager.getToken(context)
                val response = RetrofitClient.api.getBuckets(token)
                if (response.isSuccessful) _buckets.value = response.body() ?: emptyList()
            } catch (e: Exception) {
                // network error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createBucket(name: String) {
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken(context)
                val response = RetrofitClient.api.createBucket(token, BucketRequest(name))
                if (response.isSuccessful) loadBuckets()
            } catch (e: Exception) { /* ignore */ }
        }
    }

    fun renameBucket(bucketId: Int, newName: String) {
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken(context)
                val response = RetrofitClient.api.renameBucket(token, bucketId, BucketRequest(newName))
                if (response.isSuccessful) loadBuckets()
            } catch (e: Exception) { /* ignore */ }
        }
    }

    fun deleteBucket(bucketId: Int) {
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken(context)
                RetrofitClient.api.deleteBucket(token, bucketId)
                loadBuckets()
            } catch (e: Exception) { /* ignore */ }
        }
    }
}
