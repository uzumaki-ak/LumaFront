package com.edgelight.flashcam.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
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
 * EdgeLightService
 *
 * FIXED:
 * - Renamed isActive to isEdgeLightActive to avoid conflict with CoroutineScope.isActive
 * - Better cleanup logic
 * - Auto brightness with proper state management
 */
class EdgeLightService : LifecycleService() {

    companion object {
        private const val TAG = "EdgeLightService"
        private const val SERVICE_ID = 1001
        private const val CLEANUP_TIMEOUT = 2000L
    }

    private lateinit var cameraMonitor: CameraMonitor
    private lateinit var faceDetector: FaceDetector
    private lateinit var overlayView: OverlayView

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var isEdgeLightActive = false
    private var originalBrightness: Int = -1
    private var cleanupJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        initializeComponents()
        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(SERVICE_ID, createNotification())
        Log.d(TAG, "Service started")
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")

        if (isEdgeLightActive) {
            forceDeactivate()
        }

        stopMonitoring()
        serviceScope.cancel()
    }

    private fun initializeComponents() {
        cameraMonitor = CameraMonitor(this) { isCameraActive ->
            handleCameraStateChange(isCameraActive)
        }

        faceDetector = FaceDetector(
            lifecycleOwner = this,
            context = this
        ) { facePosition ->
            overlayView.updateFacePosition(facePosition)
        }

        overlayView = OverlayView(this)
    }

    private fun startMonitoring() {
        cameraMonitor.startMonitoring()
        Log.d(TAG, "Monitoring started")
    }

    private fun stopMonitoring() {
        cameraMonitor.stopMonitoring()
    }

    private fun handleCameraStateChange(isCameraActive: Boolean) {
        Log.d(TAG, "Camera state: $isCameraActive, Currently active: $isEdgeLightActive")

        cleanupJob?.cancel()

        if (isCameraActive && !isEdgeLightActive) {
            activate()
        } else if (!isCameraActive && isEdgeLightActive) {
            cleanupJob = serviceScope.launch {
                delay(CLEANUP_TIMEOUT)
                if (isEdgeLightActive) {
                    Log.d(TAG, "Cleanup timeout reached, forcing deactivation")
                    deactivate()
                }
            }
        }
    }

    private fun activate() {
        Log.d(TAG, "Activating EdgeLight")
        isEdgeLightActive = true

        setMaxBrightness()
        faceDetector.start()
        overlayView.show()

        // Update notification
        updateNotification("Lighting active")
    }

    private fun deactivate() {
        Log.d(TAG, "Deactivating EdgeLight")

        faceDetector.stop()

        serviceScope.launch {
            delay(200)
            overlayView.hide()
            restoreOriginalBrightness()
            isEdgeLightActive = false

            // Update notification
            updateNotification("Waiting for camera...")

            Log.d(TAG, "Deactivation complete")
        }
    }

    private fun forceDeactivate() {
        Log.d(TAG, "Force deactivating")
        faceDetector.stop()
        overlayView.hide()
        restoreOriginalBrightness()
        isEdgeLightActive = false
    }

    /**
     * Auto max brightness when active
     */
    private fun setMaxBrightness() {
        try {
            originalBrightness = Settings.System.getInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            )

            if (Settings.System.canWrite(this)) {
                Settings.System.putInt(
                    contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    255
                )
                Log.d(TAG, "Brightness set to max (255)")
            } else {
                Log.w(TAG, "Cannot write system settings - brightness not changed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set brightness", e)
        }
    }

    /**
     * Restore original brightness
     */
    private fun restoreOriginalBrightness() {
        try {
            if (originalBrightness != -1 && Settings.System.canWrite(this)) {
                Settings.System.putInt(
                    contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    originalBrightness
                )
                Log.d(TAG, "Brightness restored to $originalBrightness")
                originalBrightness = -1
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore brightness", e)
        }
    }

    /**
     * Creates initial notification
     */
    private fun createNotification(): Notification {
        return buildNotification("Waiting for camera...")
    }

    /**
     * Updates notification text
     */
    private fun updateNotification(text: String) {
        val notification = buildNotification(text)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(SERVICE_ID, notification)
    }

    /**
     * Builds notification with given text
     */
    private fun buildNotification(text: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, EdgeLightApp.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("EdgeLight Active")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
