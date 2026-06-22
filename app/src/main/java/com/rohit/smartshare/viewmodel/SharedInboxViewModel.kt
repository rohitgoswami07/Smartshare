package com.rohit.smartshare.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SharedInboxViewModel : ViewModel() {
    private val _pendingUris = MutableStateFlow<List<Uri>>(emptyList())
    val pendingUris: StateFlow<List<Uri>> = _pendingUris

    fun setPendingUris(uris: List<Uri>) { _pendingUris.value = uris }
    fun consume() { _pendingUris.value = emptyList() }
}
