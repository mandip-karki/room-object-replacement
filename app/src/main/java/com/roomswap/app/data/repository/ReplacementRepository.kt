package com.roomswap.app.data.repository

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.firestore.FirebaseFirestore
import com.roomswap.app.data.model.JobStatus
import com.roomswap.app.data.model.ReplacementJob
import kotlinx.coroutines.tasks.await

class ReplacementRepository(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    /** Calls the `replace` Cloud Function; returns the job_id to poll. */
    suspend fun startReplacement(
        roomPhotoUrl: String,
        tapX: Double,
        tapY: Double,
        replacementImageUrl: String,
    ): String {
        val payload = hashMapOf(
            "room_photo_url" to roomPhotoUrl,
            "tap_x" to tapX,
            "tap_y" to tapY,
            "replacement_image_url" to replacementImageUrl,
        )
        val result = functions.getHttpsCallable("replace").call(payload).await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as Map<String, Any>
        return data["job_id"] as String
    }

    suspend fun getJob(jobId: String): ReplacementJob {
        val doc = firestore.collection("replacement_jobs").document(jobId).get().await()
        return ReplacementJob(
            id = doc.id,
            userId = doc.getString("userId") ?: "",
            roomPhotoId = doc.getString("roomPhotoId") ?: "",
            productId = doc.getString("productId"),
            customItemImageUrl = doc.getString("customItemImageUrl"),
            resultImageUrl = doc.getString("resultImageUrl"),
            status = JobStatus.valueOf(doc.getString("status") ?: JobStatus.PENDING.name),
            createdAt = doc.getLong("createdAt") ?: 0L,
        )
    }
}
