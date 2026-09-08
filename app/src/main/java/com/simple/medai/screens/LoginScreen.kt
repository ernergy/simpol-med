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
    val scope = rememberCoroutineScope()

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = SimpleCardShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    "S",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(Modifier.height(18.dp))
            Text("SIMPLE", fontSize = 40.sp, fontWeight = FontWeight.Black)
            Text(
                "Smart Learning for Medical People",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(36.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = SimpleCardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "Welcome",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Sign in to continue studying.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.height(18.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; message = "" },
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = SimpleButtonShape
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; message = "" },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = SimpleButtonShape
                    )

                    Spacer(Modifier.height(18.dp))

                    SimplePrimaryButton(
                        text = if (loading) "PLEASE WAIT..." else "SIGN IN",
                        enabled = !loading,
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                message = "Enter your email and password."
                                return@SimplePrimaryButton
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
                                    message = if (e.message.orEmpty().contains("invalid_credentials", true))
                                        "Incorrect email or password."
                                    else
                                        "Unable to sign in. Please try again."
                                } finally {
                                    loading = false
                                }
                            }
                        }
                    )

                    Spacer(Modifier.height(10.dp))

                    SimpleOutlinedButton(
                        text = "CREATE ACCOUNT",
                        enabled = !loading,
                        onClick = {
                            if (email.isBlank() || password.length < 6) {
                                message = "Use a valid email and a password of at least 6 characters."
                                return@SimpleOutlinedButton
                            }
                            scope.launch {
                                loading = true
                                message = ""
                                try {
                                    SupabaseManager.client.auth.signUpWith(Email) {
                                        this.email = email.trim()
                                        this.password = password
                                    }
                                    if (SupabaseManager.client.auth.currentSessionOrNull() != null) {
                                        onLoginSuccess()
                                    } else {
                                        message = "Account created. Please sign in."
                                    }
                                } catch (e: Exception) {
                                    message = if (e.message.orEmpty().contains("already", true))
                                        "This account already exists. Use SIGN IN."
                                    else
                                        "Unable to create account. Please try again."
                                } finally {
                                    loading = false
                                }
                            }
                        }
                    )

                    if (message.isNotBlank()) {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            message,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
