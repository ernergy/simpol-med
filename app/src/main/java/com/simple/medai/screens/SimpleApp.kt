package com.simple.medai.screens

import androidx.compose.runtime.*
import com.simple.medai.data.SupabaseRepository

private enum class Screen {
    LOGIN,
    HOME,
    CHAT,
    STUDY
}

@Composable
fun SimpleApp() {

    var screen by remember {
        mutableStateOf(
            if (
                SupabaseRepository
                    .isLoggedIn()
            ) {
                Screen.HOME
            } else {
                Screen.LOGIN
            }
        )
    }

    when (screen) {

        Screen.LOGIN ->
            LoginScreen(
                onLoginSuccess = {
                    screen =
                        Screen.HOME
                }
            )

        Screen.HOME ->
            HomeScreen(
                onStartChat = {
                    screen =
                        Screen.CHAT
                },
                onStudyMode = {
                    screen =
                        Screen.STUDY
                },
                onLogout = {
                    screen =
                        Screen.LOGIN
                }
            )

        Screen.CHAT ->
            StudySessionScreen(
                onBack = {
                    screen =
                        Screen.HOME
                }
            )

        Screen.STUDY ->
            StudyModeScreen(
                onBack = {
                    screen =
                        Screen.HOME
                }
            )
    }
}
