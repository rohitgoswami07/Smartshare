package com.rohit.smartshare.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) ->
                LoginViewModel() as T
            modelClass.isAssignableFrom(RegisterViewModel::class.java) ->
                RegisterViewModel() as T
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(context) as T
            modelClass.isAssignableFrom(BucketViewModel::class.java) ->
                BucketViewModel(context) as T
            modelClass.isAssignableFrom(ShareViewModel::class.java) ->
                ShareViewModel(context) as T
            modelClass.isAssignableFrom(ProfileViewModel::class.java) ->
                ProfileViewModel(context) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
