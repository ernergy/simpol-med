package com.simple.medai.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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

    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp)
        ) {
            TextButton(onClick = onBack) { Text("← Back") }

            Spacer(Modifier.height(12.dp))
            Text("Upload medical book", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("The original book will be used temporarily for your study session.")

            Spacer(Modifier.height(28.dp))

            OutlinedButton(
                onClick = { picker.launch(arrayOf("application/pdf")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (book == null) "SELECT PDF" else "CHANGE PDF")
            }

            book?.let {
                Spacer(Modifier.height(20.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text(it.name, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(it.sizeLabel)
                        Spacer(Modifier.height(8.dp))
                        Text("PDF selected and ready for a study session.")
                    }
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = { onContinue(it) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CONTINUE")
                }
            }

            if (message.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(message)
            }

            Spacer(Modifier.weight(1f))
            Text(
                "For now the app selects the PDF locally. AI upload and temporary processing will be connected in the next stage.",
                fontSize = 12.sp
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
            if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
        }
    }

    return SelectedBook(
        uri = uri.toString(),
        name = name,
        sizeBytes = size
    )
}
