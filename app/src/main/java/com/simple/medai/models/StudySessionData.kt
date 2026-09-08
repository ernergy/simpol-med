package com.simple.medai.models

import kotlinx.serialization.Serializable

@Serializable
data class NewStudySession(
    val user_id: String,
    val title: String,
    val session_type: String = "book",
    val status: String = "created",
    val credits_used: Int = 0
)

@Serializable
data class StudySessionData(
    val id: String,
    val user_id: String,
    val title: String,
    val session_type: String,
    val status: String,
    val credits_used: Int
)

@Serializable
data class NewMaterial(
    val user_id: String,
    val session_id: String,
    val file_name: String,
    val file_size_bytes: Long,
    val processing_status: String = "selected",
    val original_deleted: Boolean = false
)
