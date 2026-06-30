package com.rohit.smartshare

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.rohit.smartshare.navigation.AppNavigation
import com.rohit.smartshare.ui.theme.SmartShareTheme
import com.rohit.smartshare.viewmodel.SharedInboxViewModel

class MainActivity : ComponentActivity() {
    private val sharedInboxViewModel: SharedInboxViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sharedUris = resolveSharedUris(intent)
        if (sharedUris.isNotEmpty()) sharedInboxViewModel.setPendingUris(sharedUris)
        setContent {
            SmartShareTheme {
                AppNavigation(sharedInboxViewModel = sharedInboxViewModel)
            }
        }
    }

    private fun resolveSharedUris(intent: Intent?): List<Uri> {
        if (intent == null) return emptyList()
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
                listOfNotNull(uri)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                @Suppress("UNCHECKED_CAST")
                (intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM) as? List<Uri>)
                    ?: emptyList()
            }
            else -> emptyList()
        }
    }
}
