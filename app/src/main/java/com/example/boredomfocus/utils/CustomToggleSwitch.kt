package com.example.boredomfocus.utils

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import com.example.boredomfocus.R

class CustomToggleSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val thumbView = View(context)

    private var checkedChangeListener: ((Boolean) -> Unit)? = null

    var isChecked: Boolean = false
        private set

    init {
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.CustomToggleSwitch
        )

        isChecked = typedArray.getBoolean(
            R.styleable.CustomToggleSwitch_switchChecked,
            false
        )

        typedArray.recycle()

        isClickable = true
        isFocusable = true

        setPadding(
            4.dpToPx(),
            4.dpToPx(),
            4.dpToPx(),
            4.dpToPx()
        )

        addThumb()

        setOnClickListener {
            setChecked(!isChecked, animate = true)
            checkedChangeListener?.invoke(isChecked)
        }

        post {
            updateSwitch(animate = false)
        }
    }

    private fun addThumb() {
        val thumbSize = 24.dpToPx()

        val params = LayoutParams(thumbSize, thumbSize).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
        }

        thumbView.layoutParams = params
        addView(thumbView)
    }

    fun setChecked(checked: Boolean, animate: Boolean = false) {
        if (isChecked == checked) return

        isChecked = checked
        updateSwitch(animate)
    }

    fun setOnCheckedChangeListener(listener: (Boolean) -> Unit) {
        checkedChangeListener = listener
    }

    private fun updateSwitch(animate: Boolean) {
        setBackgroundResource(
            if (isChecked) R.drawable.bg_switch_track_on
            else R.drawable.bg_switch_track_off
        )

        thumbView.setBackgroundResource(
            if (isChecked) R.drawable.bg_switch_thumb_on
            else R.drawable.bg_switch_thumb_off
        )

        val targetX = if (isChecked) {
            width - paddingLeft - paddingRight - thumbView.width
        } else {
            0
        }.toFloat()

        thumbView.animate().cancel()

        if (animate) {
            thumbView.animate()
                .translationX(targetX)
                .setDuration(180)
                .start()
        } else {
            thumbView.translationX = targetX
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}