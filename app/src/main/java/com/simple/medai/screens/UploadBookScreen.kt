package com.simple.medai.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simple.medai.models.SelectedBook

@Composable
fun UploadBookScreen(
    onBack: () -> Unit,
    onContinue: (SelectedBook) -> Unit
) {
    val context = LocalContext.current
    var book by remember { mutableStateOf<SelectedBook?>(null) }
    var message by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val info = readBookInfo(context, uri)
            if (info != null) {
                book = info
                message = ""
            } else {
                message = "Unable to read this file."
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
            TextButton(onClick = onBack) {
                Text("← Back")
            }

            Spacer(Modifier.height(8.dp))

            SimpleHeader(
                title = "Library",
                subtitle = "Select a medical book for a temporary AI study session."
            )

            Spacer(Modifier.height(22.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = SimpleCardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "Temporary book",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        "The original PDF is used only while SIMPLE works with it. It is not kept as a permanent library file.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(Modifier.height(14.dp))

                    SimplePrimaryButton(
                        text = if (book == null) "SELECT PDF" else "CHANGE PDF",
                        onClick = {
                            picker.launch(arrayOf("application/pdf"))
                        }
                    )
                }
            }

            book?.let {
                Spacer(Modifier.height(18.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SimpleCardShape
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(
                            it.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Spacer(Modifier.height(5.dp))

                        Text(
                            it.sizeLabel,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(10.dp))

                        Text(
                            "Ready to open the AI workspace.",
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                SimplePrimaryButton(
                    text = "OPEN AI WORKSPACE",
                    onClick = {
                        onContinue(it)
                    }
                )
            }

            if (message.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(22.dp))

            SimpleInfoCard(
                title = "What can you create?",
                body = "Ask questions, request an editable summary, explain a topic, compare concepts, create a PowerPoint or prepare another study resource."
            )
        }
    }
}

private fun readBookInfo(context: Context, uri: Uri): SelectedBook? {
    var name = "Medical book.pdf"
    var size = 0L

    context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

            if (nameIndex >= 0) {
                name = cursor.getString(nameIndex) ?: name
            }

            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                size = cursor.getLong(sizeIndex)
            }
        }
    }

    return SelectedBook(
        uri = uri.toString(),
        name = name,
        sizeBytes = size
    )
}
