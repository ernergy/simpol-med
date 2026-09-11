package com.simple.medai.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simple.medai.data.*
import kotlinx.coroutines.launch

private data class ChatMessage(
    val fromUser: Boolean,
    val text: String,
    val attachmentName: String? = null,
    val canExportPptx: Boolean = false,
    val sources: List<AiSource> = emptyList()
)

@Composable
fun StudySessionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    var prompt by remember { mutableStateOf("") }
    var attachment by remember { mutableStateOf<AiAttachment?>(null) }
    var action by remember { mutableStateOf("chat") }
    var previousResponseId by remember { mutableStateOf<String?>(null) }
    var activeVectorStoreId by remember { mutableStateOf<String?>(null) }
    var lastArtifactType by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<Int?>(null) }
    var processingStatus by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }
    var exportIndex by remember { mutableStateOf<Int?>(null) }
    var pendingFile by remember { mutableStateOf<GeneratedFile?>(null) }

    val activeFileIds = remember { mutableStateListOf<String>() }
    val messages = remember { mutableStateListOf<ChatMessage>() }

    fun closeChat() {
        val ids = activeFileIds.toList()
        val vectorId = activeVectorStoreId
        scope.launch {
            try {
                SimpleAiRepository.cleanupResources(ids, vectorId)
            } catch (_: Exception) {
            } finally {
                activeFileIds.clear()
                activeVectorStoreId = null
                previousResponseId = null
                lastArtifactType = null
                onBack()
            }
        }
    }

    BackHandler(enabled = true) {
        if (!sending) closeChat()
    }

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

    Surface(
        Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(scroll)
                .padding(18.dp)
        ) {
            SimpleBackButton(
                onClick = { if (!sending) closeChat() },
                label = "INICIO"
            )

            Spacer(Modifier.height(14.dp))

            SimpleHeader(
                "SIMPLE IA",
                "Pregunta • adjunta • amplía respuestas • revisa fuentes"
            )

            Spacer(Modifier.height(14.dp))

            QuickActions { selected, starter ->
                action = selected
                prompt = starter
                if (selected == "diapositivas") lastArtifactType = "pptx"
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
                Card(
                    Modifier.fillMaxWidth(),
                    shape = SimpleCardShape
                ) {
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
                                    Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(processingStatus.ifBlank { "SIMPLE está trabajando…" })
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(
                Modifier.fillMaxWidth(),
                shape = SimpleCardShape
            ) {
                Column(Modifier.padding(14.dp)) {
                    attachment?.let { file ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = SimpleButtonShape
                        ) {
                            Row(
                                Modifier
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
                                    Text(formatBytes(file.sizeBytes), fontSize = 11.sp)
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
                        onValueChange = {
                            prompt = it
                            errorText = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 115.dp),
                        enabled = !sending,
                        placeholder = { Text("Escribe lo que necesites…") },
                        shape = SimpleButtonShape
                    )

                    if (errorText.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            errorText,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
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

                                val request = if (typed.isBlank()) {
                                    "Analiza este archivo y continúa nuestra conversación sobre él."
                                } else typed

                                val previousText = messages.lastOrNull { !it.fromUser }?.text
                                val detectedArtifact = detectArtifactRequest(
                                    request,
                                    lastArtifactType,
                                    previousText
                                )

                                val requestedAction =
                                    if (detectedArtifact == "pptx") "diapositivas" else action

                                if (detectedArtifact != null) {
                                    lastArtifactType = detectedArtifact
                                }

                                messages += ChatMessage(
                                    fromUser = true,
                                    text = request,
                                    attachmentName = file?.name
                                )

                                prompt = ""
                                attachment = null
                                sending = true
                                processingStatus = ""
                                progress = if (file != null) 0 else null
                                errorText = ""

                                scope.launch {
                                    try {
                                        val result = SimpleAiRepository.ask(
                                            context = context,
                                            prompt = request,
                                            action = requestedAction,
                                            attachment = file,
                                            previousResponseId = previousResponseId,
                                            requestedArtifact = detectedArtifact,
                                            currentVectorStoreId = activeVectorStoreId,
                                            onUploadProgress = { progress = it },
                                            onProcessingStatus = { processingStatus = it }
                                        )

                                        previousResponseId =
                                            result.responseId ?: previousResponseId

                                        result.retainedFileId?.let { id ->
                                            if (!activeFileIds.contains(id)) {
                                                activeFileIds.add(id)
                                            }
                                        }

                                        result.vectorStoreId?.let {
                                            activeVectorStoreId = it
                                        }

                                        val finalArtifact =
                                            result.artifactType ?: detectedArtifact

                                        if (finalArtifact != null) {
                                            lastArtifactType = finalArtifact
                                        }

                                        messages += ChatMessage(
                                            fromUser = false,
                                            text = result.answer,
                                            canExportPptx =
                                                finalArtifact == "pptx" ||
                                                    requestedAction == "diapositivas",
                                            sources = result.sources
                                        )
                                    } catch (e: Exception) {
                                        errorText = e.message ?: "No se pudo procesar."
                                    } finally {
                                        sending = false
                                        progress = null
                                        processingStatus = ""
                                        action = "chat"
                                    }
                                }
                            },
                            enabled = !sending &&
                                (prompt.isNotBlank() || attachment != null),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = SimpleButtonShape
                        ) {
                            Text("ENVIAR ➤", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(7.dp))
                    Text(
                        "Las fuentes reales consultadas aparecen debajo de cada respuesta.",
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
private fun ChatBubble(
    message: ChatMessage,
    exporting: Boolean,
    onDownload: (() -> Unit)?
) {
    val uriHandler = LocalUriHandler.current
    var zoom by remember(message.text) { mutableFloatStateOf(1f) }

    Card(
        Modifier.fillMaxWidth(),
        shape = SimpleCardShape,
        colors = CardDefaults.cardColors(
            containerColor =
                if (message.fromUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
        )
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (message.fromUser) "Tú" else "SIMPLE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                if (!message.fromUser) {
                    Row {
                        TextButton(
                            onClick = { zoom = (zoom - 0.1f).coerceAtLeast(0.8f) },
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) { Text("A−") }

                        TextButton(
                            onClick = { zoom = 1f },
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) { Text("${(zoom * 100).toInt()}%") }

                        TextButton(
                            onClick = { zoom = (zoom + 0.1f).coerceAtMost(2.2f) },
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) { Text("A+") }
                    }
                }
            }

            message.attachmentName?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    "📎 $it",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(5.dp))

            if (message.fromUser) {
                Text(message.text)
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(message.text) {
                            detectTransformGestures { _, _, gestureZoom, _ ->
                                zoom = (zoom * gestureZoom).coerceIn(0.8f, 2.2f)
                            }
                        },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
                    shape = SimpleButtonShape
                ) {
                    Text(
                        text = message.text,
                        fontSize = (16f * zoom).sp,
                        lineHeight = (23f * zoom).sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Text(
                    "Pellizca con dos dedos para ampliar o usa A− / A+.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }

            if (message.sources.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))
                Text(
                    "FUENTES CONSULTADAS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                message.sources.forEachIndexed { index, source ->
                    Spacer(Modifier.height(4.dp))
                    if (!source.url.isNullOrBlank()) {
                        TextButton(
                            onClick = {
                                try {
                                    uriHandler.openUri(source.url)
                                } catch (_: Exception) {
                                }
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("${index + 1}. ${source.title} ↗", fontSize = 12.sp)
                        }
                    } else {
                        Text("${index + 1}. ${source.title}", fontSize = 12.sp)
                    }
                }
            }

            if (onDownload != null) {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onDownload,
                    enabled = !exporting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = SimpleButtonShape
                ) {
                    Text(
                        if (exporting) "CREANDO POWERPOINT…"
                        else "⬇ DESCARGAR POWERPOINT"
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActions(onAction: (String, String) -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        QuickButton(Modifier.weight(1f), "📄 Resumir") {
            onAction("resumen", "Resume de forma profesional: ")
        }
        Spacer(Modifier.width(7.dp))
        QuickButton(Modifier.weight(1f), "📊 Diapositivas") {
            onAction("diapositivas", "Crea una presentación profesional sobre: ")
        }
    }

    Spacer(Modifier.height(7.dp))

    Row(Modifier.fillMaxWidth()) {
        QuickButton(Modifier.weight(1f), "✍ Tarea") {
            onAction("tarea", "Ayúdame con esta tarea: ")
        }
        Spacer(Modifier.width(7.dp))
        QuickButton(Modifier.weight(1f), "🧠 Explicar") {
            onAction("explicar", "Explícame de forma sencilla: ")
        }
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
        modifier = modifier.height(46.dp),
        shape = SimpleButtonShape,
        contentPadding = PaddingValues(horizontal = 6.dp)
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

private fun detectArtifactRequest(
    text: String,
    lastArtifactType: String?,
    lastAssistantText: String?
): String? {
    val value = text
        .lowercase()
        .replace("á", "a")
        .replace("é", "e")
        .replace("í", "i")
        .replace("ó", "o")
        .replace("ú", "u")

    val asksPpt =
        value.contains("ppt") ||
            value.contains("pptx") ||
            value.contains("powerpoint") ||
            value.contains("power point") ||
            value.contains("diapositivas") ||
            value.contains("presentacion")

    if (asksPpt) return "pptx"

    val generic =
        value.contains("dame el archivo") ||
            value.contains("pasame el archivo") ||
            value.contains("quiero el archivo") ||
            value.contains("descargar")

    if (generic) {
        if (!lastArtifactType.isNullOrBlank()) return lastArtifactType
        val previous = lastAssistantText?.lowercase().orEmpty()
        if (
            previous.contains("diapositiva") ||
            previous.contains("presentación") ||
            previous.contains("presentacion")
        ) return "pptx"
    }

    return null
}

private fun readAttachment(context: Context, uri: Uri): AiAttachment {
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
            if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                size = cursor.getLong(sizeIndex)
            }
        }
    }

    return AiAttachment(
        uri = uri.toString(),
        name = name,
        mimeType = context.contentResolver.getType(uri)
            ?: "application/octet-stream",
        sizeBytes = size
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 0) return "tamaño desconocido"
    val mb = bytes / 1024.0 / 1024.0
    return if (mb < 1) {
        "%.1f KB".format(bytes / 1024.0)
    } else {
        "%.1f MB".format(mb)
    }
}

private fun titleFromContent(text: String): String {
    return text
        .lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() }
        ?.take(60)
        ?.ifBlank { "Presentacion SIMPLE" }
        ?: "Presentacion SIMPLE"
}
