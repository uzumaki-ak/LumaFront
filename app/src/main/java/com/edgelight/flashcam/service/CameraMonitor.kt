package com.edgelight.flashcam.service

import android.content.Context
import android.hardware.camera2.CameraManager
import android.util.Log

/**
 * CameraMonitor - Detects camera usage
 *
 * PURPOSE: Know when ANY app uses the front camera
 * - Listens to camera availability changes
 * - Detects when front camera becomes active
 * - Notifies EdgeLightService to start/stop face detection
 *
 * HOW IT WORKS:
 * When an app uses camera, CameraManager fires availability callback
 * We check if it's the front camera and notify the service
 */
class CameraMonitor(
    private val context: Context,
    private val onCameraStateChanged: (Boolean) -> Unit
) {

    companion object {
        private const val TAG = "CameraMonitor"
    }

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var frontCameraId: String? = null
    private var isMonitoring = false

    /**
     * Camera availability callback
     * Fired when camera becomes available/unavailable
     */
    private val availabilityCallback = object : CameraManager.AvailabilityCallback() {

        override fun onCameraAvailable(cameraId: String) {
            // Camera became available (no app is using it)
            if (cameraId == frontCameraId) {
                Log.d(TAG, "Front camera available (not in use)")
                onCameraStateChanged(false)
            }
        }

        override fun onCameraUnavailable(cameraId: String) {
            // Camera became unavailable (some app is using it)
            if (cameraId == frontCameraId) {
                Log.d(TAG, "Front camera unavailable (in use)")
                onCameraStateChanged(true)
            }
        }
    }

    /**
     * Starts monitoring camera availability
     */
    fun startMonitoring() {
        if (isMonitoring) return

        // Find front camera ID
        findFrontCamera()

        // Register callback
        cameraManager.registerAvailabilityCallback(availabilityCallback, null)
        isMonitoring = true

        Log.d(TAG, "Started monitoring front camera: $frontCameraId")
    }

    /**
     * Stops monitoring
     */
    fun stopMonitoring() {
        if (!isMonitoring) return

        cameraManager.unregisterAvailabilityCallback(availabilityCallback)
        isMonitoring = false

        Log.d(TAG, "Stopped monitoring")
    }

    /**
     * Finds the front-facing camera ID
     */
    private fun findFrontCamera() {
        try {
            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)

                // Check if it's front camera
                if (facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT) {
                    frontCameraId = cameraId
                    Log.d(TAG, "Found front camera: $cameraId")
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding front camera", e)
        }
    }
}