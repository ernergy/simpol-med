package com.simple.medai.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    onUploadBook: () -> Unit,
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
                .verticalScroll(rememberScrollState())
                .padding(22.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = SimpleCardShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = "Logo SIMPLE",
                        modifier = Modifier.padding(12.dp).size(28.dp)
                    )
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
                    Icon(
                        Icons.Default.MonetizationOn,
                        contentDescription = null,
                        modifier = Modifier.size(42.dp)
                    )
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
                icon = Icons.Default.ShoppingCart
            )

            Spacer(Modifier.height(22.dp))
            SimpleSectionTitle(
                "Herramientas de estudio",
                "Elige lo que quieres hacer."
            )
            Spacer(Modifier.height(12.dp))

            FeatureTile(
                icon = Icons.Default.MenuBook,
                title = "Mis libros",
                subtitle = "Selecciona un PDF temporal para trabajar con IA.",
                onClick = onUploadBook
            )

            Spacer(Modifier.height(10.dp))

            FeatureTile(
                icon = Icons.Default.Description,
                title = "Mis resúmenes",
                subtitle = "Materiales guardados y editables.",
                onClick = null
            )

            Spacer(Modifier.height(10.dp))

            FeatureTile(
                icon = Icons.Default.School,
                title = "Modo estudio",
                subtitle = "Repaso y examen desde un resumen guardado.",
                onClick = null
            )

            Spacer(Modifier.height(10.dp))

            FeatureTile(
                icon = Icons.Default.Chat,
                title = "Chat con IA",
                subtitle = "Disponible después de seleccionar un libro.",
                onClick = onUploadBook
            )

            Spacer(Modifier.height(22.dp))
            SimpleSectionTitle("Actividad reciente")
            Spacer(Modifier.height(10.dp))

            SimpleInfoCard(
                title = "Tu actividad aparecerá aquí",
                body = "Resúmenes, sesiones de estudio, exámenes y presentaciones se organizarán dentro de tu cuenta."
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
            text = {
                Column {
                    Text("Elige un paquete.")
                    Spacer(Modifier.height(12.dp))
                    CreditPackage("5 créditos")
                    CreditPackage("10 créditos")
                    CreditPackage("25 créditos")
                }
            },
            confirmButton = {
                TextButton(onClick = { showBuyCredits = false }) { Text("CERRAR") }
            }
        )
    }
}

@Composable
private fun CreditPackage(label: String) {
    OutlinedButton(
        onClick = {},
        enabled = false,
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        shape = SimpleButtonShape
    ) { Text(label) }
}
