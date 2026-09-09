package com.simple.medai.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.simple.medai.BuildConfig
import com.simple.medai.SupabaseManager
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

data class SimpleAiResult(
    val answer: String,
    val model: String? = null,
    val responseId: String? = null,
    val retainedFileId: String? = null,
    val vectorStoreId: String? = null,
    val artifactType: String? = null
)

object SimpleAiRepository {
    const val MAX_FILE_BYTES = 512L * 1024L * 1024L

    // Dejamos margen bajo el límite práctico de 50 MB del input_file.
    private const val DIRECT_FILE_LIMIT_BYTES = 45L * 1024L * 1024L
    private const val CHUNK_BYTES = 3 * 1024 * 1024

    suspend fun ask(
        context: Context,
        prompt: String,
        action: String,
        attachment: AiAttachment?,
        previousResponseId: String?,
        requestedArtifact: String?,
        currentVectorStoreId: String?,
        onUploadProgress: (Int) -> Unit = {},
        onProcessingStatus: (String) -> Unit = {}
    ): SimpleAiResult = withContext(Dispatchers.IO) {

        var fileId: String? = null
        var vectorStoreId = currentVectorStoreId

        if (attachment != null) {
            if (attachment.sizeBytes <= 0L) {
                error("No se pudo determinar el tamaño del archivo.")
            }

            if (attachment.sizeBytes > MAX_FILE_BYTES) {
                error("El archivo supera 512 MB, límite actual de SIMPLE.")
            }

            onProcessingStatus("Subiendo archivo…")
            fileId = uploadInParts(
                context = context,
                file = attachment,
                onProgress = onUploadProgress
            )

            if (attachment.sizeBytes > DIRECT_FILE_LIMIT_BYTES) {
                onProcessingStatus("Preparando archivo grande para análisis…")
                vectorStoreId = indexLargeFile(
                    fileId = fileId,
                    existingVectorStoreId = vectorStoreId,
                    onStatus = onProcessingStatus
                )
            }
        }

        onProcessingStatus("SIMPLE está analizando…")

        val json = post(
            function = "simple-ai",
            body = JSONObject().apply {
                put("prompt", prompt)
                put("action", action)

                if (!previousResponseId.isNullOrBlank()) {
                    put("previousResponseId", previousResponseId)
                }

                if (!requestedArtifact.isNullOrBlank()) {
                    put("requestedArtifact", requestedArtifact)
                }

                if (!vectorStoreId.isNullOrBlank()) {
                    put("vectorStoreId", vectorStoreId)
                }

                if (!fileId.isNullOrBlank()) {
                    put("fileId", fileId)
                    put("attachmentName", attachment?.name ?: "")
                    put("attachmentMime", attachment?.mimeType ?: "")
                }
            },
            timeout = 180_000
        )

        val answer = json.optString("answer").trim()

        if (answer.isBlank()) {
            error("La IA respondió sin texto.")
        }

        SimpleAiResult(
            answer = answer,
            model = json.optString("model").takeIf { it.isNotBlank() },
            responseId = json.optString("response_id").takeIf { it.isNotBlank() },
            retainedFileId = json.optString("retained_file_id").takeIf { it.isNotBlank() },
            vectorStoreId = json.optString("vector_store_id").takeIf { it.isNotBlank() }
                ?: vectorStoreId,
            artifactType = json.optString("artifact_type").takeIf { it.isNotBlank() }
        )
    }

    suspend fun cleanupResources(
        fileIds: Collection<String>,
        vectorStoreId: String?
    ) = withContext(Dispatchers.IO) {
        if (fileIds.isEmpty() && vectorStoreId.isNullOrBlank()) {
            return@withContext
        }

        post(
            function = "simple-index",
            body = JSONObject().apply {
                put("action", "cleanup")

                if (!vectorStoreId.isNullOrBlank()) {
                    put("vectorStoreId", vectorStoreId)
                }

                put(
                    "fileIds",
                    JSONArray(fileIds.toList())
                )
            },
            timeout = 60_000
        )
    }

