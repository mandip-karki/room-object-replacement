package com.roomswap.app.ui.subaccount

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun RoomPhotoScreen(
    onPhotoTapped: (photoUrl: String, tapX: Double, tapY: Double, tappedRegionImageUrl: String) -> Unit,
) {
    // TODO: camera/gallery picker (ActivityResultContracts.PickVisualMedia), upload the full
    // photo to Supabase Storage `room-photos/{uid}/...`, show it so the user can tap the item
    // to replace, then crop a small region around the tap and upload that too — the `replace`
    // Edge Function captions this crop (e.g. "a hardwood floor") since there's no free
    // point-prompted segmentation API to isolate the tapped object precisely.
    Column {
        Text("Take or choose a room photo")
        Button(onClick = { /* placeholder */ }) {
            Text("Take photo")
        }
    }
}
