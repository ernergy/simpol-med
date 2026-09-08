package com.simple.medai.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
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
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = "Logo SIMPLE",
                    modifier = Modifier.padding(18.dp).size(42.dp)
                )
            }

            Spacer(Modifier.height(18.dp))
            Text("SIMPLE", fontSize = 40.sp, fontWeight = FontWeight.Black)
            Text(
                "Aprendizaje inteligente para medicina",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(34.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = SimpleCardShape
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Bienvenido", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Ingresa para continuar estudiando.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.height(18.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; message = "" },
                        label = { Text("Correo electrónico") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = SimpleButtonShape
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; message = "" },
                        label = { Text("Contraseña") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = SimpleButtonShape
                    )

                    Spacer(Modifier.height(18.dp))

                    SimplePrimaryButton(
                        text = if (loading) "ESPERE..." else "INGRESAR",
                        enabled = !loading,
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                message = "Ingresa tu correo y contraseña."
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
                                        "Correo o contraseña incorrectos."
                                    else
                                        "No se pudo iniciar sesión."
                                } finally {
                                    loading = false
                                }
                            }
                        }
                    )

                    Spacer(Modifier.height(10.dp))

                    SimpleOutlinedButton(
                        text = "CREAR CUENTA",
                        enabled = !loading,
                        onClick = {
                            if (email.isBlank() || password.length < 6) {
                                message = "Usa un correo válido y una contraseña de al menos 6 caracteres."
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
                                        message = "Cuenta creada. Ahora ingresa."
                                    }
                                } catch (e: Exception) {
                                    message = if (e.message.orEmpty().contains("already", true))
                                        "Esta cuenta ya existe. Usa INGRESAR."
                                    else
                                        "No se pudo crear la cuenta."
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
