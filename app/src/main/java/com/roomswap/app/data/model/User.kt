package com.roomswap.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    @SerialName("super_admin") SUPER_ADMIN,
    @SerialName("client_admin") CLIENT_ADMIN,
    @SerialName("sub_account") SUB_ACCOUNT,
}

/** Mirrors a row in the `profiles` table (id == the Supabase Auth user's uid). */
@Serializable
data class User(
    val id: String = "",
    @SerialName("company_id") val companyId: String = "",
    val role: UserRole = UserRole.SUB_ACCOUNT,
    val email: String = "",
    @SerialName("created_at") val createdAt: String? = null,
)
