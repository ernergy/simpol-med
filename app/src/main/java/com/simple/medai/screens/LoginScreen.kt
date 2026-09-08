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

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var accountCreated by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("SIMPLE", fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Medical Study AI", fontSize = 20.sp)
            Text("Learn medicine. Simply.", fontSize = 14.sp)

            Spacer(Modifier.height(40.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

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
                            message = if (e.message.orEmpty().contains("invalid_credentials", true)) "Incorrect email or password." else "Unable to sign in. Please try again."
                        } finally {
                            loading = false
                        }
                    }
                }
            ) {
                Text(if (loading) "PLEASE WAIT..." else "SIGN IN")
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (email.isBlank() || password.length < 6) {
                        message = "Enter an email and a password of at least 6 characters."
                        return@OutlinedButton
                    }
                    scope.launch {
                        loading = true
                        message = ""
                        try {
                            SupabaseManager.client.auth.signUpWith(Email) {
                                this.email = email.trim()
                                this.password = password
                            }
                            message = "Account created. Check your email if confirmation is required."
                        } catch (e: Exception) {
                            message = e.message ?: "Unable to create account."
                        } finally {
                            loading = false
                        }
                    }
                }
            ) {
                Text("CREATE ACCOUNT")
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(onClick = onDevelopmentAccess) {
                Text("ENTER DEVELOPMENT MODE")
            }
            Text(
                "Development only — removed before release.",
                fontSize = 11.sp
            )

            if (message.isNotBlank()) {
                Spacer(Modifier.height(18.dp))
                Text(message, fontSize = 14.sp)
            }

            Spacer(Modifier.height(26.dp))
            Text("1 free study credit", fontWeight = FontWeight.Medium)
        }
    }
}
