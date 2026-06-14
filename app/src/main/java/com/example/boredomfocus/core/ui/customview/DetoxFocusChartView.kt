package com.example.boredomfocus.core.ui.customview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.boredomfocus.feature.statistics.presentation.model.ChartItem
import kotlin.math.max
import kotlin.math.min

class DetoxFocusChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val items = mutableListOf<ChartItem>()
    private var selectedIndex = -1

    private val detoxPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val focusPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val detoxRect = RectF()
    private val focusRect = RectF()

    private var selectedBarRect: RectF? = null

    private var chartAnimationProgress = 0f

    private val barBounds = mutableListOf<RectF>()
    private val barAlphas = mutableListOf<Float>()
    private val barScales = mutableListOf<Float>()
    var onBarSelected: ((ChartItem) -> Unit)? = null
    var onSelectionCleared: (() -> Unit)? = null

    init {
        detoxPaint.color = Color.parseColor("#E04A49")
        focusPaint.color = Color.parseColor("#639922")

        textPaint.apply {
            color = Color.parseColor("#666666")
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
    }

    fun submitData(data: List<ChartItem>) {
        items.clear()
        items.addAll(data)

        barAlphas.clear()
        barScales.clear()
        repeat(items.size) {
            barAlphas.add(0.3f)
            barScales.add(1f)
        }

        if(selectedIndex >= 0) {
            barAlphas[selectedIndex] = 1f
        }

        startChartAnimation()

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (items.isEmpty()) return

        barBounds.clear()

        val maxValue = items.maxOf {
            max(it.focusMinutes, it.detoxMinutes)
        }

        val chartHeight = height * 0.75f

        val itemWidth = width.toFloat() / items.size

        val groupWidth = itemWidth * 0.9f

        items.forEachIndexed { index, item ->

            val centerX = itemWidth * index + itemWidth / 2

            val focusHeight =
                chartHeight * item.focusMinutes / maxValue * chartAnimationProgress * barScales[index]

            val detoxHeight =
                chartHeight * item.detoxMinutes / maxValue * chartAnimationProgress * barScales[index]

            val groupStart = centerX - groupWidth / 2

            val detoxLeft = groupStart
            val detoxRight = detoxLeft + groupWidth / 2 - 2f

            val focusLeft = detoxRight + 4f
            val focusRight = groupStart + groupWidth

            focusRect.set(
                focusLeft,
                chartHeight - focusHeight,
                focusRight,
                chartHeight
            )

            detoxRect.set(
                detoxLeft,
                chartHeight - detoxHeight,
                detoxRight,
                chartHeight
            )

            if (selectedIndex == index) {

                val alpha = (barAlphas[index] * 255).toInt()

                focusPaint.alpha = alpha
                detoxPaint.alpha = alpha

                selectedBarRect = RectF(
                    detoxLeft,
                    min(focusRect.top, detoxRect.top),
                    focusRight,
                    chartHeight
                )

            } else {

                focusPaint.alpha = 80
                detoxPaint.alpha = 80
            }

            canvas.drawRoundRect(
                detoxRect,
                6f,
                6f,
                detoxPaint
            )

            canvas.drawRoundRect(
                focusRect,
                6f,
                6f,
                focusPaint
            )

            canvas.drawText(
                item.label,
                centerX,
                height - 10f,
                textPaint
            )

            barBounds.add(
                RectF(
                    detoxLeft,
                    0f,
                    focusRight,
                    chartHeight
                )
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if(event.action == MotionEvent.ACTION_DOWN) {
            barBounds.forEachIndexed { index, rect ->

                if(rect.contains(event.x, event.y)) {
                    if(selectedIndex == index) {
                        clearSelection()
                    } else {
                        animateSelection(index)
                        onBarSelected?.invoke(items[index])
                    }

                    return true
                }

            }
        }

        return super.onTouchEvent(event)
    }

    fun startChartAnimation() {

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 800
            addUpdateListener {
                chartAnimationProgress = it.animatedValue as Float
                invalidate()
            }

            start()
        }

    }

    private fun animateSelection(newIndex: Int) {
        if(selectedIndex == newIndex) return

        val oldIndex = selectedIndex

        ValueAnimator.ofFloat(0f, 1f).apply {

            duration = 250

            addUpdateListener {

                val progress = it.animatedValue as Float

                if(oldIndex >= 0) {
                    barAlphas[oldIndex] = 1f - progress * 0.7f
                    barScales[oldIndex] = 1.08f - progress * 0.08f
                }

                barAlphas[newIndex] = 0.3f + progress * 0.7f
                barScales[newIndex] = 1f + progress * 0.12f

                invalidate()

            }

            start()
        }
        selectedIndex = newIndex
    }

    private fun clearSelection() {
        val oldIndex = selectedIndex

        if(oldIndex < 0) return

        ValueAnimator.ofFloat(1f, 0.3f).apply {
            duration = 200

            addUpdateListener {
                val alpha = it.animatedValue as Float
                barAlphas[oldIndex] = alpha
                barScales[oldIndex] = 1f + (alpha - 0.3f) / 0.7f * 0.08f

                invalidate()
            }
            start()
        }

        selectedIndex = -1
        onSelectionCleared?.invoke()
    }
}