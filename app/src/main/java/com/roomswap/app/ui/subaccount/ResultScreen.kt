package com.roomswap.app.ui.subaccount

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import coil.compose.AsyncImage
import com.roomswap.app.data.model.JobStatus
import com.roomswap.app.data.repository.ReplacementRepository
import kotlinx.coroutines.delay

@Composable
fun ResultScreen(jobId: String, repository: ReplacementRepository = ReplacementRepository()) {
    var status by remember { mutableStateOf(JobStatus.PENDING) }
    var resultUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(jobId) {
        while (status == JobStatus.PENDING) {
            val job = repository.getJob(jobId)
            status = job.status
            resultUrl = job.resultImageUrl
            if (status == JobStatus.PENDING) delay(2000)
        }
    }

    Column {
        when (status) {
            JobStatus.PENDING -> CircularProgressIndicator()
            JobStatus.DONE -> resultUrl?.let { AsyncImage(model = it, contentDescription = "Result") }
            JobStatus.FAILED -> Text("Replacement failed. Please try again.")
        }
    }
}
