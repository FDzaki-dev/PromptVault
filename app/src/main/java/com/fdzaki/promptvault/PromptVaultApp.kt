package com.fdzaki.promptvault

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class PromptVaultApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PromptVault Sorting",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for automatic Downloads sorting"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "promptvault_sorting_channel"
    }
}
