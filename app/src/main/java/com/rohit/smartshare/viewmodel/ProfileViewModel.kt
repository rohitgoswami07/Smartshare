package com.rohit.smartshare.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rohit.smartshare.api.ChangePasswordRequest
import com.rohit.smartshare.api.RetrofitClient
import com.rohit.smartshare.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(private val context: Context) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun changePassword(
        currentPassword: String,
        newPassword: String,
        onResult: (success: Boolean, error: String?) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = SessionManager.getToken(context)
                val response = RetrofitClient.api.changePassword(
                    token,
                    ChangePasswordRequest(currentPassword, newPassword)
                )
                if (response.isSuccessful) {
                    onResult(true, null)
                } else {
                    val error = if (response.code() == 400) "Current password is incorrect" else "Failed to change password"
                    onResult(false, error)
                }
            } catch (e: Exception) {
                onResult(false, "Cannot connect to server")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
