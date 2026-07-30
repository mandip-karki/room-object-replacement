package com.roomswap.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String = "",
    @SerialName("owner_company_id") val ownerCompanyId: String = "",
    val name: String = "",
    val category: String = "",
    @SerialName("image_url") val imageUrl: String = "",
    @SerialName("created_at") val createdAt: String? = null,
)
