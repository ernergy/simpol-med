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
            book = readBookInfo(context, uri)
            message = ""
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
                Text("← Volver")
            }

            Spacer(Modifier.height(8.dp))

            SimpleHeader(
                title = "Mis libros",
                subtitle = "Selecciona un libro médico para trabajar temporalmente con IA."
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
                    Text("📚", fontSize = 38.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("Libro temporal", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "El PDF original se usa solo durante la sesión. No queda guardado permanentemente.",
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    SimplePrimaryButton(
                        text = if (book == null) "SELECCIONAR PDF" else "CAMBIAR PDF",
                        onClick = { picker.launch(arrayOf("application/pdf")) },
                        icon = "⬆️"
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
                        Text(it.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(5.dp))
                        Text(it.sizeLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(10.dp))
                        Text("Libro listo para abrir el chat con IA.")
                    }
                }

                Spacer(Modifier.height(18.dp))

                SimplePrimaryButton(
                    text = "ABRIR CHAT CON IA",
                    onClick = { onContinue(it) },
                    icon = "💬"
                )
            }

            if (message.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Text(message)
            }
        }
    }
}

private fun readBookInfo(context: Context, uri: Uri): SelectedBook {
    var name = "Libro médico.pdf"
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
