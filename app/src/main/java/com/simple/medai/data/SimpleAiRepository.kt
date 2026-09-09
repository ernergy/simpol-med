package com.simple.medai.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.simple.medai.BuildConfig
import com.simple.medai.SupabaseManager
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AiAttachment(
    val uri: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long
)

data class SimpleAiResult(val answer: String, val model: String? = null)

object SimpleAiRepository {
    const val MAX_FILE_BYTES = 512L * 1024L * 1024L
    private const val CHUNK_BYTES = 3 * 1024 * 1024

    suspend fun ask(
        context: Context,
        prompt: String,
        action: String,
        attachment: AiAttachment?,
        onUploadProgress: (Int) -> Unit = {}
    ): SimpleAiResult = withContext(Dispatchers.IO) {
        val fileId = if (attachment != null) {
            if (attachment.sizeBytes <= 0L) error("No se pudo determinar el tamaño del archivo.")
            if (attachment.sizeBytes > MAX_FILE_BYTES) error("El archivo supera 512 MB.")
            uploadInParts(context, attachment, onUploadProgress)
        } else null

        val json = post(
            "simple-ai",
            JSONObject().apply {
                put("prompt", prompt)
                put("action", action)
                if (fileId != null) {
                    put("fileId", fileId)
                    put("attachmentName", attachment?.name ?: "")
                    put("attachmentMime", attachment?.mimeType ?: "")
                }
            }
        )

        val answer = json.optString("answer").trim()
        if (answer.isBlank()) error("La IA respondió sin texto.")
        SimpleAiResult(answer, json.optString("model").takeIf { it.isNotBlank() })
    }

    private fun uploadInParts(
        context: Context,
        file: AiAttachment,
        onProgress: (Int) -> Unit
    ): String {
        val init = post("simple-upload", JSONObject().apply {
            put("action", "init")
            put("bytes", file.sizeBytes)
            put("filename", file.name)
            put("mimeType", file.mimeType)
        })
        val uploadId = init.getString("uploadId")
        val partIds = mutableListOf<String>()
        var uploaded = 0L

        try {
            context.contentResolver.openInputStream(Uri.parse(file.uri))?.use { input ->
                val buffer = ByteArray(CHUNK_BYTES)
                while (true) {
                    var count = 0
                    while (count < buffer.size) {
                        val n = input.read(buffer, count, buffer.size - count)
                        if (n <= 0) break
                        count += n
                    }
                    if (count == 0) break

                    val bytes = if (count == buffer.size) buffer else buffer.copyOf(count)
                    val part = post("simple-upload", JSONObject().apply {
                        put("action", "part")
                        put("uploadId", uploadId)
                        put("dataBase64", Base64.encodeToString(bytes, Base64.NO_WRAP))
                    })
                    partIds += part.getString("partId")
                    uploaded += count
                    onProgress(((uploaded * 100) / file.sizeBytes).coerceIn(0,100).toInt())
                    if (count < buffer.size) break
                }
            } ?: error("No se pudo abrir el archivo.")

            val done = post("simple-upload", JSONObject().apply {
                put("action", "complete")
                put("uploadId", uploadId)
                put("partIds", JSONArray(partIds))
            })
            onProgress(100)
            return done.getString("fileId")
        } catch (e: Exception) {
            try {
                post("simple-upload", JSONObject().apply {
                    put("action", "cancel")
                    put("uploadId", uploadId)
                })
            } catch (_: Exception) {}
            throw e
        }
    }

    private fun post(function: String, body: JSONObject): JSONObject {
        val session = SupabaseManager.client.auth.currentSessionOrNull()
            ?: error("Tu sesión ha expirado. Vuelve a ingresar.")

        val conn = (URL(
            BuildConfig.SUPABASE_URL.trimEnd('/') + "/functions/v1/$function"
        ).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 180_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer ${session.accessToken}")
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
        }

        try {
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = if (text.isBlank()) JSONObject() else JSONObject(text)
            if (status !in 200..299) error(json.optString("error", "Error de conexión."))
            return json
        } finally {
            conn.disconnect()
        }
    }
}
