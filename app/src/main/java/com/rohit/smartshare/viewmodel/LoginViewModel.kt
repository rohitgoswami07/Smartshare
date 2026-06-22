package com.rohit.smartshare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rohit.smartshare.api.LoginRequest
import com.rohit.smartshare.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun validateLogin(email: String, password: String): String {
        return when {
            email.isBlank() -> "Email is required"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Enter a valid email"
            password.isBlank() -> "Password is required"
            password.length < 8 -> "Password must be at least 8 characters"
            else -> ""
        }
    }

    fun forgotPassword(
        email: String,
        newPassword: String,
        onResult: (success: Boolean, error: String?) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.api.forgotPassword(
                    com.rohit.smartshare.api.ForgotPasswordRequest(email, newPassword)
                )
                if (response.isSuccessful) onResult(true, null)
                else onResult(false, "No account found with this email")
            } catch (e: Exception) {
                onResult(false, "Cannot connect to server. Check your network.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loginUser(
        email: String,
        password: String,
        onResult: (userId: Int?, email: String?, username: String?, token: String?, error: String?) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.api.login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    val body = response.body()!!
                    onResult(body.user_id, body.email, body.username, body.token, null)
                } else {
                    onResult(null, null, null, null, "Invalid email or password")
                }
            } catch (e: Exception) {
                onResult(null, null, null, null, "Cannot connect to server. Check your network.")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
