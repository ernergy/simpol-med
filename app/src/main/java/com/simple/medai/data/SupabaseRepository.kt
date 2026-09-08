package com.simple.medai.data

import com.simple.medai.SupabaseManager
import com.simple.medai.models.*
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

object SupabaseRepository {
    fun isLoggedIn() = SupabaseManager.client.auth.currentSessionOrNull() != null

    suspend fun loadProfile(): ProfileData? {
        val user = SupabaseManager.client.auth.currentUserOrNull() ?: return null
        return SupabaseManager.client.from("profiles").select {
            filter { eq("user_id", user.id) }
        }.decodeSingle<ProfileData>()
    }

    suspend fun createStudySession(book: SelectedBook): StudySessionData? {
        val user = SupabaseManager.client.auth.currentUserOrNull() ?: return null
        val session = SupabaseManager.client.from("study_sessions").insert(
            NewStudySession(user_id = user.id, title = book.name)
        ) { select() }.decodeSingle<StudySessionData>()

        SupabaseManager.client.from("materials").insert(
            NewMaterial(
                user_id = user.id,
                session_id = session.id,
                file_name = book.name,
                file_size_bytes = book.sizeBytes
            )
        )
        return session
    }

    suspend fun signOut() = SupabaseManager.client.auth.signOut()
}
