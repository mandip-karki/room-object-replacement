package com.roomswap.app.data.model

enum class CompanyType { MAIN, CLIENT }

data class Company(
    val id: String = "",
    val name: String = "",
    val type: CompanyType = CompanyType.CLIENT,
    val createdBy: String = "",
    val createdAt: Long = 0L,
)
