package com.simple.medai.screens

import androidx.compose.foundation.layout.*
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
        SupabaseManager.client.auth.currentUserOrNull()?.email ?: "My account"
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

    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text("SIMPLE", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text("My study account", fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(email, fontSize = 13.sp)

            Spacer(Modifier.height(24.dp))

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("MY CREDITS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when {
                            loading -> "..."
                            credits != null -> credits.toString()
                            else -> "—"
                        },
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("available study credits")
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { showBuyCredits = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("BUY CREDITS")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Start studying", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))

            Button(
                onClick = onUploadBook,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("UPLOAD MEDICAL BOOK")
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("EXAM MODE — SELECT A BOOK FIRST")
            }

            Spacer(Modifier.height(22.dp))
            Text("My activity", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Your summaries, exams and study sessions will appear here.", fontSize = 14.sp)

            Spacer(Modifier.weight(1f))

            TextButton(
                onClick = {
                    scope.launch {
                        SupabaseRepository.signOut()
                        onLogout()
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("SIGN OUT")
            }
        }
    }

    if (showBuyCredits) {
        AlertDialog(
            onDismissRequest = { showBuyCredits = false },
            title = { Text("Buy credits") },
            text = {
                Column {
                    Text("Choose a credit package.")
                    Spacer(Modifier.height(12.dp))
                    CreditPackage("5 credits")
                    CreditPackage("10 credits")
                    CreditPackage("25 credits")
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Payment will be connected after the AI cost per study session is finalized.",
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showBuyCredits = false }) {
                    Text("CLOSE")
                }
            }
        )
    }
}

@Composable
private fun CreditPackage(label: String) {
    OutlinedButton(
        onClick = {},
        enabled = false,
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
    ) {
        Text(label)
    }
}
