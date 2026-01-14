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
import kotlinx.coroutines.*

/**
 * FaceDetector - ML Kit face detection
 *
 * PURPOSE: Track face position in real-time
 * - Uses Google ML Kit (FREE, no API key needed)
 * - Runs on-device (no internet required)
 * - Detects face bounding box
 * - Sends position updates to overlay
 *
 * HOW IT WORKS:
 * 1. Opens camera in background (low priority)
 * 2. Feeds frames to ML Kit
 * 3. ML Kit returns face position
 * 4. We send position to OverlayView
 *
 * FIXED: Now properly accepts LifecycleOwner instead of casting context
 */
class FaceDetector(
    private val lifecycleOwner: LifecycleOwner,  // <-- CHANGED: Now explicitly require LifecycleOwner
    private val context: Context,
    private val onFaceDetected: (Rect?) -> Unit
) {

    companion object {
        private const val TAG = "FaceDetector"
    }

    // ML Kit face detector
    private val detector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)  // Fast mode for real-time
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)  // We don't need landmarks
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)  // Don't need smile/eyes detection
            .setMinFaceSize(0.15f)  // Minimum face size (15% of image)
            .build()

        FaceDetection.getClient(options)
    }

    // Camera components
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var isRunning = false
    private val detectionScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Starts face detection
     * Opens camera and begins processing frames
     */
    fun start() {
        if (isRunning) return
        isRunning = true

        Log.d(TAG, "Starting face detection...")
        startCamera()
    }

    /**
     * Stops face detection
     * Releases camera and ML Kit resources
     */
    fun stop() {
        if (!isRunning) return
        isRunning = false

        Log.d(TAG, "Stopping face detection...")

        // Release camera
        cameraProvider?.unbindAll()
        camera = null

        // Cancel any ongoing detection
        detectionScope.cancel()
    }

    /**
     * Initializes and starts camera
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            } catch (e: Exception) {
                Log.e(TAG, "Camera initialization failed", e)
                // Fall back to estimated position if camera fails
                onFaceDetected(getEstimatedFacePosition())
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Binds camera use cases for face detection
     */
    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return

        // Select front camera
        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        // Image analysis for face detection
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)  // Drop old frames
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImageProxy(imageProxy)
                }
            }

        try {
            // Unbind any existing use cases
            cameraProvider.unbindAll()

            // Bind camera to lifecycle
            // FIXED: Now using the proper lifecycleOwner parameter instead of casting context
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,  // <-- CHANGED: Use the proper LifecycleOwner we got in constructor
                cameraSelector,
                imageAnalysis
            )

            Log.d(TAG, "Camera bound successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Camera binding failed: ${e.message}", e)
            // Use estimated position as fallback
            onFaceDetected(getEstimatedFacePosition())
        }
    }

    /**
     * Processes camera frame for face detection
     */
    @androidx.camera.core.ExperimentalGetImage
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            // Run face detection
            detector.process(image)
                .addOnSuccessListener { faces ->
                    handleDetectionResult(faces)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Face detection failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()  // Always close the image
                }
        } else {
            imageProxy.close()
        }
    }

    /**
     * Handles face detection results
     */
    private fun handleDetectionResult(faces: List<Face>) {
        if (faces.isNotEmpty()) {
            // Use the first (largest) face detected
            val face = faces[0]
            val boundingBox = face.boundingBox

            Log.d(TAG, "Face detected at: $boundingBox")
            onFaceDetected(boundingBox)
        } else {
            // No face detected - use estimated position
            Log.d(TAG, "No face detected, using estimated position")
            onFaceDetected(getEstimatedFacePosition())
        }
    }

    /**
     * Returns estimated face position (center of screen)
     * Used as fallback when camera access fails or no face detected
     */
    private fun getEstimatedFacePosition(): Rect {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Assume face is centered, roughly 30% of screen height
        val faceHeight = (screenHeight * 0.3).toInt()
        val faceWidth = (faceHeight * 0.75).toInt()  // 3:4 aspect ratio

        val left = (screenWidth - faceWidth) / 2
        val top = (screenHeight - faceHeight) / 2
        val right = left + faceWidth
        val bottom = top + faceHeight

        return Rect(left, top, right, bottom)
    }
}