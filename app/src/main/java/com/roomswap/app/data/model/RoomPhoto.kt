package com.roomswap.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoomPhoto(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("image_url") val imageUrl: String = "",
    @SerialName("created_at") val createdAt: String? = null,
)
