package com.roomswap.app.data.repository

import com.roomswap.app.SupabaseClientProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.serialization.Serializable

@Serializable
private data class CreateSubAccountRequest(val email: String, val password: String)

@Serializable
private data class CreateSubAccountResponse(val uid: String)

class AdminRepository(
    private val client: SupabaseClient = SupabaseClientProvider.client,
) {
    /** Calls the `create-sub-account` Edge Function; the new account's companyId comes
     *  from the calling Client Admin's own row server-side, never from this request. */
    suspend fun createSubAccount(email: String, password: String): String {
        val response = client.functions.invoke("create-sub-account", CreateSubAccountRequest(email, password))
        return response.body<CreateSubAccountResponse>().uid
    }
}
