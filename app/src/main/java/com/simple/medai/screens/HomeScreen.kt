package com.simple.medai.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simple.medai.SupabaseManager
import com.simple.medai.data.SupabaseRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onStartChat: () -> Unit,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val email = remember {
        SupabaseManager.client.auth.currentUserOrNull()?.email ?: "Mi cuenta"
    }
    var credits by remember { mutableStateOf<Int?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showBuyCredits by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            credits = SupabaseRepository.loadProfile()?.credits_balance
        } catch (_: Exception) {
            credits = null
        } finally {
            loading = false
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = SimpleCardShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text("💡", fontSize = 28.sp, modifier = Modifier.padding(12.dp))
                }
                Spacer(Modifier.width(12.dp))
                SimpleHeader(
                    title = "SIMPLE",
                    subtitle = email
                )
            }

            Spacer(Modifier.height(22.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = SimpleCardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🪙", fontSize = 38.sp)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("MIS CRÉDITOS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            when {
                                loading -> "..."
                                credits != null -> credits.toString()
                                else -> "—"
                            },
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text("créditos disponibles")
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            SimplePrimaryButton(
                text = "COMPRAR CRÉDITOS",
                onClick = { showBuyCredits = true },
                icon = "🛒"
            )

            Spacer(Modifier.height(22.dp))

            SimpleSectionTitle(
                "¿Qué quieres hacer?",
                "Ahora todo empieza desde un solo chat."
            )

            Spacer(Modifier.height(12.dp))

            Card(
                onClick = onStartChat,
                modifier = Modifier.fillMaxWidth(),
                shape = SimpleCardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = SimpleButtonShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text("＋", fontSize = 34.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Nuevo chat",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "Escribe lo que quieras o adjunta un archivo.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                        Text("›", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        "PDF • Word • imágenes • apuntes • tareas • presentaciones",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            FeatureTile(
                icon = "📄",
                title = "Mis resúmenes",
                subtitle = "Materiales que decidas guardar para estudiar.",
                onClick = null
            )

            Spacer(Modifier.height(10.dp))

            FeatureTile(
                icon = "🎓",
                title = "Modo estudio",
                subtitle = "Se habilitará desde un resumen guardado.",
                onClick = null
            )

            Spacer(Modifier.height(22.dp))

            SimpleInfoCard(
                title = "Más simple",
                body = "Ya no necesitas entrar por “Mis libros” y después por “Chat con IA”. Abres un chat, adjuntas algo si quieres y preguntas directamente."
            )

            Spacer(Modifier.height(24.dp))

            TextButton(
                onClick = {
                    scope.launch {
                        SupabaseRepository.signOut()
                        onLogout()
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("CERRAR SESIÓN")
            }
        }
    }

    if (showBuyCredits) {
        AlertDialog(
            onDismissRequest = { showBuyCredits = false },
            title = { Text("Comprar créditos") },
            text = { Text("Los paquetes se habilitarán cuando terminemos de medir el costo real de las funciones.") },
            confirmButton = {
                TextButton(onClick = { showBuyCredits = false }) { Text("CERRAR") }
            }
        )
    }
}
