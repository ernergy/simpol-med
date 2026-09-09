package com.simple.medai.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.simple.medai.BuildConfig
import com.simple.medai.SupabaseManager
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AiAttachment(
    val uri: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long
)

data class SimpleAiResult(
    val answer: String,
    val model: String? = null,
    val attachmentReceived: Boolean = false
)

object SimpleAiRepository {

    const val MAX_ATTACHMENT_BYTES = 10L * 1024L * 1024L

    suspend fun ask(
        context: Context,
        prompt: String,
        action: String = "chat",
        attachment: AiAttachment? = null
    ): SimpleAiResult = withContext(Dispatchers.IO) {

        val session = SupabaseManager.client.auth.currentSessionOrNull()
            ?: throw IllegalStateException("Tu sesión ha expirado. Vuelve a ingresar.")

        if (attachment != null && attachment.sizeBytes > MAX_ATTACHMENT_BYTES) {
            throw IllegalStateException("El archivo supera el límite temporal de 10 MB.")
        }

        val request = JSONObject().apply {
            put("prompt", prompt)
            put("action", action)

            if (attachment != null) {
                val bytes = context.contentResolver
                    .openInputStream(Uri.parse(attachment.uri))
                    ?.use { it.readBytes() }
                    ?: throw IllegalStateException("No se pudo abrir el archivo adjunto.")

                if (bytes.size > MAX_ATTACHMENT_BYTES) {
                    throw IllegalStateException("El archivo supera el límite temporal de 10 MB.")
                }

                put("attachment", JSONObject().apply {
                    put("name", attachment.name)
                    put("mimeType", attachment.mimeType)
                    put("dataBase64", Base64.encodeToString(bytes, Base64.NO_WRAP))
                })
            }
        }.toString()

        val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') + "/functions/v1/simple-ai"

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 25_000
            readTimeout = 120_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer ${session.accessToken}")
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
        }

        try {
            connection.outputStream.use {
                it.write(request.toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val text = stream
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()

            val json = if (text.isNotBlank()) JSONObject(text) else JSONObject()

            if (status !in 200..299) {
                throw IllegalStateException(
                    json.optString(
                        "error",
                        "No se pudo consultar la inteligencia artificial."
                    )
                )
            }

            val answer = json.optString("answer").trim()
            if (answer.isBlank()) {
                throw IllegalStateException("La IA respondió sin texto.")
            }

            SimpleAiResult(
                answer = answer,
                model = json.optString("model").takeIf { it.isNotBlank() },
                attachmentReceived = json.optBoolean("attachment_received", false)
            )
        } finally {
            connection.disconnect()
        }
    }
}
