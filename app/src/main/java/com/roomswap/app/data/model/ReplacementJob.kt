package com.roomswap.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class JobStatus {
    @SerialName("pending") PENDING,
    @SerialName("done") DONE,
    @SerialName("failed") FAILED,
}

@Serializable
data class ReplacementJob(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("company_id") val companyId: String = "",
    @SerialName("room_photo_url") val roomPhotoUrl: String = "",
    @SerialName("tap_x") val tapX: Double = 0.0,
    @SerialName("tap_y") val tapY: Double = 0.0,
    @SerialName("product_id") val productId: String? = null,
    @SerialName("custom_item_image_url") val customItemImageUrl: String? = null,
    @SerialName("tapped_label") val tappedLabel: String? = null,
    @SerialName("result_image_url") val resultImageUrl: String? = null,
    val status: JobStatus = JobStatus.PENDING,
    val error: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)
