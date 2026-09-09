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
fun StudySessionScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var prompt by remember { mutableStateOf("") }
    var attachment by remember { mutableStateOf<AiAttachment?>(null) }
    var sending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var selectedAction by remember { mutableStateOf("chat") }
    val messages = remember { mutableStateListOf<ChatMessage>() }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val info = readAttachmentInfo(context, uri)
                if (info.sizeBytes > SimpleAiRepository.MAX_ATTACHMENT_BYTES) {
                    errorMessage = "El archivo supera el límite temporal de 10 MB."
                    attachment = null
                } else {
                    attachment = info
                    errorMessage = ""
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "No se pudo adjuntar el archivo."
            }
        }
    }

    LaunchedEffect(messages.size, sending) {
        if (messages.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Surface(
        Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(18.dp)
        ) {
            SimpleBackButton(
                onClick = onBack,
                label = "INICIO"
            )

            Spacer(Modifier.height(14.dp))

            SimpleHeader(
                title = "SIMPLE IA",
                subtitle = "Pregunta, adjunta y trabaja en un solo lugar."
            )

            Spacer(Modifier.height(16.dp))

            QuickActions(
                onAction = { action, text ->
                    selectedAction = action
                    prompt = text
                }
            )

            if (messages.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                messages.forEach { message ->
                    ChatBubble(message)
                    Spacer(Modifier.height(9.dp))
                }

                if (sending) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = SimpleCardShape
                    ) {
                        Row(Modifier.padding(16.dp)) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("SIMPLE está trabajando...")
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = SimpleCardShape
            ) {
                Column(Modifier.padding(14.dp)) {

                    attachment?.let { file ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = SimpleButtonShape
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Text("📎", fontSize = 20.sp)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        file.name,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        formatBytes(file.sizeBytes),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                TextButton(
                                    onClick = { attachment = null }
                                ) {
                                    Text("QUITAR")
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                    }

                    OutlinedTextField(
                        value = prompt,
                        onValueChange = {
                            prompt = it
                            selectedAction = "chat"
                            errorMessage = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 110.dp),
                        enabled = !sending,
                        placeholder = {
                            Text(
                                "Escribe lo que quieras...\nEj.: Resume este archivo, resuelve esta tarea o crea 8 diapositivas."
                            )
                        },
                        shape = SimpleButtonShape
                    )

                    if (errorMessage.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Row {
                        OutlinedButton(
                            onClick = {
                                picker.launch(arrayOf("*/*"))
                            },
                            enabled = !sending,
                            modifier = Modifier.height(52.dp),
                            shape = SimpleButtonShape
                        ) {
                            Text("＋", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(5.dp))
                            Text("ADJUNTAR")
                        }

                        Spacer(Modifier.width(10.dp))

                        Button(
                            onClick = {
                                val text = prompt.trim()

                                if (text.isBlank() && attachment == null) {
                                    errorMessage = "Escribe algo o adjunta un archivo."
                                    return@Button
                                }

                                val shownText = if (text.isBlank()) {
                                    "Analiza este archivo y dime lo más importante."
                                } else text

                                val currentAttachment = attachment

                                messages.add(
                                    ChatMessage(
                                        fromUser = true,
                                        text = shownText,
                                        attachmentName = currentAttachment?.name
                                    )
                                )

                                prompt = ""
                                attachment = null
                                sending = true
                                errorMessage = ""

                                scope.launch {
                                    try {
                                        val result = SimpleAiRepository.ask(
                                            context = context,
                                            prompt = shownText,
                                            action = selectedAction,
                                            attachment = currentAttachment
                                        )

                                        messages.add(
                                            ChatMessage(
                                                fromUser = false,
                                                text = result.answer
                                            )
                                        )
                                    } catch (e: Exception) {
                                        errorMessage =
                                            e.message ?: "No se pudo consultar la IA."
                                    } finally {
                                        sending = false
                                        selectedAction = "chat"
                                    }
                                }
                            },
                            enabled = !sending && (prompt.isNotBlank() || attachment != null),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = SimpleButtonShape
                        ) {
                            Text(if (sending) "..." else "ENVIAR ➤", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(7.dp))

                    Text(
                        "Adjuntos temporales: máximo 10 MB por consulta. SIMPLE no los guarda como biblioteca.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
        }
    }
}

@Composable
private fun QuickActions(
    onAction: (String, String) -> Unit
) {
    Text(
        "Acciones rápidas",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(8.dp))

    Row(Modifier.fillMaxWidth()) {
        QuickButton(
            modifier = Modifier.weight(1f),
            label = "📄 Resumir",
            onClick = {
                onAction("resumen", "Resume el contenido adjunto de forma clara y estructurada.")
            }
        )
        Spacer(Modifier.width(8.dp))
        QuickButton(
            modifier = Modifier.weight(1f),
            label = "📊 Diapositivas",
            onClick = {
                onAction("diapositivas", "Crea una presentación por diapositivas a partir del contenido adjunto o del tema que escribiré.")
            }
        )
    }

    Spacer(Modifier.height(8.dp))

    Row(Modifier.fillMaxWidth()) {
        QuickButton(
            modifier = Modifier.weight(1f),
            label = "✍ Tarea",
            onClick = {
                onAction("tarea", "Ayúdame a resolver esta tarea de manera clara, correcta y lista para revisar.")
            }
        )
        Spacer(Modifier.width(8.dp))
        QuickButton(
            modifier = Modifier.weight(1f),
            label = "🧠 Explicar",
            onClick = {
                onAction("explicar", "Explícame este contenido de forma sencilla y didáctica.")
            }
        )
    }
}

@Composable
private fun QuickButton(
    modifier: Modifier,
    label: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = SimpleButtonShape
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = SimpleCardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (message.fromUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(Modifier.padding(15.dp)) {
            Text(
                if (message.fromUser) "Tú" else "SIMPLE",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )

            message.attachmentName?.let {
                Spacer(Modifier.height(5.dp))
                Text(
                    "📎 $it",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(6.dp))
            Text(message.text)
        }
    }
}

private fun readAttachmentInfo(
    context: Context,
    uri: Uri
): AiAttachment {
    var name = "archivo"
    var size = -1L

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

    val mime = context.contentResolver.getType(uri)
        ?: "application/octet-stream"

    return AiAttachment(
        uri = uri.toString(),
        name = name,
        mimeType = mime,
        sizeBytes = size
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 0) return "tamaño desconocido"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    return "%.1f MB".format(kb / 1024.0)
}
