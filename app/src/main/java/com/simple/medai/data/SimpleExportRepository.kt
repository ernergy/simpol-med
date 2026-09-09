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

data class GeneratedFile(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray
)

object SimpleExportRepository {
    suspend fun createPowerPoint(
        content: String,
        title: String = "Presentacion_SIMPLE"
    ): GeneratedFile = withContext(Dispatchers.IO) {
        val session = SupabaseManager.client.auth.currentSessionOrNull()
            ?: error("Tu sesión ha expirado.")

        val conn = (URL(
            BuildConfig.SUPABASE_URL.trimEnd('/') + "/functions/v1/simple-export"
        ).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 120_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer ${session.accessToken}")
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
        }

        try {
            val request = JSONObject().apply {
                put("type", "pptx")
                put("content", content)
                put("title", title)
            }
            conn.outputStream.use {
                it.write(request.toString().toByteArray(Charsets.UTF_8))
            }

            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = if (text.isBlank()) JSONObject() else JSONObject(text)

            if (status !in 200..299) {
                error(json.optString("error", "No se pudo generar el PowerPoint."))
            }

            GeneratedFile(
                fileName = json.getString("filename"),
                mimeType = json.getString("mimeType"),
                bytes = Base64.decode(json.getString("dataBase64"), Base64.DEFAULT)
            )
        } finally {
            conn.disconnect()
        }
    }

    fun saveToUri(context: Context, uri: Uri, file: GeneratedFile) {
        context.contentResolver.openOutputStream(uri)?.use {
            it.write(file.bytes)
        } ?: error("No se pudo guardar el archivo.")
    }
}
