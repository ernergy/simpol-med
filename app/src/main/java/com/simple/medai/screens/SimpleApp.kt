package com.simple.medai.screens

import androidx.compose.runtime.*
import com.simple.medai.data.SupabaseRepository
import com.simple.medai.models.SelectedBook

private enum class Screen { LOGIN, HOME, UPLOAD, STUDY }

@Composable
fun SimpleApp() {
    var devMode by remember { mutableStateOf(false) }
    var screen by remember {
        mutableStateOf(if (SupabaseRepository.isLoggedIn()) Screen.HOME else Screen.LOGIN)
    }
    var book by remember { mutableStateOf<SelectedBook?>(null) }

    when (screen) {
        Screen.LOGIN -> LoginScreen(
            onLoginSuccess = { devMode = false; screen = Screen.HOME },
            onDevelopmentAccess = { devMode = true; screen = Screen.HOME }
        )
        Screen.HOME -> HomeScreen(
            developmentMode = devMode,
            onUploadBook = { screen = Screen.UPLOAD },
            onLogout = { devMode = false; book = null; screen = Screen.LOGIN }
        )
        Screen.UPLOAD -> UploadBookScreen(
            onBack = { screen = Screen.HOME },
            onContinue = { book = it; screen = Screen.STUDY }
        )
        Screen.STUDY -> StudySessionScreen(
            book = book,
            developmentMode = devMode,
            onBack = { screen = Screen.UPLOAD }
        )
    }
}
