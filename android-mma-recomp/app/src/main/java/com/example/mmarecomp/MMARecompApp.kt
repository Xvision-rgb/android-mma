package com.example.mmarecomp

import android.app.Application
import com.example.mmarecomp.data.offline.OfflineCoordinator

class MMARecompApp : Application() {
    override fun onCreate() {
        super.onCreate()
        OfflineCoordinator.get(this)
    }
}
