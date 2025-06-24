package com.me.gemini

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import androidx.work.Configuration

@HiltAndroidApp
class MyApp : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        ensureWorkManagerInitialization()
    }

    private fun ensureWorkManagerInitialization() {
        try {
            // Force early initialization
            WorkManager.getInstance(this)
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Log.e("AppInit", "WorkManager init failed on Android 12+", e)
                // Fallback initialization
                WorkManager.initialize(this, workManagerConfiguration)
            }
        }
    }
}