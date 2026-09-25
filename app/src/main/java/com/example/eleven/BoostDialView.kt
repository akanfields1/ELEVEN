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

    private val gold = Color.rgb(212, 162, 26)
    private val brightGold = Color.rgb(229, 192, 77)
    private val dimGold = Color.rgb(96, 72, 12)
    private val panel = Color.rgb(13, 13, 13)
    private val white = Color.rgb(243, 235, 210)
    private val muted = Color.rgb(180, 158, 98)

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
        canvas.drawCircle(cx, cy, radius + 38f, paint)

        paint.color = Color.rgb(32, 24, 10)
        canvas.drawCircle(cx, cy, radius + 24f, paint)

        paint.color = panel
        canvas.drawCircle(cx, cy, radius + 10f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = 18f
        paint.color = Color.rgb(33, 33, 33)
        arcRect.set(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(arcRect, 135f, 270f, false, paint)

        val progress = (boostPercent - 100) / 100f
        paint.color = if (active) brightGold else dimGold
        canvas.drawArc(arcRect, 135f, 270f * progress, false, paint)

        paint.strokeWidth = 3.5f
        repeat(29) { i ->
            val f = i / 28f
            val angleDeg = 135f + 270f * f
            val angle = Math.toRadians(angleDeg.toDouble())
            val outer = radius + 37f
            val inner = if (i % 7 == 0) radius + 18f else radius + 26f
            val x1 = cx + (cos(angle) * inner).toFloat()
            val y1 = cy + (sin(angle) * inner).toFloat()
            val x2 = cx + (cos(angle) * outer).toFloat()
            val y2 = cy + (sin(angle) * outer).toFloat()
            paint.color = if (f <= progress && active) gold else Color.rgb(58, 58, 58)
            canvas.drawLine(x1, y1, x2, y2, paint)
        }

        paint.style = Paint.Style.FILL
        paint.color = if (active) Color.rgb(18, 16, 12) else Color.rgb(14, 14, 14)
        canvas.drawCircle(cx, cy, radius - 18f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = if (active) Color.argb(150, 255, 243, 176) else Color.argb(70, 212, 162, 26)
        canvas.drawCircle(cx, cy, radius - 18f, paint)

        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD)
        paint.textSize = 53f
        paint.color = if (active) brightGold else white
        canvas.drawText("$boostPercent%", cx, cy - 4f, paint)

        val gainDb = GlobalBoostEngine.percentToDb(boostPercent)
        paint.typeface = android.graphics.Typeface.create("monospace", android.graphics.Typeface.NORMAL)
        paint.textSize = 17f
        paint.color = muted
        canvas.drawText(String.format("%+.1f dB", gainDb), cx, cy + 28f, paint)

        paint.textSize = 14f
        paint.color = if (active) gold else muted
        canvas.drawText(if (active) "ELEVEN // ACTIVE" else "ELEVEN // STANDBY", cx, cy + 55f, paint)
    }
}
