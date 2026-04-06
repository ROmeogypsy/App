package com.sdmedia.launcher

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LauncherApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Hilt initializes all singletons here
    }
}
