package com.roomswap.app.auth

import com.roomswap.app.SupabaseClientProvider
import com.roomswap.app.data.model.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from

/**
 * Role and companyId come from the `profiles` table, not trusted from the client
 * elsewhere. Edge Functions re-derive them from the caller's own row on every call.
 */
class AuthRepository(
    private val client: SupabaseClient = SupabaseClientProvider.client,
) {
    suspend fun signIn(email: String, password: String): User {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        val uid = client.auth.currentUserOrNull()?.id ?: error("Sign-in succeeded but no user returned")
        return loadProfile(uid)
    }

    suspend fun currentUser(): User? {
        val uid = client.auth.currentUserOrNull()?.id ?: return null
        return loadProfile(uid)
    }

    suspend fun signOut() = client.auth.signOut()

    private suspend fun loadProfile(uid: String): User =
        client.from("profiles").select {
            filter { eq("id", uid) }
        }.decodeSingle()
}
