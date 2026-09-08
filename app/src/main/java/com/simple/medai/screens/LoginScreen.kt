package com.simple.medai.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simple.medai.SupabaseManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch

private const val EMAIL_CONFIRM_REDIRECT = "simple://auth-confirm"

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onDevelopmentAccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var waitingForConfirmation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("SIMPLE", fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Medical Study AI", fontSize = 20.sp)
            Text("Learn medicine. Simply.", fontSize = 14.sp)
            Spacer(Modifier.height(34.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            Button(
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        message = "Enter your email and password."
                        return@Button
                    }
                    scope.launch {
                        loading = true
                        message = ""
                        try {
                            SupabaseManager.client.auth.signInWith(Email) {
                                this.email = email.trim()
                                this.password = password
                            }
                            onLoginSuccess()
                        } catch (e: Exception) {
                            val raw = e.message.orEmpty()
                            message = when {
                                raw.contains("email not confirmed", true) ->
                                    "Confirm your email first, then sign in."
                                raw.contains("invalid_credentials", true) ->
                                    "Incorrect email or password."
                                else -> "Unable to sign in. Please try again."
                            }
                        } finally {
                            loading = false
                        }
                    }
                }
            ) {
                Text(if (loading) "PLEASE WAIT..." else "SIGN IN")
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (email.isBlank() || password.length < 6) {
                        message = "Enter a valid email and a password of at least 6 characters."
                        return@OutlinedButton
                    }
                    scope.launch {
                        loading = true
                        message = ""
                        try {
                            SupabaseManager.client.auth.signUpWith(
                                provider = Email,
                                redirectUrl = EMAIL_CONFIRM_REDIRECT
                            ) {
                                this.email = email.trim()
                                this.password = password
                            }
                            waitingForConfirmation = true
                            message = "Account created. Check your email and tap the confirmation link."
                        } catch (e: Exception) {
                            val raw = e.message.orEmpty()
                            message = if (raw.contains("already", true))
                                "This account already exists. Confirm the email or sign in."
                            else
                                "Unable to create account. Please try again."
                        } finally {
                            loading = false
                        }
                    }
                }
            ) {
                Text("CREATE ACCOUNT")
            }

            if (waitingForConfirmation) {
                Spacer(Modifier.height(14.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("CHECK YOUR EMAIL", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text("Tap the confirmation link. Android should return you to SIMPLE automatically.")
                        Spacer(Modifier.height(8.dp))
                        Text("Then sign in with the same email and password.", fontSize = 12.sp)
                    }
                }
            }

            if (message.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Text(message, fontSize = 14.sp)
            }

            Spacer(Modifier.height(18.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            TextButton(onClick = onDevelopmentAccess) {
                Text("ENTER DEVELOPMENT MODE")
            }
            Text("Temporary development access — removed before release.", fontSize = 11.sp)
        }
    }
}
