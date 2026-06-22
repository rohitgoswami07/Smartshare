package com.rohit.smartshare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rohit.smartshare.api.RegisterRequest
import com.rohit.smartshare.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun validateRegistration(
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ): String {
        return when {
            username.isBlank() -> "Username is required"
            email.isBlank() -> "Email is required"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Enter a valid email address"
            password.isBlank() -> "Password is required"
            password.length < 8 -> "Password must be at least 8 characters"
            password != confirmPassword -> "Passwords do not match"
            else -> ""
        }
    }

    fun registerUser(
        username: String,
        email: String,
        password: String,
        onResult: (success: Boolean, error: String?) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.api.register(
                    RegisterRequest(username, email, password)
                )
                if (response.isSuccessful) {
                    onResult(true, null)
                } else {
                    val error = if (response.code() == 400) "Email already registered" else "Registration failed"
                    onResult(false, error)
                }
            } catch (e: Exception) {
                onResult(false, "Cannot connect to server. Check your network.")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
