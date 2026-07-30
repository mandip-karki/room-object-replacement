package com.roomswap.app.data.model

data class Product(
    val id: String = "",
    val ownerCompanyId: String = "",
    val name: String = "",
    val category: String = "",
    val imageUrl: String = "",
    val createdAt: Long = 0L,
)
