package com.rohit.smartshare.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rohit.smartshare.api.FileResponse
import com.rohit.smartshare.api.RetrofitClient
import com.rohit.smartshare.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class FileViewModel(private val context: Context, private val bucketId: Int) : ViewModel() {

    private val _files = MutableStateFlow<List<FileResponse>>(emptyList())
    val files: StateFlow<List<FileResponse>> = _files

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _shareCode = MutableStateFlow("")
    val shareCode: StateFlow<String> = _shareCode

    private val _error = MutableStateFlow("")
    val error: StateFlow<String> = _error

    init { loadFiles() }

    fun loadFiles() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val token = SessionManager.getToken(context)
                val response = RetrofitClient.api.getFiles(token, bucketId)
                if (response.isSuccessful) _files.value = response.body() ?: emptyList()
            } catch (e: Exception) {
                _error.value = "Failed to load files"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun uploadFiles(files: List<Pair<Uri, String>>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = ""
            try {
                val token = SessionManager.getToken(context)
                for ((uri, fileName) in files) {
                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes() ?: continue
                    val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                    val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("file", fileName, requestBody)
                    RetrofitClient.api.uploadFile(token, bucketId, part)
                }
                loadFiles()
            } catch (e: Exception) {
                _error.value = "Upload failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun renameFile(fileId: Int, newName: String) {
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken(context)
                val response = RetrofitClient.api.renameFile(token, fileId, com.rohit.smartshare.api.FileRenameRequest(newName))
                if (response.isSuccessful) loadFiles()
                else _error.value = "Rename failed"
            } catch (e: Exception) {
                _error.value = "Rename failed"
            }
        }
    }

    fun uploadFile(uri: Uri, fileName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = ""
            try {
                val token = SessionManager.getToken(context)
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes() ?: return@launch
                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", fileName, requestBody)
                val response = RetrofitClient.api.uploadFile(token, bucketId, part)
                if (response.isSuccessful) {
                    loadFiles()
                } else {
                    _error.value = "Upload failed"
                }
            } catch (e: Exception) {
                _error.value = "Upload failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun downloadFile(fileId: Int, fileName: String, shareLink: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = ""
            try {
                withContext(Dispatchers.IO) {
                    val client = OkHttpClient()
                    val request = Request.Builder().url(shareLink).build()
                    val response = client.newCall(request).execute()

                    if (response.isSuccessful) {
                        val bytes = response.body?.bytes() ?: run {
                            _error.value = "Empty response"
                            return@withContext
                        }

                        val extension = fileName.substringAfterLast('.', "")
                        val mimeType = android.webkit.MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(extension.lowercase())
                            ?: "application/octet-stream"

                        // Save to Downloads using MediaStore (works on Android 10+)
                        val resolver = context.contentResolver
                        val contentValues = android.content.ContentValues().apply {
                            put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                            put(android.provider.MediaStore.Downloads.MIME_TYPE, mimeType)
                            put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
                        }

                        val collection = android.provider.MediaStore.Downloads.getContentUri(
                            android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY
                        )
                        val itemUri = resolver.insert(collection, contentValues)
                            ?: run {
                                _error.value = "Could not create file in Downloads"
                                return@withContext
                            }

                        resolver.openOutputStream(itemUri)?.use { it.write(bytes) }

                        // Mark as complete
                        contentValues.clear()
                        contentValues.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
                        resolver.update(itemUri, contentValues, null, null)

                        // Open the file
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(itemUri, mimeType)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        val resolvers = context.packageManager.queryIntentActivities(intent, 0)
                        if (resolvers.isNotEmpty()) {
                            context.startActivity(intent)
                        } else {
                            // File saved, no app to open but that's fine
                            _error.value = "Saved to Downloads. No app to open this file type."
                        }
                    } else {
                        _error.value = "Server error: ${response.code}"
                    }
                }
            } catch (e: Exception) {
                _error.value = "Download failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteFile(fileId: Int) {
        viewModelScope.launch {
            try {
                val token = SessionManager.getToken(context)
                RetrofitClient.api.deleteFile(token, fileId)
                loadFiles()
            } catch (e: Exception) {
                _error.value = "Delete failed"
            }
        }
    }

    fun generateShareCode() {
        viewModelScope.launch {
            _error.value = ""
            try {
                val token = SessionManager.getToken(context)
                val response = RetrofitClient.api.createShare(token, bucketId)
                if (response.isSuccessful) {
                    _shareCode.value = response.body()?.share_code ?: ""
                } else {
                    _error.value = "Failed to generate share code"
                }
            } catch (e: Exception) {
                _error.value = "Failed to generate share code"
            }
        }
    }
}
