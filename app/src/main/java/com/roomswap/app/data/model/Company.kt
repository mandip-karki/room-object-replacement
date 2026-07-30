package com.roomswap.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class CompanyType {
    @SerialName("main") MAIN,
    @SerialName("client") CLIENT,
}

@Serializable
data class Company(
    val id: String = "",
    val name: String = "",
    val type: CompanyType = CompanyType.CLIENT,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)
