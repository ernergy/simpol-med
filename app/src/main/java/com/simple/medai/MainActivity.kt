package com.simple.medai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.simple.medai.screens.SimpleApp
import com.simple.medai.ui.theme.SIMPLETheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SIMPLETheme {
                SimpleApp()
            }
        }
    }
}
