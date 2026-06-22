package com.rohit.smartshare.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rohit.smartshare.api.RetrofitClient
import com.rohit.smartshare.api.ShareLookupResponse
import com.rohit.smartshare.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ShareViewModel(private val context: Context) : ViewModel() {

    private val _shareResult = MutableStateFlow<ShareLookupResponse?>(null)
    val shareResult: StateFlow<ShareLookupResponse?> = _shareResult

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage

    fun lookupShare(code: String) {
        viewModelScope.launch {
            _errorMessage.value = ""
            _shareResult.value = null
            try {
                val response = RetrofitClient.api.lookupShare(code)
                if (response.isSuccessful) {
                    _shareResult.value = response.body()
                } else {
                    _errorMessage.value = if (response.code() == 410) "Share code has expired" else "Invalid share code"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Cannot connect to server"
            }
        }
    }
}
