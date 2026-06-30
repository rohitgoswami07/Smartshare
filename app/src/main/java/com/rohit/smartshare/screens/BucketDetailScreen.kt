package com.rohit.smartshare.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.rohit.smartshare.navigation.Routes
import com.rohit.smartshare.viewmodel.FileViewModel
import com.rohit.smartshare.viewmodel.FileViewModelFactory
import com.rohit.smartshare.viewmodel.SharedInboxViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BucketDetailScreen(navController: NavController, bucketId: Int, isOwner: Boolean = true) {
    val context = LocalContext.current
    val factory = remember { FileViewModelFactory(context, bucketId) }
    val fileViewModel: FileViewModel = viewModel(key = "file_$bucketId", factory = factory)
    val sharedInboxViewModel: SharedInboxViewModel = viewModel()
    val pendingUris by sharedInboxViewModel.pendingUris.collectAsStateWithLifecycle()

    val files by fileViewModel.files.collectAsStateWithLifecycle()
    val isLoading by fileViewModel.isLoading.collectAsStateWithLifecycle()
    val shareCode by fileViewModel.shareCode.collectAsStateWithLifecycle()
    val error by fileViewModel.error.collectAsStateWithLifecycle()
    val message by fileViewModel.message.collectAsStateWithLifecycle()

    var selectedFiles by remember { mutableStateOf<List<Pair<Uri, String>>>(emptyList()) }
    LaunchedEffect(pendingUris) {
        if (pendingUris.isNotEmpty()) {
            val resolved = pendingUris.map { uri ->
                val cursor = context.contentResolver.query(
                    uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null
                )
                val name = cursor?.use { if (it.moveToFirst()) it.getString(0) else null } ?: uri.lastPathSegment ?: "file"
                uri to name
            }
            selectedFiles = resolved
            sharedInboxViewModel.consume()
        }
    }

    var showRenameDialog by remember { mutableStateOf(false) }
    var renameFileId by remember { mutableStateOf(0) }
    var renameText by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(message) {
        if (message.isNotEmpty()) {
            snackbarHostState.showSnackbar(message)
            fileViewModel.consumeMessage()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        val picked = uris.map { uri ->
            val cursor = context.contentResolver.query(
                uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null
            )
            val name = cursor?.use { if (it.moveToFirst()) it.getString(0) else null } ?: uri.lastPathSegment ?: "file"
            uri to name
        }
        selectedFiles = selectedFiles + picked
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename File", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("New Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (renameText.isNotBlank()) {
                        fileViewModel.renameFile(renameFileId, renameText)
                        showRenameDialog = false
                        renameText = ""
                    }
                }, shape = RoundedCornerShape(10.dp)) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Bucket Files", fontWeight = FontWeight.Bold)
                        if (!isOwner) Text(
                            "Shared view — you can manage your own uploads",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Upload section — available to everyone (owner + shared users)
            item {
                Text("Upload Files", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (selectedFiles.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(selectedFiles) { (uri, name) ->
                                    InputChip(
                                        selected = false,
                                        onClick = {},
                                        label = { Text(name, maxLines = 1) },
                                        trailingIcon = {
                                            IconButton(
                                                onClick = { selectedFiles = selectedFiles.filter { it.first != uri } },
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (error.isNotEmpty()) {
                            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text(if (selectedFiles.isEmpty()) "Pick Files" else "Add More") }

                            Button(
                                onClick = {
                                    if (selectedFiles.isNotEmpty()) {
                                        fileViewModel.uploadFiles(selectedFiles)
                                        selectedFiles = emptyList()
                                    }
                                },
                                enabled = selectedFiles.isNotEmpty() && !isLoading,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                else Text("Upload ${if (selectedFiles.size > 1) "(${selectedFiles.size})" else ""}")
                            }
                        }
                    }
                }
            }

            item { HorizontalDivider() }

            // Share code section — only for owner
            if (isOwner) {
                item {
                    Text("Share This Bucket", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Button(
                                onClick = { fileViewModel.generateShareCode() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("Generate Share Code") }

                            if (shareCode.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Share Code", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(shareCode, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                    IconButton(onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Share Code", shareCode))
                                        scope.launch { snackbarHostState.showSnackbar("Share code copied!") }
                                    }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Text("Valid for 24 hours", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                item { HorizontalDivider() }
            }

            // Files section
            item {
                Text("Files (${files.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            if (files.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.InsertDriveFile, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No files yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(files) { file ->
                    val ext = file.filename.substringAfterLast('.', "").lowercase()
                    val isImage = ext in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
                    val isVideo = ext in listOf("mp4", "mkv", "mov", "avi", "webm")
                    // Can edit/delete if owner OR if this file was uploaded by current user
                    val canModify = isOwner || file.is_mine

                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isImage) {
                                    AsyncImage(
                                        model = file.share_link,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(48.dp).clickable {
                                            navController.navigate(Routes.mediaPreview(file.share_link, false))
                                        }
                                    )
                                } else if (isVideo) {
                                    Box(
                                        modifier = Modifier.size(48.dp).clickable {
                                            navController.navigate(Routes.mediaPreview(file.share_link, true))
                                        },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.PlayCircle, contentDescription = "Play", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                } else {
                                    Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Column {
                                    Text(file.filename, fontWeight = FontWeight.Medium)
                                    Text("${file.size / 1024} KB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (!isOwner && file.is_mine) {
                                        Text("Uploaded by you", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                            Row {
                                if (canModify) {
                                    IconButton(onClick = {
                                        renameFileId = file.id
                                        renameText = file.filename
                                        showRenameDialog = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Rename", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                IconButton(onClick = {
                                    fileViewModel.downloadFile(file.id, file.filename, file.share_link)
                                }) {
                                    Icon(Icons.Default.Download, contentDescription = "Download", tint = MaterialTheme.colorScheme.primary)
                                }
                                if (canModify) {
                                    IconButton(onClick = { fileViewModel.deleteFile(file.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
