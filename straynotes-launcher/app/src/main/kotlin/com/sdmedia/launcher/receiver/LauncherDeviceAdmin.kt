package com.sdmedia.launcher.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class LauncherDeviceAdmin : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        // Device admin enabled — double-tap-to-lock is now available
    }

    override fun onDisabled(context: Context, intent: Intent) {
        // Device admin disabled — double-tap-to-lock no longer functional
    }
}
