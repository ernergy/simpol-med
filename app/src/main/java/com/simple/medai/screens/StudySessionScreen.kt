package com.simple.medai.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simple.medai.data.SupabaseRepository
import com.simple.medai.models.SelectedBook

@Composable
fun StudySessionScreen(
    book: SelectedBook?,
    developmentMode: Boolean,
    onBack: () -> Unit
) {
    var status by remember {
        mutableStateOf(if (developmentMode) "Demo session ready" else "Creating session...")
    }

    LaunchedEffect(book, developmentMode) {
        if (!developmentMode && book != null) {
            status = try {
                if (SupabaseRepository.createStudySession(book) != null)
                    "Session registered in Supabase"
                else "Session unavailable"
            } catch (_: Exception) {
                "Could not register session"
            }
        }
    }

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            TextButton(onClick = onBack) { Text("← Back") }
            Spacer(Modifier.height(8.dp))
            Text("Study session", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(book?.name ?: "Medical book")
            Spacer(Modifier.height(6.dp))
            Text(status, fontSize = 12.sp)

            Spacer(Modifier.height(24.dp))
            StudyAction("SUMMARY", "Create an editable medical study summary.")
            StudyAction("ASK AI", "Ask follow-up questions about this book.")
            StudyAction("SLIDES", "Generate a presentation from a selected topic.")
            StudyAction("EXAM MODE", "Practice questions, receive a score and repeat the exam.")

            Spacer(Modifier.weight(1f))
            Text(
                if (developmentMode)
                    "Development mode does not write protected user data."
                else
                    "The session is registered. Temporary AI upload is the next step.",
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun StudyAction(title: String, subtitle: String) {
    OutlinedButton(
        onClick = {},
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, fontSize = 12.sp)
        }
    }
}
