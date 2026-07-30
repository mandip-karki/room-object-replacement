package com.roomswap.app.data.model

enum class JobStatus { PENDING, DONE, FAILED }

data class ReplacementJob(
    val id: String = "",
    val userId: String = "",
    val roomPhotoId: String = "",
    val productId: String? = null,
    val customItemImageUrl: String? = null,
    val tapX: Double = 0.0,
    val tapY: Double = 0.0,
    val resultImageUrl: String? = null,
    val status: JobStatus = JobStatus.PENDING,
    val createdAt: Long = 0L,
)
