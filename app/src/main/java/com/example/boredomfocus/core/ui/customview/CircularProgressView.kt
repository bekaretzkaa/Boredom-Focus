package com.example.boredomfocus.core.ui.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.View

class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs)  {

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1A1A")
        style = Paint.Style.STROKE
        strokeWidth = 22f
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E24B4A")
        style = Paint.Style.STROKE
        strokeWidth = 22f
        strokeCap = Paint.Cap.ROUND
    }

    private val rect = RectF()
    var progress: Float = 1f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val padding = progressPaint.strokeWidth

        rect.set(
            padding,
            padding,
            width - padding,
            height - padding
        )

        canvas.drawArc(
            rect,
            0f,
            360f,
            false,
            backgroundPaint
        )

        canvas.drawArc(
            rect,
            -90f,
            progress * 360f,
            false,
            progressPaint
        )
    }

    private var shader: SweepGradient? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        shader = SweepGradient(
            w / 2f,
            h / 2f,
            intArrayOf(
                Color.parseColor("#FF7A7A"),
                Color.parseColor("#FF5252"),
                Color.parseColor("#FF2D55"),
                Color.parseColor("#FF7A7A")
            ),
            null
        )
        progressPaint.shader = shader
    }

}