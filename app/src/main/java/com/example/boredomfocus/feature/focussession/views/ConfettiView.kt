package com.example.boredomfocus.feature.focussession.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class ConfettiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private data class Particle(
        var x: Float,
        var y: Float,
        val width: Float,
        val height: Float,
        val color: Int,
        val speedY: Float,
        val speedX: Float,
        var rotation: Float,
        val rotationSpeed: Float
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val maxFrames = 240

    private val colors = listOf(
        Color.parseColor("#639922"),
        Color.WHITE,
        Color.parseColor("#E24B4A"),
        Color.parseColor("#BA7517"),
        Color.parseColor("#378ADD")
    )

    private var particles = emptyList<Particle>()
    private var isRunning = false
    private var frame = 0

    fun start() {
        visibility = VISIBLE
        bringToFront()

        if (width == 0 || height == 0) {
            post { start() }
            return
        }

        frame = 0
        isRunning = true

        particles = List(110) {
            Particle(
                x = Random.nextFloat() * width,
                y = -Random.nextFloat() * height * 0.4f,
                width = Random.nextFloat() * 10f + 6f,
                height = Random.nextFloat() * 5f + 4f,
                color = colors.random(),
                speedY = Random.nextFloat() * 4f + 3f,
                speedX = Random.nextFloat() * 3f - 1.5f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = Random.nextFloat() * 8f - 4f
            )
        }

        invalidate()
    }

    fun stop() {
        isRunning = false
        particles = emptyList()
        visibility = INVISIBLE
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!isRunning) return

        particles.forEach { particle ->
            particle.x += particle.speedX
            particle.y += particle.speedY
            particle.rotation += particle.rotationSpeed

            val alpha = ((1f - particle.y / height) * 255)
                .toInt()
                .coerceIn(0, 255)

            paint.color = particle.color
            paint.alpha = alpha

            canvas.save()
            canvas.translate(particle.x, particle.y)
            canvas.rotate(particle.rotation)
            canvas.drawRect(
                -particle.width / 2,
                -particle.height / 2,
                particle.width / 2,
                particle.height / 2,
                paint
            )
            canvas.restore()
        }

        frame++

        if (frame < maxFrames) {
            postInvalidateOnAnimation()
        } else {
            stop()
        }
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }
}