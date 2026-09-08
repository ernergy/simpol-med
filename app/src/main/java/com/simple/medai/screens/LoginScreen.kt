package com.simple.medai.screens

import android.util.Log
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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val EMAIL_CONFIRM_REDIRECT = "simple://auth-confirm"
private const val TAG = "SIMPLE_AUTH"

private fun safeError(e: Throwable): String {
    val raw = e.message.orEmpty()
        .replace(Regex("https?://\\S+"), "[url]")
        .replace(Regex("sb_[A-Za-z0-9_\\-]+"), "[key]")
        .take(500)

    return if (raw.isBlank()) e::class.simpleName ?: "Unknown error"
    else "${e::class.simpleName}: $raw"
}

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

    LaunchedEffect(Unit) {
        Log.d(TAG, "LoginScreen opened")
        Log.d(TAG, "Current session exists = ${SupabaseManager.client.auth.currentSessionOrNull() != null}")
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("SIMPLE", fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Medical Study AI", fontSize = 20.sp)
            Text("Learn medicine. Simply.", fontSize = 14.sp)

            Spacer(Modifier.height(30.dp))

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
                    Log.d(TAG, "SIGN IN pressed")

                    if (email.isBlank() || password.isBlank()) {
                        Log.w(TAG, "SIGN IN blocked: missing email/password")
                        message = "Enter your email and password."
                        return@Button
                    }

                    scope.launch {
                        loading = true
                        message = "Signing in..."
                        Log.d(TAG, "SIGN IN request started for ${email.trim()}")

                        try {
                            withTimeout(15000) {
                                SupabaseManager.client.auth.signInWith(Email) {
                                    this.email = email.trim()
                                    this.password = password
                                }
                            }

                            Log.d(TAG, "SIGN IN success")
                            message = "Sign in successful."
                            onLoginSuccess()

                        } catch (_: TimeoutCancellationException) {
                            Log.e(TAG, "SIGN IN timeout after 15 seconds")
                            message = "LOGIN TIMEOUT: Supabase did not answer within 15 seconds."
                        } catch (e: Exception) {
                            val err = safeError(e)
                            Log.e(TAG, "SIGN IN error: $err", e)
                            message = "LOGIN ERROR: $err"
                        } finally {
                            loading = false
                            Log.d(TAG, "SIGN IN finished")
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
                    Log.d(TAG, "CREATE ACCOUNT pressed")

                    if (email.isBlank() || password.length < 6) {
                        Log.w(TAG, "SIGNUP blocked: invalid email/password length")
                        message = "Enter a valid email and a password of at least 6 characters."
                        return@OutlinedButton
                    }

                    scope.launch {
                        loading = true
                        message = "Creating account..."
                        Log.d(TAG, "SIGNUP request started for ${email.trim()}")
                        Log.d(TAG, "SIGNUP redirect = $EMAIL_CONFIRM_REDIRECT")

                        try {
                            withTimeout(15000) {
                                SupabaseManager.client.auth.signUpWith(
                                    provider = Email,
                                    redirectUrl = EMAIL_CONFIRM_REDIRECT
                                ) {
                                    this.email = email.trim()
                                    this.password = password
                                }
                            }

                            Log.d(TAG, "SIGNUP success")
                            waitingForConfirmation = true
                            message = "SIGNUP OK: Account created. Check your email."

                        } catch (_: TimeoutCancellationException) {
                            Log.e(TAG, "SIGNUP timeout after 15 seconds")
                            message = "SIGNUP TIMEOUT: Supabase did not answer within 15 seconds."
                        } catch (e: Exception) {
                            val err = safeError(e)
                            Log.e(TAG, "SIGNUP error: $err", e)
                            message = "SIGNUP ERROR: $err"
                        } finally {
                            loading = false
                            Log.d(TAG, "SIGNUP finished")
                        }
                    }
                }
            ) {
                Text("CREATE ACCOUNT")
            }

            if (waitingForConfirmation) {
                Spacer(Modifier.height(12.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("CHECK YOUR EMAIL", fontWeight = FontWeight.Bold)
                        Text("Tap the confirmation link and return to SIMPLE.")
                    }
                }
            }

            if (message.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(14.dp),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            TextButton(onClick = onDevelopmentAccess) {
                Text("ENTER DEVELOPMENT MODE")
            }
        }
    }
}
