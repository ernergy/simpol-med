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

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(22.dp)
        ) {
            SimpleHeader(
                title = "SIMPLE",
                subtitle = email
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
                    Text(
                        "MY CREDITS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when {
                            loading -> "..."
                            credits != null -> credits.toString()
                            else -> "—"
                        },
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "available study credits",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(14.dp))
                    SimplePrimaryButton("BUY CREDITS") { showBuyCredits = true }
                }
            }

            Spacer(Modifier.height(22.dp))
            SimpleSectionTitle(
                "Start studying",
                "Books are temporary. Your saved summaries are your study library."
            )
            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = SimpleCardShape
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Medical book / PDF", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "Select a medical book, talk with SIMPLE AI and create useful study material.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    SimplePrimaryButton("SELECT MEDICAL BOOK") { onUploadBook() }
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(Modifier.fillMaxWidth()) {
                DashboardMiniCard(
                    modifier = Modifier.weight(1f),
                    title = "My summaries",
                    value = "Saved",
                    subtitle = "Editable materials"
                )
                Spacer(Modifier.width(12.dp))
                DashboardMiniCard(
                    modifier = Modifier.weight(1f),
                    title = "Study mode",
                    value = "Ready",
                    subtitle = "From saved summaries"
                )
            }

            Spacer(Modifier.height(22.dp))
            SimpleSectionTitle("My activity")
            Spacer(Modifier.height(10.dp))

            SimpleInfoCard(
                title = "Your recent work will appear here",
                body = "Summaries, study sessions, exams and generated presentations will be organized inside your account."
            )

            Spacer(Modifier.height(26.dp))

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

            Spacer(Modifier.height(12.dp))
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
                        "Payments will be connected after the final AI cost per study action is measured.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showBuyCredits = false }) { Text("CLOSE") }
            }
        )
    }
}

@Composable
private fun DashboardMiniCard(
    modifier: Modifier,
    title: String,
    value: String,
    subtitle: String
) {
    Card(modifier = modifier, shape = SimpleCardShape) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
