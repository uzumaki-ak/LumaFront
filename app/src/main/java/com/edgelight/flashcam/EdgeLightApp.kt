package com.edgelight.flashcam

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

/**
 * EdgeLightApp - Application class
 *
 * PURPOSE: Initialize app-wide components
 * - Creates notification channel for foreground service
 * - Sets up any global configurations
 *
 * This runs ONCE when the app starts
 */
class EdgeLightApp : Application() {

    companion object {
        // Notification channel ID for our foreground service
        const val NOTIFICATION_CHANNEL_ID = "edgelight_service_channel"
        const val NOTIFICATION_CHANNEL_NAME = "EdgeLight Service"
    }

    override fun onCreate() {
        super.onCreate()

        // Create notification channel for Android 8.0+ (required for foreground services)
        createNotificationChannel()
    }

    /**
     * Creates notification channel for the foreground service
     * Required for Android 8.0+ to show service notifications
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW  // Low importance = no sound
            ).apply {
                description = "Keeps EdgeLight running in background"
                setShowBadge(false)  // Don't show badge on app icon
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}