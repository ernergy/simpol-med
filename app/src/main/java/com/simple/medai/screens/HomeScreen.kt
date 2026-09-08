package com.simple.medai.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simple.medai.data.SupabaseRepository
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    developmentMode: Boolean,
    onUploadBook: () -> Unit,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var credits by remember { mutableStateOf<Int?>(if (developmentMode) 1 else null) }
    var loading by remember { mutableStateOf(!developmentMode) }

    LaunchedEffect(developmentMode) {
        if (!developmentMode) {
            try { credits = SupabaseRepository.loadProfile()?.credits_balance }
            catch (_: Exception) { credits = null }
            finally { loading = false }
        }
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            Text("SIMPLE", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text("Your medical study assistant")

            Spacer(Modifier.height(32.dp))

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Study credits", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        when {
                            loading -> "Loading..."
                            credits != null -> "$credits available"
                            else -> "Unable to load"
                        },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = onUploadBook,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("UPLOAD MEDICAL BOOK")
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("EXAM MODE — SELECT A BOOK FIRST")
            }

            Spacer(Modifier.weight(1f))

            TextButton(
                onClick = {
                    scope.launch {
                        if (!developmentMode) SupabaseRepository.signOut()
                        onLogout()
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Sign out")
            }
        }
    }
}
