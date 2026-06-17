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
import com.example.boredomfocus.data.local.entity.DailyStatsEntity
import com.example.boredomfocus.feature.statistics.presentation.model.ChartItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
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
    private val disabledPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val detoxRect = RectF()
    private val focusRect = RectF()

    private var chartAnimationProgress = 0f

    private val barBounds = mutableListOf<RectF>()
    private val barAlphas = mutableListOf<Float>()
    private val barScales = mutableListOf<Float>()

    var onBarSelected: ((ChartItem) -> Unit)? = null
    var onSelectionCleared: (() -> Unit)? = null

    init {
        detoxPaint.color = Color.parseColor("#E04A49")
        focusPaint.color = Color.parseColor("#639922")

        disabledPaint.apply {
            color = Color.parseColor("#D9D9D9")
            alpha = 90
            style = Paint.Style.FILL
        }

        textPaint.apply {
            color = Color.parseColor("#666666")
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
    }

    fun submitData(data: List<ChartItem>) {
        items.clear()
        items.addAll(data)

        if (selectedIndex !in items.indices || items.getOrNull(selectedIndex)?.isFutureDay() == true) {
            selectedIndex = -1
        }

        barAlphas.clear()
        barScales.clear()

        repeat(items.size) {
            barAlphas.add(0.3f)
            barScales.add(1f)
        }

        if (selectedIndex >= 0) {
            barAlphas[selectedIndex] = 1f
        }

        startChartAnimation()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (items.isEmpty()) return

        barBounds.clear()

        val maxValue = items
            .filterNot { it.isFutureDay() }
            .maxOfOrNull { item ->
                max(
                    item.detoxMinutes.toFloat(),
                    item.focusMinutes.toFloat()
                )
            } ?: 0f

        val safeMaxValue = max(maxValue, 1f)

        val chartHeight = height * 0.75f
        val itemWidth = width.toFloat() / items.size
        val groupWidth = itemWidth * 0.9f

        items.forEachIndexed { index, item ->

            val centerX = itemWidth * index + itemWidth / 2
            val groupStart = centerX - groupWidth / 2

            val detoxLeft = groupStart
            val detoxRight = detoxLeft + groupWidth / 2 - 2f

            val focusLeft = detoxRight + 4f
            val focusRight = groupStart + groupWidth

            barBounds.add(
                RectF(
                    detoxLeft,
                    0f,
                    focusRight,
                    chartHeight
                )
            )

            if (item.isFutureDay()) {
                drawDisabledBar(
                    canvas = canvas,
                    detoxLeft = detoxLeft,
                    detoxRight = detoxRight,
                    focusLeft = focusLeft,
                    focusRight = focusRight,
                    chartHeight = chartHeight,
                    centerX = centerX,
                    label = item.label
                )

                return@forEachIndexed
            }

            drawActiveBar(
                canvas = canvas,
                item = item,
                index = index,
                detoxLeft = detoxLeft,
                detoxRight = detoxRight,
                focusLeft = focusLeft,
                focusRight = focusRight,
                chartHeight = chartHeight,
                centerX = centerX,
                safeMaxValue = safeMaxValue
            )
        }
    }

    private fun drawActiveBar(
        canvas: Canvas,
        item: ChartItem,
        index: Int,
        detoxLeft: Float,
        detoxRight: Float,
        focusLeft: Float,
        focusRight: Float,
        chartHeight: Float,
        centerX: Float,
        safeMaxValue: Float
    ) {
        val minVisibleBarHeight = if (selectedIndex == index) 14f else 10f

        val detoxValue = item.detoxMinutes.coerceAtLeast(0).toFloat()
        val focusValue = item.focusMinutes.coerceAtLeast(0).toFloat()

        val detoxHeight = if (detoxValue == 0f) {
            minVisibleBarHeight
        } else {
            chartHeight * detoxValue / safeMaxValue * chartAnimationProgress * barScales[index]
        }

        val focusHeight = if (focusValue == 0f) {
            minVisibleBarHeight
        } else {
            chartHeight * focusValue / safeMaxValue * chartAnimationProgress * barScales[index]
        }

        detoxRect.set(
            detoxLeft,
            chartHeight - detoxHeight,
            detoxRight,
            chartHeight
        )

        focusRect.set(
            focusLeft,
            chartHeight - focusHeight,
            focusRight,
            chartHeight
        )

        if (selectedIndex == index) {
            val alpha = (barAlphas[index] * 255).toInt()
            detoxPaint.alpha = alpha
            focusPaint.alpha = alpha
        } else {
            detoxPaint.alpha = 80
            focusPaint.alpha = 80
        }

        detoxPaint.style = Paint.Style.FILL
        focusPaint.style = Paint.Style.FILL

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

        textPaint.alpha = 255

        canvas.drawText(
            item.label,
            centerX,
            height - 10f,
            textPaint
        )
    }

    private fun drawDisabledBar(
        canvas: Canvas,
        detoxLeft: Float,
        detoxRight: Float,
        focusLeft: Float,
        focusRight: Float,
        chartHeight: Float,
        centerX: Float,
        label: String
    ) {
        val placeholderHeight = 8f

        detoxRect.set(
            detoxLeft,
            chartHeight - placeholderHeight,
            detoxRight,
            chartHeight
        )

        focusRect.set(
            focusLeft,
            chartHeight - placeholderHeight,
            focusRight,
            chartHeight
        )

        disabledPaint.style = Paint.Style.STROKE
        disabledPaint.strokeWidth = 3f

        canvas.drawRoundRect(
            detoxRect,
            6f,
            6f,
            disabledPaint
        )

        canvas.drawRoundRect(
            focusRect,
            6f,
            6f,
            disabledPaint
        )

        textPaint.alpha = 100

        canvas.drawText(
            label,
            centerX,
            height - 10f,
            textPaint
        )

        textPaint.alpha = 255
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            barBounds.forEachIndexed { index, rect ->

                val item = items.getOrNull(index) ?: return@forEachIndexed

                if (item.isFutureDay()) {
                    return@forEachIndexed
                }

                if (rect.contains(event.x, event.y)) {
                    if (selectedIndex == index) {
                        clearSelection()
                    } else {
                        animateSelection(index)
                        onBarSelected?.invoke(item)
                    }

                    return true
                }
            }
        }

        return super.onTouchEvent(event)
    }

    private fun startChartAnimation() {
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
        if (selectedIndex == newIndex) return

        val oldIndex = selectedIndex
        selectedIndex = newIndex

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 250

            addUpdateListener {
                val progress = it.animatedValue as Float

                if (oldIndex >= 0 && oldIndex < barAlphas.size) {
                    barAlphas[oldIndex] = 1f - progress * 0.7f
                    barScales[oldIndex] = 1.08f - progress * 0.08f
                }

                if (newIndex < barAlphas.size) {
                    barAlphas[newIndex] = 0.3f + progress * 0.7f
                    barScales[newIndex] = 1f + progress * 0.12f
                }

                invalidate()
            }

            start()
        }
    }

    private fun clearSelection() {
        val oldIndex = selectedIndex

        if (oldIndex < 0) return

        selectedIndex = -1

        ValueAnimator.ofFloat(1f, 0.3f).apply {
            duration = 200

            addUpdateListener {
                val alpha = it.animatedValue as Float

                if (oldIndex < barAlphas.size) {
                    barAlphas[oldIndex] = alpha
                    barScales[oldIndex] = 1f + (alpha - 0.3f) / 0.7f * 0.08f
                }

                invalidate()
            }

            start()
        }

        onSelectionCleared?.invoke()
    }

    private fun ChartItem.isFutureDay(): Boolean {
        return detoxMinutes == -1 &&
                focusMinutes == -1 &&
                sessionsCount == -1
    }
}