package com.simple.medai.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    var status by remember { mutableStateOf("Preparando espacio de trabajo...") }
    var prompt by remember { mutableStateOf("") }
    var lastPrompt by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(book) {
        if (book != null) {
            status = try {
                if (SupabaseRepository.createStudySession(book) != null)
                    "Espacio de trabajo listo"
                else
                    "Sesión disponible"
            } catch (_: Exception) {
                "Espacio de trabajo disponible"
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
            TextButton(onClick = onBack) { Text("← Mis libros") }

            Spacer(Modifier.height(6.dp))
            SimpleHeader(
                title = "Chat con IA",
                subtitle = book?.name ?: "Libro médico"
            )

            Spacer(Modifier.height(10.dp))
            AssistChip(onClick = {}, label = { Text(status) })

            Spacer(Modifier.height(20.dp))
            SimpleSectionTitle(
                "¿Qué quieres hacer con este libro?",
                "Puedes escribir libremente o usar una de las opciones."
            )
            Spacer(Modifier.height(12.dp))

            AiAction(Icons.Default.Description, "Crear resumen editable", "Genera un resumen que luego podrás editar y guardar.")
            AiAction(Icons.Default.Psychology, "Explicarme un tema", "Pide una explicación simple o avanzada.")
            AiAction(Icons.Default.Slideshow, "Crear PowerPoint", "Prepara una presentación a partir de un tema.")
            AiAction(Icons.Default.CompareArrows, "Comparar conceptos", "Crea comparaciones claras y ordenadas.")
            AiAction(Icons.Default.Quiz, "Crear preguntas de repaso", "Genera preguntas, pero el examen formal será desde un resumen guardado.")

            Spacer(Modifier.height(18.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = SimpleCardShape
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Escribe lo que necesitas", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 110.dp),
                        placeholder = {
                            Text("Ejemplo: Explícame el capítulo 4 y haz un resumen con puntos clave.")
                        },
                        shape = SimpleButtonShape
                    )

                    Spacer(Modifier.height(10.dp))

                    SimplePrimaryButton(
                        text = "ENVIAR A LA IA",
                        enabled = prompt.isNotBlank(),
                        icon = Icons.Default.Send,
                        onClick = {
                            lastPrompt = prompt.trim()
                            prompt = ""
                        }
                    )
                }
            }

            lastPrompt?.let {
                Spacer(Modifier.height(14.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SimpleCardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Tu solicitud", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(it)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "La conexión con el modelo de IA se incorporará en el siguiente paso funcional.",
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            SimpleInfoCard(
                title = "Guardado inteligente",
                body = "El libro original es temporal. Solo los resúmenes o materiales que decidas guardar permanecen en tu cuenta y estarán sujetos a un límite de almacenamiento."
            )

            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = SimpleCardShape
            ) {
                Column(Modifier.padding(18.dp)) {
                    Icon(Icons.Default.School, contentDescription = null)
                    Spacer(Modifier.height(8.dp))
                    Text("Modo estudio y examen", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "Se habilita desde un resumen guardado, no directamente desde el libro original.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    SimpleOutlinedButton(
                        text = "SELECCIONA UN RESUMEN GUARDADO",
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
private fun AiAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    OutlinedButton(
        onClick = {},
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = SimpleButtonShape,
        contentPadding = PaddingValues(16.dp)
    ) {
        Icon(icon, contentDescription = null)
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
