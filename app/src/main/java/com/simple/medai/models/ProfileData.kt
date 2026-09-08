package com.simple.medai.models

import kotlinx.serialization.Serializable

@Serializable
data class ProfileData(
    val user_id: String,
    val credits_balance: Int
)
