package com.gaurav.jarvis

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

/** Animated circular voice visualizer. Only animates while JARVIS is active, to save battery. */
class VisualizerView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {
    enum class Mode { IDLE, LISTENING, THINKING, SPEAKING }

    var mode: Mode = Mode.IDLE
        set(v) { field = v; invalidate() }
    var level: Float = 0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private var smooth = 0f
    private val oval = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val t = System.currentTimeMillis() / 1000f
        val cx = width / 2f
        val cy = height / 2f
        val base = min(width, height) * 0.26f
        val target = when (mode) {
            Mode.LISTENING -> 0.15f + level * 0.85f
            Mode.SPEAKING -> 0.35f + 0.3f * abs(sin(t * 6f))
            Mode.THINKING -> 0.2f
            Mode.IDLE -> 0.05f
        }
        smooth += (target - smooth) * 0.15f
        for (i in 0..3) {
            val r = base + i * 14f * (1f + smooth * 1.5f) + smooth * 24f * sin(t * 2f + i)
            paint.color = Color.argb((210 - i * 50).coerceAtLeast(30), 0, 229, 255)
            canvas.drawCircle(cx, cy, r, paint)
        }
        if (mode == Mode.THINKING) {
            val r = base + 70f
            oval.set(cx - r, cy - r, cx + r, cy + r)
            paint.color = Color.argb(255, 0, 229, 255)
            canvas.drawArc(oval, (t * 240f) % 360f, 90f, false, paint)
        }
        if (mode != Mode.IDLE || smooth > 0.06f) postInvalidateOnAnimation()
    }
}
