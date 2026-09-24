package com.example.eleven

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class BoostDialView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var boostPercent: Int = 125
        set(value) {
            field = value.coerceIn(100, 200)
            invalidate()
        }

    var active: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    private val yellow = Color.rgb(255, 214, 0)
    private val dimYellow = Color.rgb(95, 80, 0)
    private val panel = Color.rgb(16, 16, 16)
    private val white = Color.rgb(245, 245, 245)
    private val muted = Color.rgb(120, 120, 120)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arcRect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f + 8f
        val radius = min(w, h) * 0.35f

        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        canvas.drawCircle(cx, cy, radius + 34f, paint)

        paint.color = panel
        canvas.drawCircle(cx, cy, radius + 18f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = 16f
        paint.color = Color.rgb(38, 38, 38)
        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(arcRect, 135f, 270f, false, paint)

        val progress = (boostPercent - 100) / 100f
        paint.color = if (active) yellow else dimYellow
        paint.strokeWidth = 16f
        canvas.drawArc(arcRect, 135f, 270f * progress, false, paint)

        paint.strokeWidth = 3f
        repeat(25) { i ->
            val f = i / 24f
            val angleDeg = 135f + 270f * f
            val angle = Math.toRadians(angleDeg.toDouble())
            val outer = radius + 34f
            val inner = if (i % 6 == 0) radius + 21f else radius + 26f
            val x1 = cx + (cos(angle) * inner).toFloat()
            val y1 = cy + (sin(angle) * inner).toFloat()
            val x2 = cx + (cos(angle) * outer).toFloat()
            val y2 = cy + (sin(angle) * outer).toFloat()
            paint.color = if (f <= progress && active) yellow else Color.rgb(65, 65, 65)
            canvas.drawLine(x1, y1, x2, y2, paint)
        }

        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD)
        paint.textSize = 54f
        paint.color = if (active) yellow else white
        canvas.drawText("$boostPercent%", cx, cy - 4f, paint)

        val gainDb = GlobalBoostEngine.percentToDb(boostPercent)
        paint.typeface = android.graphics.Typeface.create("monospace", android.graphics.Typeface.NORMAL)
        paint.textSize = 18f
        paint.color = muted
        canvas.drawText(String.format("%+.1f dB", gainDb), cx, cy + 30f, paint)

        paint.textSize = 14f
        paint.color = if (active) yellow else muted
        canvas.drawText(if (active) "BOOST // ACTIVE" else "BOOST // STANDBY", cx, cy + 59f, paint)
    }
}
