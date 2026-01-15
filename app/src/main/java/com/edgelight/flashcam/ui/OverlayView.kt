package com.edgelight.flashcam.ui

import android.content.Context
import android.graphics.*
import android.view.View
import android.view.WindowManager
import android.view.Gravity
import android.graphics.Rect as AndroidRect
import kotlin.math.max

/**
 * OverlayView - MacBook-style Edge Light
 *
 * COMPLETE REDESIGN TO MATCH MACBOOK:
 * - Full screen edge glow with rounded corners
 * - Dimmed center (dark overlay in middle)
 * - Bright warm glow on edges only
 * - Face-aware brightness adjustment
 *
 * This is what makes it look professional like MacBook!
 */
class OverlayView(private val context: Context) : View(context) {

    companion object {
        private const val TAG = "OverlayView"

        // MacBook-style colors
        private const val EDGE_GLOW_COLOR = 0xFFFFF4E6.toInt()  // Warm white/cream
        private const val CENTER_DIM_COLOR = 0x50000000  // Semi-transparent black

        // Glow settings
        private const val EDGE_GLOW_WIDTH = 200  // How far the glow extends inward
        private const val CORNER_RADIUS = 80f  // Rounded corners like MacBook
        private const val EDGE_BRIGHTNESS = 255  // Max brightness on edges

        // Center dimming
        private const val CENTER_DIM_ALPHA = 80  // How dark the center is
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var isVisible = false

    private var faceRect: AndroidRect? = null
    private var screenWidth = 0
    private var screenHeight = 0

    // Paints for different layers
    private val edgeGlowPaint = Paint().apply {
        isAntiAlias = true
        isDither = true
    }

    private val centerDimPaint = Paint().apply {
        color = CENTER_DIM_COLOR
        alpha = CENTER_DIM_ALPHA
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    init {
        // Enable hardware acceleration for smooth rendering
        setLayerType(LAYER_TYPE_HARDWARE, null)

        // Get screen dimensions
        val displayMetrics = context.resources.displayMetrics
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
    }

    fun show() {
        if (isVisible) return

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
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to show overlay", e)
        }
    }

    fun hide() {
        if (!isVisible) return

        try {
            windowManager.removeView(this)
            isVisible = false
        } catch (e: Exception) {
            // Already removed
        }
    }

    fun updateFacePosition(rect: AndroidRect?) {
        faceRect = rect
        invalidate()
    }

    /**
     * MAIN DRAWING - MacBook Style!
     *
     * Drawing order:
     * 1. Edge glow (bright warm light on edges)
     * 2. Center dimming (dark overlay in center)
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw edge glow
        drawEdgeGlow(canvas)

        // Draw center dimming
        drawCenterDim(canvas)
    }

    /**
     * Draws the edge glow effect (MacBook style)
     *
     * Creates a radial gradient from edges inward
     * Brighter on edges, fades to transparent toward center
     */
    private fun drawEdgeGlow(canvas: Canvas) {
        // Create full screen rectangle with rounded corners
        val fullScreenRect = RectF(0f, 0f, width.toFloat(), height.toFloat())

        // Create radial gradient shader for edge glow
        val gradient = RadialGradient(
            width / 2f,  // Center X
            height / 2f,  // Center Y
            max(width, height) / 1.5f,  // Radius
            intArrayOf(
                Color.TRANSPARENT,  // Transparent in center
                adjustColorAlpha(EDGE_GLOW_COLOR, 180),  // Semi-bright mid
                adjustColorAlpha(EDGE_GLOW_COLOR, EDGE_BRIGHTNESS)  // Full bright on edges
            ),
            floatArrayOf(0f, 0.6f, 1f),  // Gradient stops
            Shader.TileMode.CLAMP
        )

        edgeGlowPaint.shader = gradient

        // Draw the glowing rounded rectangle
        canvas.drawRoundRect(fullScreenRect, CORNER_RADIUS, CORNER_RADIUS, edgeGlowPaint)
    }

    /**
     * Draws center dimming effect
     *
     * Darkens the center of screen to make edges appear brighter
     * This is KEY to MacBook's effect!
     */
    private fun drawCenterDim(canvas: Canvas) {
        // Calculate center dimming area (smaller than full screen)
        val dimPadding = EDGE_GLOW_WIDTH.toFloat()
        val dimRect = RectF(
            dimPadding,
            dimPadding,
            width - dimPadding,
            height - dimPadding
        )

        // Draw semi-transparent dark overlay in center
        canvas.drawRoundRect(
            dimRect,
            CORNER_RADIUS / 2,
            CORNER_RADIUS / 2,
            centerDimPaint
        )
    }

    /**
     * Helper: Adjust color alpha
     */
    private fun adjustColorAlpha(color: Int, alpha: Int): Int {
        return Color.argb(
            alpha,
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }
}
