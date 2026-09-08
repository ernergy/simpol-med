package com.simple.medai.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    onBack: () -> Unit
) {
    var status by remember { mutableStateOf("Preparing workspace...") }
    var chatText by remember { mutableStateOf("") }

    LaunchedEffect(book) {
        if (book != null) {
            status = try {
                if (SupabaseRepository.createStudySession(book) != null)
                    "AI workspace ready"
                else
                    "Session unavailable"
            } catch (_: Exception) {
                "Workspace available locally"
            }
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(22.dp)
        ) {
            TextButton(onClick = onBack) { Text("← Library") }

            Spacer(Modifier.height(6.dp))
            SimpleHeader(
                title = "Chat with your book",
                subtitle = book?.name ?: "Medical book"
            )

            Spacer(Modifier.height(10.dp))
            AssistChip(
                onClick = {},
                label = { Text(status) }
            )

            Spacer(Modifier.height(20.dp))

            SimpleSectionTitle(
                "What would you like to do?",
                "SIMPLE AI will work only with the selected book during this temporary session."
            )
            Spacer(Modifier.height(12.dp))

            AiAction("CREATE EDITABLE SUMMARY", "Generate a structured summary you can edit and save.")
            AiAction("EXPLAIN A TOPIC", "Ask SIMPLE to teach a chapter or concept step by step.")
            AiAction("CREATE POWERPOINT", "Prepare a presentation from a topic or summary.")
            AiAction("COMPARE CONCEPTS", "Build a clear comparison table or explanation.")
            AiAction("KEY POINTS / FLASH NOTES", "Extract high-value facts for quick review.")
            AiAction("OTHER REQUEST", "Use the chat below for any study instruction.")

            Spacer(Modifier.height(18.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = SimpleCardShape
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Ask SIMPLE AI", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = chatText,
                        onValueChange = { chatText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        placeholder = {
                            Text("Example: Summarize chapter 4 focusing on diagnosis and treatment.")
                        },
                        shape = SimpleButtonShape
                    )
                    Spacer(Modifier.height(10.dp))
                    SimplePrimaryButton("SEND TO AI", onClick = {}, enabled = false)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "AI connection is the next functional step. The complete workspace design is ready.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            SimpleInfoCard(
                title = "Saved study materials",
                body = "Only the summaries or study materials you choose to save remain in your account, subject to your storage limit. The original book stays temporary."
            )

            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = SimpleCardShape
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("STUDY / EXAM MODE", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "Study Mode and Exam Mode become available from a saved summary or study material — not directly from the raw book.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    SimpleOutlinedButton(
                        text = "SELECT A SAVED SUMMARY FIRST",
                        onClick = {},
                        enabled = false
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AiAction(title: String, subtitle: String) {
    OutlinedButton(
        onClick = {},
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = SimpleButtonShape,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(
                subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
