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
import com.simple.medai.data.AiAttachment
import com.simple.medai.data.SimpleAiRepository
import kotlinx.coroutines.launch

private data class ChatMessage(
    val fromUser: Boolean,
    val text: String,
    val attachmentName: String? = null
)

@Composable
fun StudySessionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    var prompt by remember { mutableStateOf("") }
    var attachment by remember { mutableStateOf<AiAttachment?>(null) }
    var action by remember { mutableStateOf("chat") }
    var sending by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<Int?>(null) }
    var errorText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ChatMessage>() }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                attachment = readAttachment(context, uri)
                errorText = ""
            } catch (e: Exception) {
                errorText = e.message ?: "No se pudo adjuntar."
            }
        }
    }

    LaunchedEffect(messages.size, sending) {
        if (messages.isNotEmpty()) scroll.animateScrollTo(scroll.maxValue)
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(scroll)
                .padding(18.dp)
        ) {
            SimpleBackButton(onClick = onBack, label = "INICIO")
            Spacer(Modifier.height(14.dp))
            SimpleHeader("SIMPLE IA", "Escribe o adjunta desde tu celular.")

            Spacer(Modifier.height(14.dp))
            QuickActions { a, starter ->
                action = a
                prompt = starter
            }

            if (messages.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                messages.forEach {
                    ChatBubble(it)
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (sending) {
                Spacer(Modifier.height(10.dp))
                Card(Modifier.fillMaxWidth(), shape = SimpleCardShape) {
                    Column(Modifier.padding(15.dp)) {
                        if (progress != null && progress!! < 100) {
                            Text("Subiendo archivo… ${progress}%")
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progress!! / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Row {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("SIMPLE está trabajando…")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Card(Modifier.fillMaxWidth(), shape = SimpleCardShape) {
                Column(Modifier.padding(14.dp)) {
                    attachment?.let { f ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = SimpleButtonShape
                        ) {
                            Row(Modifier.fillMaxWidth().padding(12.dp)) {
                                Text("📎", fontSize = 20.sp)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(f.name, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text(formatBytes(f.sizeBytes), fontSize = 11.sp)
                                }
                                TextButton(onClick = { attachment = null }) {
                                    Text("QUITAR")
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it; errorText = "" },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 115.dp),
                        enabled = !sending,
                        placeholder = { Text("Escribe lo que quieras…") },
                        shape = SimpleButtonShape
                    )

                    if (errorText.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(errorText, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    Spacer(Modifier.height(10.dp))
                    Row {
                        OutlinedButton(
                            onClick = { picker.launch(arrayOf("*/*")) },
                            enabled = !sending,
                            modifier = Modifier.height(52.dp),
                            shape = SimpleButtonShape
                        ) {
                            Text("＋", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(5.dp))
                            Text("ADJUNTAR")
                        }

                        Spacer(Modifier.width(10.dp))

                        Button(
                            onClick = {
                                val typed = prompt.trim()
                                val file = attachment
                                if (typed.isBlank() && file == null) return@Button
                                val request = if (typed.isBlank())
                                    "Analiza este archivo y dime lo más importante."
                                else typed

                                messages += ChatMessage(true, request, file?.name)
                                prompt = ""
                                attachment = null
                                sending = true
                                progress = if (file != null) 0 else null
                                errorText = ""

                                scope.launch {
                                    try {
                                        val result = SimpleAiRepository.ask(
                                            context = context,
                                            prompt = request,
                                            action = action,
                                            attachment = file,
                                            onUploadProgress = { progress = it }
                                        )
                                        messages += ChatMessage(false, result.answer)
                                    } catch (e: Exception) {
                                        errorText = e.message ?: "No se pudo procesar."
                                    } finally {
                                        sending = false
                                        progress = null
                                        action = "chat"
                                    }
                                }
                            },
                            enabled = !sending && (prompt.isNotBlank() || attachment != null),
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = SimpleButtonShape
                        ) {
                            Text("ENVIAR ➤", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(7.dp))
                    Text(
                        "Los archivos se leen desde tu celular, se procesan temporalmente y no se guardan como biblioteca.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuickActions(onAction: (String, String) -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        QuickButton(Modifier.weight(1f), "📄 Resumir") {
            onAction("resumen", "Resume el archivo o tema de forma clara y estructurada.")
        }
        Spacer(Modifier.width(7.dp))
        QuickButton(Modifier.weight(1f), "📊 Diapositivas") {
            onAction("diapositivas", "Prepara una presentación profesional sobre: ")
        }
    }
    Spacer(Modifier.height(7.dp))
    Row(Modifier.fillMaxWidth()) {
        QuickButton(Modifier.weight(1f), "✍ Tarea") {
            onAction("tarea", "Ayúdame a resolver esta tarea: ")
        }
        Spacer(Modifier.width(7.dp))
        QuickButton(Modifier.weight(1f), "🧠 Explicar") {
            onAction("explicar", "Explícame de forma sencilla: ")
        }
    }
}

@Composable
private fun QuickButton(modifier: Modifier, label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        shape = SimpleButtonShape,
        contentPadding = PaddingValues(horizontal = 6.dp)
    ) { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun ChatBubble(m: ChatMessage) {
    Card(
        Modifier.fillMaxWidth(),
        shape = SimpleCardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (m.fromUser)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(15.dp)) {
            Text(if (m.fromUser) "Tú" else "SIMPLE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            m.attachmentName?.let {
                Spacer(Modifier.height(4.dp))
                Text("📎 $it", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(5.dp))
            Text(m.text)
        }
    }
}

private fun readAttachment(context: Context, uri: Uri): AiAttachment {
    var name = "archivo"
    var size = -1L
    context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
        null, null, null
    )?.use { c ->
        if (c.moveToFirst()) {
            val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val si = c.getColumnIndex(OpenableColumns.SIZE)
            if (ni >= 0) name = c.getString(ni) ?: name
            if (si >= 0 && !c.isNull(si)) size = c.getLong(si)
        }
    }
    return AiAttachment(
        uri = uri.toString(),
        name = name,
        mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream",
        sizeBytes = size
    )
}

private fun formatBytes(b: Long): String {
    if (b < 0) return "tamaño desconocido"
    val mb = b / 1024.0 / 1024.0
    return if (mb < 1) "%.1f KB".format(b / 1024.0) else "%.1f MB".format(mb)
}
