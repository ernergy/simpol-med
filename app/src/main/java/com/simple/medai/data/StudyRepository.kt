package com.simple.medai.data

import com.simple.medai.BuildConfig
import com.simple.medai.SupabaseManager
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class StudyQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class StudyQuiz(
    val title: String,
    val questions: List<StudyQuestion>
)

object StudyRepository {

    suspend fun generateQuiz(
        topic: String,
        previousQuestions: List<String> = emptyList()
    ): StudyQuiz = withContext(Dispatchers.IO) {

        val session = SupabaseManager.client.auth.currentSessionOrNull()
            ?: error("Tu sesión ha expirado.")

        val conn = (
            URL(
                BuildConfig.SUPABASE_URL.trimEnd('/') +
                    "/functions/v1/simple-study"
            ).openConnection() as HttpURLConnection
        ).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 120_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty(
                "Authorization",
                "Bearer ${session.accessToken}"
            )
            setRequestProperty(
                "apikey",
                BuildConfig.SUPABASE_PUBLISHABLE_KEY
            )
        }

        try {
            val body = JSONObject().apply {
                put("topic", topic)
                put(
                    "previousQuestions",
                    org.json.JSONArray(previousQuestions)
                )
            }

            conn.outputStream.use {
                it.write(
                    body.toString()
                        .toByteArray(Charsets.UTF_8)
                )
            }

            val status = conn.responseCode
            val stream =
                if (status in 200..299) {
                    conn.inputStream
                } else {
                    conn.errorStream
                }

            val text = stream
                ?.bufferedReader()
                ?.use { it.readText() }
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
                        "No se pudo generar la práctica."
                    )
                )
            }

            val array = json.getJSONArray("questions")
            val list = mutableListOf<StudyQuestion>()

            for (i in 0 until array.length()) {
                val q = array.getJSONObject(i)
                val optionsJson = q.getJSONArray("options")
                val options = mutableListOf<String>()

                for (j in 0 until optionsJson.length()) {
                    options += optionsJson.getString(j)
                }

                list += StudyQuestion(
                    question = q.getString("question"),
                    options = options,
                    correctIndex = q.getInt("correctIndex"),
                    explanation = q.getString("explanation")
                )
            }

            StudyQuiz(
                title = json.optString(
                    "title",
                    "Práctica: $topic"
                ),
                questions = list
            )

        } finally {
            conn.disconnect()
        }
    }
}
