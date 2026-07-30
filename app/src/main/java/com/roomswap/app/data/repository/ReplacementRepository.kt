package com.roomswap.app.data.repository

import com.roomswap.app.SupabaseClientProvider
import com.roomswap.app.data.model.ReplacementJob
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.ktor.client.call.body
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class ReplaceRequest(
    @SerialName("room_photo_url") val roomPhotoUrl: String,
    @SerialName("tap_x") val tapX: Double,
    @SerialName("tap_y") val tapY: Double,
    @SerialName("tapped_region_image_url") val tappedRegionImageUrl: String,
    @SerialName("replacement_image_url") val replacementImageUrl: String,
)

@Serializable
private data class ReplaceResponse(@SerialName("job_id") val jobId: String)

class ReplacementRepository(
    private val client: SupabaseClient = SupabaseClientProvider.client,
) {
    /** Calls the `replace` Edge Function; returns the job id to poll for a result. */
    suspend fun startReplacement(
        roomPhotoUrl: String,
        tapX: Double,
        tapY: Double,
        tappedRegionImageUrl: String,
        replacementImageUrl: String,
    ): String {
        val response = client.functions.invoke(
            "replace",
            ReplaceRequest(roomPhotoUrl, tapX, tapY, tappedRegionImageUrl, replacementImageUrl),
        )
        return response.body<ReplaceResponse>().jobId
    }

    suspend fun getJob(jobId: String): ReplacementJob =
        client.from("replacement_jobs").select {
            filter { eq("id", jobId) }
        }.decodeSingle()
}
