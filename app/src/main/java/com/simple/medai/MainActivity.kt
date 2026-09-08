package com.simple.medai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.simple.medai.screens.SimpleApp
import com.simple.medai.ui.theme.SIMPLETheme
import io.github.jan.supabase.auth.handleDeeplinks

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleAuthIntent(intent, false)

        setContent {
            SIMPLETheme {
                SimpleApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent, true)
    }

    private fun handleAuthIntent(intent: Intent, recreateAfterSuccess: Boolean) {
        SupabaseManager.client.handleDeeplinks(
            intent = intent,
            onSessionSuccess = {
                if (recreateAfterSuccess) {
                    runOnUiThread { recreate() }
                }
            }
        )
    }
}
