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
import com.simple.medai.data.*
import kotlinx.coroutines.launch

private data class ChatMessage(
    val fromUser: Boolean,
    val text: String,
    val attachmentName: String? = null,
    val canExportPptx: Boolean = false
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
    var exportIndex by remember { mutableStateOf<Int?>(null) }
    var pendingFile by remember { mutableStateOf<GeneratedFile?>(null) }
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

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        )
    ) { uri ->
        val file = pendingFile
        if (uri != null && file != null) {
            try {
                SimpleExportRepository.save(context, uri, file)
                errorText = ""
            } catch (e: Exception) {
                errorText = e.message ?: "No se pudo guardar."
            } finally {
                pendingFile = null
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
                messages.forEachIndexed { index, message ->
                    ChatBubble(
                        message = message,
                        exporting = exportIndex == index,
                        onDownload = if (message.canExportPptx) {
                            {
                                scope.launch {
                                    exportIndex = index
                                    errorText = ""
                                    try {
                                        val file = SimpleExportRepository.createPowerPoint(
                                            content = message.text,
                                            title = titleFromContent(message.text)
                                        )
                                        pendingFile = file
                                        saveLauncher.launch(file.fileName)
                                    } catch (e: Exception) {
                                        errorText = e.message ?: "No se pudo crear el PowerPoint."
                                    } finally {
                                        exportIndex = null
                                    }
                                }
                            }
                        } else null
                    )
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
                                TextButton(onClick = { attachment = null }) { Text("QUITAR") }
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
                                val requestedAction = action

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
                                            action = requestedAction,
                                            attachment = file,
                                            onUploadProgress = { progress = it }
                                        )
                                        messages += ChatMessage(
                                            fromUser = false,
                                            text = result.answer,
                                            canExportPptx = requestedAction == "diapositivas"
                                        )
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
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    exporting: Boolean,
    onDownload: (() -> Unit)?
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = SimpleCardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (message.fromUser)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(15.dp)) {
            Text(
                if (message.fromUser) "Tú" else "SIMPLE",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            message.attachmentName?.let {
                Spacer(Modifier.height(4.dp))
                Text("📎 $it", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(5.dp))
            Text(message.text)

            if (onDownload != null) {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onDownload,
                    enabled = !exporting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = SimpleButtonShape
                ) {
                    Text(if (exporting) "CREANDO POWERPOINT…" else "⬇ DESCARGAR POWERPOINT")
                }
            }
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
            onAction(
                "diapositivas",
                "Crea una presentación profesional. Escribe cada sección como 'Diapositiva 1: Título', seguida de viñetas. Tema: "
            )
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
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
        uri.toString(),
        name,
        context.contentResolver.getType(uri) ?: "application/octet-stream",
        size
    )
}

private fun formatBytes(b: Long): String {
    if (b < 0) return "tamaño desconocido"
    val mb = b / 1024.0 / 1024.0
    return if (mb < 1) "%.1f KB".format(b / 1024.0) else "%.1f MB".format(mb)
}

private fun titleFromContent(text: String): String {
    val first = text.lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() }
        ?: "Presentacion SIMPLE"
    return first
        .replace(Regex("^(#+\\s*)?(Diapositiva|Slide)\\s*\\d+\\s*[:.-]?\\s*", RegexOption.IGNORE_CASE), "")
        .take(60)
        .ifBlank { "Presentacion SIMPLE" }
}
