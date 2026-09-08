package com.simple.medai.screens

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import com.simple.medai.data.SupabaseRepository
import com.simple.medai.models.SelectedBook

private enum class Screen { LOGIN, HOME, UPLOAD, STUDY }

@Composable
fun SimpleApp() {
    var screen by remember {
        mutableStateOf(
            if (SupabaseRepository.isLoggedIn()) Screen.HOME else Screen.LOGIN
        )
    }
    var book by remember { mutableStateOf<SelectedBook?>(null) }

    // Respeta el botón/gesto ATRÁS configurado por Android.
    // En Inicio no lo interceptamos: Android mantiene su comportamiento normal.
    BackHandler(enabled = screen == Screen.UPLOAD || screen == Screen.STUDY) {
        when (screen) {
            Screen.STUDY -> screen = Screen.UPLOAD
            Screen.UPLOAD -> {
                book = null
                screen = Screen.HOME
            }
            else -> Unit
        }
    }

    when (screen) {
        Screen.LOGIN -> LoginScreen(
            onLoginSuccess = { screen = Screen.HOME }
        )

        Screen.HOME -> HomeScreen(
            onUploadBook = { screen = Screen.UPLOAD },
            onLogout = {
                book = null
                screen = Screen.LOGIN
            }
        )

        Screen.UPLOAD -> UploadBookScreen(
            onBack = {
                book = null
                screen = Screen.HOME
            },
            onContinue = {
                book = it
                screen = Screen.STUDY
            }
        )

        Screen.STUDY -> StudySessionScreen(
            book = book,
            onBack = { screen = Screen.UPLOAD }
        )
    }
}
