package com.roomswap.app

import android.app.Application
import com.google.firebase.FirebaseApp

class RoomSwapApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
