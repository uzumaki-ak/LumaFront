package com.edgelight.flashcam.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService  // IMPORTANT: Use LifecycleService not Service
import com.edgelight.flashcam.EdgeLightApp
import com.edgelight.flashcam.MainActivity
import com.edgelight.flashcam.R
import com.edgelight.flashcam.ml.FaceDetector
import com.edgelight.flashcam.ui.OverlayView
import kotlinx.coroutines.*

/**
 * EdgeLightService - Main foreground service
 *
 * PURPOSE: The brain of EdgeLight
 * - Monitors camera usage by other apps
 * - Runs face detection
 * - Controls overlay display
 * - Runs 24/7 in background
 *
 * IMPORTANT: Extends LifecycleService (not Service) because FaceDetector needs it
 *
 * This is what makes EdgeLight work system-wide
 */
class EdgeLightService : LifecycleService() {  // <-- CHANGED: Was Service(), now LifecycleService()

    companion object {
        private const val SERVICE_ID = 1001
    }

    // Core components
    private lateinit var cameraMonitor: CameraMonitor
    private lateinit var faceDetector: FaceDetector
    private lateinit var overlayView: OverlayView

    // Coroutine scope for async operations
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()

        // Initialize components
        initializeComponents()

        // Start monitoring
        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)  // IMPORTANT: Call super for LifecycleService

        // Show notification (required for foreground service)
        startForeground(SERVICE_ID, createNotification())

        return START_STICKY  // Restart service if killed by system
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)  // IMPORTANT: Call super for LifecycleService
        return null  // We don't need binding
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clean up everything
        stopMonitoring()
        serviceScope.cancel()
    }

    /**
     * Initializes all EdgeLight components
     */
    private fun initializeComponents() {
        // Camera monitor - detects when camera is in use
        cameraMonitor = CameraMonitor(this) { isCameraActive ->
            handleCameraStateChange(isCameraActive)
        }

        // Face detector - tracks face position
        // IMPORTANT: Pass 'this' (the LifecycleService) not just context
        faceDetector = FaceDetector(
            lifecycleOwner = this,  // <-- CHANGED: Now passing LifecycleOwner properly
            context = this
        ) { facePosition ->
            // Update overlay with new face position
            overlayView.updateFacePosition(facePosition)
        }

        // Overlay view - draws the glow on screen
        overlayView = OverlayView(this)
    }

    /**
     * Starts monitoring for camera usage
     */
    private fun startMonitoring() {
        cameraMonitor.startMonitoring()
    }

    /**
     * Stops monitoring and cleans up
     */
    private fun stopMonitoring() {
        cameraMonitor.stopMonitoring()
        faceDetector.stop()
        overlayView.hide()
    }

    /**
     * Handles camera state changes (active/inactive)
     *
     * @param isActive true if camera is being used by any app
     */
    private fun handleCameraStateChange(isActive: Boolean) {
        if (isActive) {
            // Camera is active - start face detection and show overlay
            faceDetector.start()
            overlayView.show()
        } else {
            // Camera inactive - stop face detection and hide overlay
            faceDetector.stop()
            overlayView.hide()
        }
    }

    /**
     * Creates notification for foreground service
     * Required by Android for services that run in background
     */
    private fun createNotification(): Notification {
        // Intent to open app when notification is tapped
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, EdgeLightApp.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("EdgeLight Active")
            .setContentText("Face-tracking flash is running")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)  // Can't be dismissed by user
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}