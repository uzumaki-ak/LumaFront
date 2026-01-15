package com.edgelight.flashcam.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.edgelight.flashcam.EdgeLightApp
import com.edgelight.flashcam.MainActivity
import com.edgelight.flashcam.R
import com.edgelight.flashcam.ml.FaceDetector
import com.edgelight.flashcam.ui.OverlayView
import kotlinx.coroutines.*

/**
 * EdgeLightService - Main foreground service
 *
 * FIXED ISSUES:
 * - Now only shows overlay when camera is ACTUALLY active
 * - Properly waits for camera to be in use before starting detection
 * - Cleaner state management
 */
class EdgeLightService : LifecycleService() {

    companion object {
        private const val TAG = "EdgeLightService"
        private const val SERVICE_ID = 1001
    }

    private lateinit var cameraMonitor: CameraMonitor
    private lateinit var faceDetector: FaceDetector
    private lateinit var overlayView: OverlayView

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Track if we're currently active (camera in use)
    private var isActive = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        initializeComponents()
        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(SERVICE_ID, createNotification())
        Log.d(TAG, "Service started in foreground")
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        stopMonitoring()
        serviceScope.cancel()
    }

    private fun initializeComponents() {
        // Camera monitor
        cameraMonitor = CameraMonitor(this) { isCameraActive ->
            handleCameraStateChange(isCameraActive)
        }

        // Face detector - pass lifecycle owner properly
        faceDetector = FaceDetector(
            lifecycleOwner = this,
            context = this
        ) { facePosition ->
            overlayView.updateFacePosition(facePosition)
        }

        // Overlay view - redesigned to match MacBook style
        overlayView = OverlayView(this)
    }

    private fun startMonitoring() {
        cameraMonitor.startMonitoring()
        Log.d(TAG, "Started monitoring camera")
    }

    private fun stopMonitoring() {
        cameraMonitor.stopMonitoring()
        if (isActive) {
            deactivate()
        }
    }

    /**
     * FIXED: Better state management
     * Only activates when camera is ACTUALLY in use
     */
    private fun handleCameraStateChange(isCameraActive: Boolean) {
        Log.d(TAG, "Camera state changed: $isCameraActive")

        if (isCameraActive && !isActive) {
            // Camera just became active
            activate()
        } else if (!isCameraActive && isActive) {
            // Camera just became inactive
            deactivate()
        }
    }

    /**
     * Activates EdgeLight (camera in use)
     */
    private fun activate() {
        Log.d(TAG, "Activating EdgeLight")
        isActive = true
        faceDetector.start()
        overlayView.show()
    }

    /**
     * Deactivates EdgeLight (camera not in use)
     */
    private fun deactivate() {
        Log.d(TAG, "Deactivating EdgeLight")
        isActive = false
        faceDetector.stop()
        overlayView.hide()
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, EdgeLightApp.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("EdgeLight Running")
            .setContentText("Waiting for camera...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
