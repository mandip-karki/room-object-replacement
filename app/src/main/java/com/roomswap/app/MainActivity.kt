package com.roomswap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.roomswap.app.navigation.AppNavHost
import com.roomswap.app.ui.theme.RoomSwapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RoomSwapTheme {
                AppNavHost()
            }
        }
    }
}
