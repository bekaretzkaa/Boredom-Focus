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

    private val items = mutableListOf<DailyStatsEntity?>()
    private var selectedIndex = -1

    private val detoxPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val focusPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val disabledPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val detoxRect = RectF()
    private val focusRect = RectF()

    private var selectedBarRect: RectF? = null

    private var chartAnimationProgress = 0f

    private val barBounds = mutableListOf<RectF>()
    private val barAlphas = mutableListOf<Float>()
    private val barScales = mutableListOf<Float>()

    var onBarSelected: ((DailyStatsEntity) -> Unit)? = null
    var onSelectionCleared: (() -> Unit)? = null

    init {
        detoxPaint.color = Color.parseColor("#E04A49")
        focusPaint.color = Color.parseColor("#639922")

        disabledPaint.apply {
            color = Color.parseColor("#D9D9D9")
            alpha = 90
        }

        textPaint.apply {
            color = Color.parseColor("#666666")
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
    }

    fun submitData(data: List<DailyStatsEntity?>) {
        items.clear()
        items.addAll(data)

        if (selectedIndex !in items.indices || items.getOrNull(selectedIndex) == null) {
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
            .filterNotNull()
            .maxOfOrNull { item ->
                max(
                    item.totalDetoxMinutes.toFloat(),
                    item.totalFocusSeconds / 60f
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

            val label = getLabelByIndex(index)

            if (item == null) {
                drawDisabledBar(
                    canvas = canvas,
                    detoxLeft = detoxLeft,
                    detoxRight = detoxRight,
                    focusLeft = focusLeft,
                    focusRight = focusRight,
                    chartHeight = chartHeight,
                    centerX = centerX,
                    label = label
                )

                return@forEachIndexed
            }

            if (item.isEmptyDay()) {
                drawEmptyActiveBar(
                    canvas = canvas,
                    detoxLeft = detoxLeft,
                    detoxRight = detoxRight,
                    focusLeft = focusLeft,
                    focusRight = focusRight,
                    chartHeight = chartHeight,
                    centerX = centerX,
                    label = label,
                    isSelected = selectedIndex == index
                )

                return@forEachIndexed
            }

            val detoxMinutes = item.totalDetoxMinutes.toFloat()
            val focusMinutes = item.totalFocusSeconds / 60f

            val focusHeight =
                chartHeight * focusMinutes / safeMaxValue * chartAnimationProgress * barScales[index]

            val detoxHeight =
                chartHeight * detoxMinutes / safeMaxValue * chartAnimationProgress * barScales[index]

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

            textPaint.alpha = 255

            canvas.drawText(
                label,
                centerX,
                height - 10f,
                textPaint
            )
        }
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

                if (oldIndex >= 0) {
                    barAlphas[oldIndex] = 1f - progress * 0.7f
                    barScales[oldIndex] = 1.08f - progress * 0.08f
                }

                barAlphas[newIndex] = 0.3f + progress * 0.7f
                barScales[newIndex] = 1f + progress * 0.12f

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

                barAlphas[oldIndex] = alpha
                barScales[oldIndex] = 1f + (alpha - 0.3f) / 0.7f * 0.08f

                invalidate()
            }

            start()
        }

        onSelectionCleared?.invoke()
    }

    private fun getLabelByIndex(index: Int): String {
        return when (index) {
            0 -> "ПН"
            1 -> "ВТ"
            2 -> "СР"
            3 -> "ЧТ"
            4 -> "ПТ"
            5 -> "СБ"
            6 -> "ВС"
            else -> ""
        }
    }

    private fun DailyStatsEntity.isEmptyDay(): Boolean {
        return totalDetoxMinutes == 0L && totalFocusSeconds == 0L
    }

    private fun drawEmptyActiveBar(
        canvas: Canvas,
        detoxLeft: Float,
        detoxRight: Float,
        focusLeft: Float,
        focusRight: Float,
        chartHeight: Float,
        centerX: Float,
        label: String,
        isSelected: Boolean
    ) {
        val placeholderHeight = if (isSelected) 14f else 10f
        val alpha = if (isSelected) 255 else 120

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

        detoxPaint.alpha = alpha
        focusPaint.alpha = alpha

        detoxPaint.style = Paint.Style.STROKE
        focusPaint.style = Paint.Style.STROKE

        detoxPaint.strokeWidth = 3f
        focusPaint.strokeWidth = 3f

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

        detoxPaint.style = Paint.Style.FILL
        focusPaint.style = Paint.Style.FILL

        textPaint.alpha = 255

        canvas.drawText(
            label,
            centerX,
            height - 10f,
            textPaint
        )
    }
}