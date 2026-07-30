package com.roomswap.app.data.model

enum class UserRole { SUPER_ADMIN, CLIENT_ADMIN, SUB_ACCOUNT }

data class User(
    val id: String = "",
    val companyId: String = "",
    val role: UserRole = UserRole.SUB_ACCOUNT,
    val email: String = "",
    val createdAt: Long = 0L,
)
