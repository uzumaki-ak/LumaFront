package com.edgelight.flashcam.ml

import android.content.Context
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * FaceDetector - Enhanced ML Kit face detection
 *
 * IMPROVEMENTS:
 * - Better ML Kit configuration for accuracy
 * - More reliable face tracking
 * - Smoother position updates
 * - Better fallback positioning
 */
class FaceDetector(
    private val lifecycleOwner: LifecycleOwner,
    private val context: Context,
    private val onFaceDetected: (Rect?) -> Unit
) {

    companion object {
        private const val TAG = "FaceDetector"
    }

    // Enhanced ML Kit detector with better accuracy
    private val detector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)  // CHANGED: Use accurate mode
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)  // CHANGED: Get all landmarks for better tracking
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)  // CHANGED: Get contours for precise positioning
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.1f)  // CHANGED: Detect smaller faces too
            .enableTracking()  // CHANGED: Enable tracking for smoother updates
            .build()

        FaceDetection.getClient(options)
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var isRunning = false

    // Smoothing - average last few face positions
    private val facePositionHistory = mutableListOf<Rect>()
    private val historySize = 3

    fun start() {
        if (isRunning) return
        isRunning = true

        Log.d(TAG, "Starting face detection...")
        startCamera()
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false

        Log.d(TAG, "Stopping face detection...")

        cameraProvider?.unbindAll()
        camera = null
        facePositionHistory.clear()

        // Send null to hide overlay
        onFaceDetected(null)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            } catch (e: Exception) {
                Log.e(TAG, "Camera initialization failed", e)
                // Use estimated position as fallback
                onFaceDetected(getEstimatedFacePosition())
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return

        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        // Enhanced image analysis with better quality
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(android.view.Surface.ROTATION_0)  // Proper rotation
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImageProxy(imageProxy)
                }
            }

        try {
            cameraProvider.unbindAll()

            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                imageAnalysis
            )

            Log.d(TAG, "Camera bound successfully for face detection")
        } catch (e: Exception) {
            Log.e(TAG, "Camera binding failed", e)
            onFaceDetected(getEstimatedFacePosition())
        }
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && isRunning) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            detector.process(image)
                .addOnSuccessListener { faces ->
                    handleDetectionResult(faces, imageProxy.width, imageProxy.height)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Face detection failed", e)
                    onFaceDetected(getEstimatedFacePosition())
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    /**
     * IMPROVED: Better face position handling with smoothing
     */
    private fun handleDetectionResult(faces: List<Face>, imageWidth: Int, imageHeight: Int) {
        if (faces.isNotEmpty()) {
            // Use the largest face (usually the person in front)
            val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() } ?: faces[0]

            // Convert camera coordinates to screen coordinates
            val screenRect = convertToScreenCoordinates(face.boundingBox, imageWidth, imageHeight)

            // Add to history for smoothing
            facePositionHistory.add(screenRect)
            if (facePositionHistory.size > historySize) {
                facePositionHistory.removeAt(0)
            }

            // Get smoothed position
            val smoothedRect = getSmoothedFacePosition()

            Log.d(TAG, "Face detected at: $smoothedRect")
            onFaceDetected(smoothedRect)
        } else {
            Log.d(TAG, "No face detected")
            onFaceDetected(getEstimatedFacePosition())
        }
    }

    /**
     * Converts camera image coordinates to screen coordinates
     */
    private fun convertToScreenCoordinates(rect: Rect, imageWidth: Int, imageHeight: Int): Rect {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Scale factor
        val scaleX = screenWidth.toFloat() / imageWidth.toFloat()
        val scaleY = screenHeight.toFloat() / imageHeight.toFloat()

        return Rect(
            (rect.left * scaleX).toInt(),
            (rect.top * scaleY).toInt(),
            (rect.right * scaleX).toInt(),
            (rect.bottom * scaleY).toInt()
        )
    }

    /**
     * Returns smoothed face position (average of recent positions)
     */
    private fun getSmoothedFacePosition(): Rect {
        if (facePositionHistory.isEmpty()) {
            return getEstimatedFacePosition()
        }

        var left = 0
        var top = 0
        var right = 0
        var bottom = 0

        for (rect in facePositionHistory) {
            left += rect.left
            top += rect.top
            right += rect.right
            bottom += rect.bottom
        }

        val count = facePositionHistory.size
        return Rect(
            left / count,
            top / count,
            right / count,
            bottom / count
        )
    }

    /**
     * Returns estimated face position (center-upper area of screen)
     * Used when no face detected
     */
    private fun getEstimatedFacePosition(): Rect {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Face typically in upper-center during video calls
        val faceHeight = (screenHeight * 0.35).toInt()
        val faceWidth = (faceHeight * 0.7).toInt()

        val left = (screenWidth - faceWidth) / 2
        val top = (screenHeight * 0.25).toInt()  // Upper portion of screen
        val right = left + faceWidth
        val bottom = top + faceHeight

        return Rect(left, top, right, bottom)
    }
}
