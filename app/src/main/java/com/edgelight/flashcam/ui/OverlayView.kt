package com.edgelight.flashcam.ui

import android.content.Context
import android.graphics.*
import android.view.View
import android.view.WindowManager
import android.view.Gravity
import android.graphics.Rect as AndroidRect

/**
 * OverlayView - The magic glow overlay
 *
 * PURPOSE: Draws the edge light glow on screen
 * - Appears on top of ALL apps (WhatsApp, Telegram, etc.)
 * - Follows face position
 * - Looks like real studio lighting
 *
 * DESIGN:
 * - Soft rounded rectangle glow around face
 * - Warm white color (not pure white)
 * - Smooth gradients (no harsh edges)
 * - Mimics Apple's MacBook Edge Light
 */
class OverlayView(private val context: Context) : View(context) {

    companion object {
        private const val GLOW_COLOR = 0xFFFFF8E1.toInt()  // Warm white
        private const val GLOW_ALPHA = 180  // Semi-transparent
        private const val GLOW_BLUR_RADIUS = 100f  // Blur amount
        private const val GLOW_PADDING = 150  // Space around face
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var isVisible = false

    // Face position (updated by FaceDetector)
    private var faceRect: AndroidRect? = null

    // Paint for drawing glow
    private val glowPaint = Paint().apply {
        color = GLOW_COLOR
        alpha = GLOW_ALPHA
        style = Paint.Style.FILL
        isAntiAlias = true
        // Blur effect for soft glow
        maskFilter = BlurMaskFilter(GLOW_BLUR_RADIUS, BlurMaskFilter.Blur.NORMAL)
    }

    init {
        // Enable software rendering for blur effects
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    /**
     * Shows the overlay on screen
     */
    fun show() {
        if (isVisible) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,  // Draw on top of everything
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or  // Don't steal focus
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or  // Don't intercept touches
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,  // Allow drawing outside bounds
            PixelFormat.TRANSLUCENT  // Support transparency
        )

        params.gravity = Gravity.TOP or Gravity.START

        try {
            windowManager.addView(this, params)
            isVisible = true
        } catch (e: Exception) {
            // Permission not granted yet
        }
    }

    /**
     * Hides the overlay
     */
    fun hide() {
        if (!isVisible) return

        try {
            windowManager.removeView(this)
            isVisible = false
        } catch (e: Exception) {
            // Already removed
        }
    }

    /**
     * Updates face position and redraws
     * Called by FaceDetector when face moves
     */
    fun updateFacePosition(rect: AndroidRect?) {
        faceRect = rect
        invalidate()  // Trigger redraw
    }

    /**
     * Draws the glow on screen
     * This is where the magic happens!
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val face = faceRect ?: return

        // Calculate glow rectangle (face + padding)
        val glowRect = RectF(
            (face.left - GLOW_PADDING).toFloat(),
            (face.top - GLOW_PADDING).toFloat(),
            (face.right + GLOW_PADDING).toFloat(),
            (face.bottom + GLOW_PADDING).toFloat()
        )

        // Calculate corner radius (makes it look more natural)
        val cornerRadius = GLOW_PADDING.toFloat()

        // Draw the glowing rounded rectangle
        canvas.drawRoundRect(glowRect, cornerRadius, cornerRadius, glowPaint)
    }
}