    private suspend fun indexLargeFile(
        fileId: String,
        existingVectorStoreId: String?,
        onStatus: (String) -> Unit
    ): String {

        val started = post(
            function = "simple-index",
            body = JSONObject().apply {
                put("action", "index")
                put("fileId", fileId)

                if (!existingVectorStoreId.isNullOrBlank()) {
                    put("vectorStoreId", existingVectorStoreId)
                }
            },
            timeout = 60_000
        )

        val vectorStoreId = started.getString("vectorStoreId")

        repeat(150) { attempt ->
            val statusJson = post(
                function = "simple-index",
                body = JSONObject().apply {
                    put("action", "status")
                    put("fileId", fileId)
                    put("vectorStoreId", vectorStoreId)
                },
                timeout = 30_000
            )

            when (statusJson.optString("status")) {
                "completed" -> {
                    onStatus("Archivo listo. Analizando contenido…")
                    return vectorStoreId
                }

                "failed", "cancelled" -> {
                    val detail = statusJson
                        .optJSONObject("last_error")
                        ?.optString("message")
                        .orEmpty()

                    error(
                        if (detail.isBlank()) {
                            "No se pudo preparar el archivo grande."
                        } else {
                            "No se pudo preparar el archivo: $detail"
                        }
                    )
                }

                else -> {
                    val seconds = (attempt + 1) * 2
                    onStatus(
                        "Preparando archivo grande… ${seconds}s"
                    )
                    delay(2_000)
                }
            }
        }

        error(
            "El archivo sigue procesándose después de 5 minutos. " +
                "Intenta nuevamente en unos minutos."
        )
    }

    private fun uploadInParts(
        context: Context,
        file: AiAttachment,
        onProgress: (Int) -> Unit
    ): String {

        val init = post(
            "simple-upload",
            JSONObject().apply {
                put("action", "init")
                put("bytes", file.sizeBytes)
                put("filename", file.name)
                put("mimeType", file.mimeType)
            },
            60_000
        )

        val uploadId = init.getString("uploadId")
        val parts = mutableListOf<String>()
        var uploaded = 0L

        try {
            context.contentResolver
                .openInputStream(Uri.parse(file.uri))
                ?.use { input ->

                    val buffer = ByteArray(CHUNK_BYTES)

                    while (true) {
                        var count = 0

                        while (count < buffer.size) {
                            val n = input.read(
                                buffer,
                                count,
                                buffer.size - count
                            )

                            if (n <= 0) break
                            count += n
                        }

                        if (count == 0) break

                        val bytes =
                            if (count == buffer.size) {
                                buffer
                            } else {
                                buffer.copyOf(count)
                            }

                        val part = post(
                            "simple-upload",
                            JSONObject().apply {
                                put("action", "part")
                                put("uploadId", uploadId)
                                put(
                                    "dataBase64",
                                    Base64.encodeToString(
                                        bytes,
                                        Base64.NO_WRAP
                                    )
                                )
                            },
                            90_000
                        )

                        parts += part.getString("partId")
                        uploaded += count

                        onProgress(
                            (
                                (uploaded * 100) /
                                    file.sizeBytes
                                )
                                .coerceIn(0, 100)
                                .toInt()
                        )

                        if (count < buffer.size) break
                    }
                }
                ?: error("No se pudo abrir el archivo.")

            val done = post(
                "simple-upload",
                JSONObject().apply {
                    put("action", "complete")
                    put("uploadId", uploadId)
                    put(
                        "partIds",
                        JSONArray(parts)
                    )
                },
                90_000
            )

            onProgress(100)

            return done.getString("fileId")

        } catch (e: Exception) {
            try {
                post(
                    "simple-upload",
                    JSONObject().apply {
                        put("action", "cancel")
                        put("uploadId", uploadId)
                    },
                    30_000
                )
            } catch (_: Exception) {
            }

            throw e
        }
    }

    private fun post(
        function: String,
        body: JSONObject,
        timeout: Int
    ): JSONObject {

        val session =
            SupabaseManager
                .client
                .auth
                .currentSessionOrNull()
                ?: error(
                    "Tu sesión ha expirado."
                )

        val connection =
            (
                URL(
                    BuildConfig
                        .SUPABASE_URL
                        .trimEnd('/') +
                        "/functions/v1/$function"
                ).openConnection()
                    as HttpURLConnection
                )
                .apply {

                    requestMethod = "POST"
                    connectTimeout = 30_000
                    readTimeout = timeout
                    doOutput = true

                    setRequestProperty(
                        "Content-Type",
                        "application/json"
                    )

                    setRequestProperty(
                        "Authorization",
                        "Bearer ${session.accessToken}"
                    )

                    setRequestProperty(
                        "apikey",
                        BuildConfig
                            .SUPABASE_PUBLISHABLE_KEY
                    )
                }

        try {
            connection
                .outputStream
                .use {
                    it.write(
                        body
                            .toString()
                            .toByteArray(
                                Charsets.UTF_8
                            )
                    )
                }

            val status =
                connection.responseCode

            val stream =
                if (status in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

            val text =
                stream
                    ?.bufferedReader()
                    ?.use {
                        it.readText()
                    }
                    .orEmpty()

            val json =
                if (text.isBlank()) {
                    JSONObject()
                } else {
                    JSONObject(text)
                }

            if (status !in 200..299) {
                error(
                    json.optString(
                        "error",
                        "Error de conexión."
                    )
                )
            }

            return json

        } finally {
            connection.disconnect()
        }
    }
}
