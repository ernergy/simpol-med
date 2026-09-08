package com.simple.medai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.simple.medai.ui.theme.SIMPLETheme
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SIMPLETheme {
                SimpleApp()
            }
        }
    }
}

@Composable
fun SimpleApp() {

    var loggedIn by remember {
        mutableStateOf(
            SupabaseManager.client.auth.currentSessionOrNull() != null
        )
    }

    if (loggedIn) {
        HomeScreen(
            onLogout = {
                loggedIn = false
            }
        )
    } else {
        LoginScreen(
            onLoginSuccess = {
                loggedIn = true
            }
        )
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "SIMPLE",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Medical Study AI",
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Learn medicine. Simply.",
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                },
                label = {
                    Text("Email")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                },
                label = {
                    Text("Password")
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
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

                            message =
                                e.message ?: "Unable to sign in."

                        } finally {

                            loading = false
                        }
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    if (loading)
                        "PLEASE WAIT..."
                    else
                        "SIGN IN"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {

                    if (email.isBlank() || password.isBlank()) {
                        message = "Enter an email and password first."
                        return@OutlinedButton
                    }

                    if (password.length < 6) {
                        message = "Password must have at least 6 characters."
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

                            message =
                                "Account created. Check your email if confirmation is required."

                        } catch (e: Exception) {

                            message =
                                e.message ?: "Unable to create account."

                        } finally {

                            loading = false
                        }
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {

                Text("CREATE ACCOUNT")
            }

            if (message.isNotBlank()) {

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = message,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "1 free study credit",
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Serializable
data class ProfileData(
    val user_id: String,
    val credits_balance: Int
)

@Composable
fun HomeScreen(
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var credits by remember { mutableStateOf<Int?>(null) }
    var loadingCredits by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val user = SupabaseManager.client.auth.currentUserOrNull()

            if (user != null) {
                val profile = SupabaseManager.client
                    .from("profiles")
                    .select {
                        filter {
                            eq("user_id", user.id)
                        }
                    }
                    .decodeSingle<ProfileData>()

                credits = profile.credits_balance
            }
        } catch (_: Exception) {
            credits = null
        } finally {
            loadingCredits = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp)
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "SIMPLE",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(text = "Your medical study assistant")

            Spacer(modifier = Modifier.height(40.dp))

            when {
                loadingCredits -> Text("Loading credits...")
                credits != null -> Text(
                    text = if (credits == 1) "1 study credit available" else "$credits study credits available",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                else -> Text("Unable to load credits")
            }

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = {
                    // Next step: choose a PDF and create a study session.
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("UPLOAD MEDICAL BOOK")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    // Next step: exam mode.
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("EXAM MODE")
            }

            Spacer(modifier = Modifier.weight(1f))

            TextButton(
                onClick = {
                    scope.launch {
                        SupabaseManager.client.auth.signOut()
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
