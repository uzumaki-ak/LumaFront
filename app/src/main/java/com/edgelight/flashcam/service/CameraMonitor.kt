package com.edgelight.flashcam.service

import android.content.Context
import android.hardware.camera2.CameraManager
import android.util.Log

/**
 * CameraMonitor - Detects camera usage
 *
 * IMPROVED:
 * - More reliable camera detection
 * - Better logging
 * - Handles edge cases
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
    private var lastCameraState = false  // Track last state to avoid duplicate calls

    private val availabilityCallback = object : CameraManager.AvailabilityCallback() {

        override fun onCameraAvailable(cameraId: String) {
            if (cameraId == frontCameraId) {
                Log.d(TAG, "Front camera available (not in use)")
                updateCameraState(false)
            }
        }

        override fun onCameraUnavailable(cameraId: String) {
            if (cameraId == frontCameraId) {
                Log.d(TAG, "Front camera unavailable (in use!)")
                updateCameraState(true)
            }
        }
    }

    fun startMonitoring() {
        if (isMonitoring) return

        findFrontCamera()

        if (frontCameraId != null) {
            cameraManager.registerAvailabilityCallback(availabilityCallback, null)
            isMonitoring = true
            Log.d(TAG, "Started monitoring front camera: $frontCameraId")
        } else {
            Log.e(TAG, "Could not find front camera!")
        }
    }

    fun stopMonitoring() {
        if (!isMonitoring) return

        cameraManager.unregisterAvailabilityCallback(availabilityCallback)
        isMonitoring = false
        Log.d(TAG, "Stopped monitoring")
    }

    /**
     * Updates camera state, avoiding duplicate notifications
     */
    private fun updateCameraState(isActive: Boolean) {
        if (isActive != lastCameraState) {
            lastCameraState = isActive
            onCameraStateChanged(isActive)
        }
    }

    private fun findFrontCamera() {
        try {
            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)

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