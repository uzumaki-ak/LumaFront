package com.edgelight.flashcam.ui

import android.content.Context
import android.graphics.*
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.Gravity
import com.edgelight.flashcam.utils.PreferencesManager
import kotlinx.coroutines.*
import android.graphics.Rect as AndroidRect

/**
 * OverlayView - FIXED MacBook-style with proper center dimming
 *
 * FIXES:
 * - Proper center dimming (solid black, not fade)
 * - MAX brightness (255 alpha everywhere)
 * - Color customization support
 * - Better cleanup
 */
class OverlayView(private val context: Context) : View(context) {

    companion object {
        private const val TAG = "OverlayView"

        // FIXED: Max brightness settings
        private const val EDGE_BRIGHTNESS = 255  // MAX (was too low before)
        private const val EDGE_GLOW_WIDTH = 250  // Wider glow
        private const val CORNER_RADIUS = 80f

        // FIXED: Proper center dimming
        private const val CENTER_DIM_ALPHA = 120  // Darker center (not fade)
        private const val CENTER_DIM_COLOR = 0xFF000000.toInt()  // Pure black
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val preferencesManager = PreferencesManager(context)
    private val overlayScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var isVisible = false
    private var faceRect: AndroidRect? = null
    private var currentColor = 0xFFFFF4E6.toInt()  // Default warm white

    private var screenWidth = 0
    private var screenHeight = 0

    // Paints
    private val edgeGlowPaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.FILL
    }

    private val centerDimPaint = Paint().apply {
        color = CENTER_DIM_COLOR
        alpha = CENTER_DIM_ALPHA
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)

        val displayMetrics = context.resources.displayMetrics
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels

        // Load color preference
        overlayScope.launch {
            preferencesManager.glowColorFlow.collect { color ->
                currentColor = color
                invalidate()
            }
        }
    }

    fun show() {
        if (isVisible) {
            Log.w(TAG, "Already visible")
            return
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START

        try {
            windowManager.addView(this, params)
            isVisible = true
            Log.d(TAG, "Overlay shown")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay", e)
        }
    }

    /**
     * FIXED: Better cleanup with proper view removal
     */
    fun hide() {
        if (!isVisible) {
            Log.w(TAG, "Already hidden")
            return
        }

        try {
            windowManager.removeView(this)
            isVisible = false
            faceRect = null
            Log.d(TAG, "Overlay hidden")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hide overlay", e)
            // Force reset state even if removal failed
            isVisible = false
        }
    }

    fun updateFacePosition(rect: AndroidRect?) {
        faceRect = rect
        invalidate()
    }

    /**
     * FIXED: Proper layered rendering
     *
     * Layer 1: Bright edge glow (full brightness)
     * Layer 2: Solid black dimming in center (not gradient!)
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Layer 1: Edge glow (MAX brightness on edges)
        drawEdgeGlow(canvas)

        // Layer 2: Center dimming (solid black overlay)
        drawCenterDim(canvas)
    }

    /**
     * FIXED: Edge glow with MAX brightness
     */
    private fun drawEdgeGlow(canvas: Canvas) {
        val fullRect = RectF(0f, 0f, width.toFloat(), height.toFloat())

        // Create shader with MAX brightness
        val edgeColor = adjustColorAlpha(currentColor, EDGE_BRIGHTNESS)

        // Radial gradient from center (transparent) to edges (max bright)
        val gradient = RadialGradient(
            width / 2f,
            height / 2f,
            kotlin.math.max(width, height) / 1.5f,
            intArrayOf(
                Color.TRANSPARENT,
                adjustColorAlpha(currentColor, EDGE_BRIGHTNESS / 2),
                adjustColorAlpha(currentColor, EDGE_BRIGHTNESS)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )

        edgeGlowPaint.shader = gradient
        canvas.drawRoundRect(fullRect, CORNER_RADIUS, CORNER_RADIUS, edgeGlowPaint)
    }

    /**
     * FIXED: Proper center dimming (solid black, not gradient)
     */
    private fun drawCenterDim(canvas: Canvas) {
        // Calculate center area to dim
        val dimPadding = EDGE_GLOW_WIDTH.toFloat()
        val centerRect = RectF(
            dimPadding,
            dimPadding,
            width - dimPadding,
            height - dimPadding
        )

        // Draw SOLID black overlay (this is the fix!)
        canvas.drawRoundRect(
            centerRect,
            CORNER_RADIUS / 2,
            CORNER_RADIUS / 2,
            centerDimPaint
        )
    }

    private fun adjustColorAlpha(color: Int, alpha: Int): Int {
        return Color.argb(
            alpha,
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }

    /**
     * Cleanup on detach
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        overlayScope.cancel()
    }
}