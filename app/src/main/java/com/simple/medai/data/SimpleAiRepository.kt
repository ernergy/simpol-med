package com.simple.medai.data

import com.simple.medai.BuildConfig
import com.simple.medai.SupabaseManager
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class SimpleAiResult(val answer: String, val model: String? = null)

object SimpleAiRepository {
    suspend fun ask(prompt: String, bookName: String?): SimpleAiResult =
        withContext(Dispatchers.IO) {
            val session = SupabaseManager.client.auth.currentSessionOrNull()
                ?: throw IllegalStateException("Tu sesión ha expirado. Vuelve a ingresar.")

            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + "/functions/v1/simple-ai"
            val body = JSONObject().apply {
                put("prompt", prompt)
                put("bookName", bookName ?: "")
            }.toString()

            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20000
                readTimeout = 90000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer ${session.accessToken}")
                setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            }

            try {
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val status = conn.responseCode
                val stream = if (status in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                val json = if (text.isNotBlank()) JSONObject(text) else JSONObject()

                if (status !in 200..299) {
                    throw IllegalStateException(
                        json.optString("error", "No se pudo consultar la inteligencia artificial.")
                    )
                }

                val answer = json.optString("answer").trim()
                if (answer.isBlank()) {
                    throw IllegalStateException("La inteligencia artificial respondió sin texto.")
                }

                SimpleAiResult(
                    answer = answer,
                    model = json.optString("model").takeIf { it.isNotBlank() }
                )
            } finally {
                conn.disconnect()
            }
        }
}
