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
import com.simple.medai.data.SimpleAiRepository
import com.simple.medai.data.SupabaseRepository
import com.simple.medai.models.SelectedBook
import kotlinx.coroutines.launch

private data class ChatMessage(val fromUser: Boolean, val text: String)

@Composable
fun StudySessionScreen(book: SelectedBook?, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var status by remember { mutableStateOf("Preparando espacio de trabajo...") }
    var prompt by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ChatMessage>() }

    LaunchedEffect(book) {
        status = try {
            if (book != null) SupabaseRepository.createStudySession(book)
            "IA conectada"
        } catch (_: Exception) {
            "IA conectada"
        }
    }

    LaunchedEffect(messages.size, sending) {
        if (messages.isNotEmpty()) scrollState.animateScrollTo(scrollState.maxValue)
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(22.dp)
        ) {
            SimpleBackButton(onClick = onBack, label = "MIS LIBROS")
            Spacer(Modifier.height(16.dp))

            SimpleHeader(
                title = "Chat con IA",
                subtitle = book?.name ?: "Sin libro seleccionado"
            )

            Spacer(Modifier.height(10.dp))
            AssistChip(onClick = {}, label = { Text(status) })

            Spacer(Modifier.height(18.dp))
            SimpleSectionTitle(
                "¿Qué quieres hacer?",
                "Escribe libremente o usa una opción para empezar."
            )
            Spacer(Modifier.height(12.dp))

            AiAction("📄", "Crear resumen editable", "Genera una estructura para un resumen.") {
                prompt = "Crea un resumen editable, claro y estructurado sobre: "
            }
            AiAction("🧠", "Explicarme un tema", "Pide una explicación simple o avanzada.") {
                prompt = "Explícame de forma clara y didáctica: "
            }
            AiAction("📊", "Preparar PowerPoint", "Genera el contenido base para una presentación.") {
                prompt = "Prepara el contenido de una presentación PowerPoint sobre: "
            }
            AiAction("↔", "Comparar conceptos", "Pide una comparación médica ordenada.") {
                prompt = "Compara de manera clara y ordenada: "
            }

            if (messages.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                SimpleSectionTitle("Conversación")
                Spacer(Modifier.height(10.dp))

                messages.forEach {
                    ChatBubble(it)
                    Spacer(Modifier.height(9.dp))
                }

                if (sending) {
                    Card(Modifier.fillMaxWidth(), shape = SimpleCardShape) {
                        Row(Modifier.padding(16.dp)) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("SIMPLE está pensando...")
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Card(Modifier.fillMaxWidth(), shape = SimpleCardShape) {
                Column(Modifier.padding(16.dp)) {
                    Text("Escribe lo que necesitas", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = prompt,
                        onValueChange = {
                            prompt = it
                            errorMessage = ""
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                        enabled = !sending,
                        placeholder = {
                            Text("Ejemplo: Explícame la insuficiencia cardíaca y dame puntos clave.")
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

                    SimplePrimaryButton(
                        text = if (sending) "CONSULTANDO..." else "ENVIAR A LA IA",
                        enabled = prompt.isNotBlank() && !sending,
                        icon = "➤",
                        onClick = {
                            val text = prompt.trim()
                            if (text.isBlank()) return@SimplePrimaryButton

                            messages.add(ChatMessage(true, text))
                            prompt = ""
                            errorMessage = ""
                            sending = true

                            scope.launch {
                                try {
                                    val result = SimpleAiRepository.ask(text, book?.name)
                                    messages.add(ChatMessage(false, result.answer))
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "No se pudo consultar la IA."
                                } finally {
                                    sending = false
                                }
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            SimpleInfoCard(
                title = "Estado actual",
                body = "La IA ya responde de verdad. En esta primera conexión recibe tu pregunta y el nombre del libro. El contenido completo del PDF se conectará en el siguiente paso."
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = SimpleCardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (message.fromUser)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (message.fromUser) "Tú" else "SIMPLE",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(5.dp))
            Text(message.text)
        }
    }
}

@Composable
private fun AiAction(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        shape = SimpleButtonShape,
        contentPadding = PaddingValues(16.dp)
    ) {
        Text(icon, fontSize = 22.sp)
        Spacer(Modifier.width(10.dp))
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
