package com.roomswap.app.ui.subaccount

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun RoomPhotoScreen(onPhotoTapped: (photoUrl: String, tapX: Double, tapY: Double) -> Unit) {
    // TODO: camera/gallery picker (ActivityResultContracts.PickVisualMedia), upload to
    // Firebase Storage, then show the photo and capture the user's tap coordinates.
    Column {
        Text("Take or choose a room photo")
        Button(onClick = { /* placeholder */ }) {
            Text("Take photo")
        }
    }
}
