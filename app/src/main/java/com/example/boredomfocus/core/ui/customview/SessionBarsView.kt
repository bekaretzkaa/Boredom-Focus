package com.example.boredomfocus.core.ui.customview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.boredomfocus.databinding.ViewSessionBarsBinding

class SessionBarsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(
    context, attrs
) {
    private val binding = ViewSessionBarsBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    fun bind(
        detoxTime: Int,
        focusTime: Int
    ) {
        binding.tvSessionDetoxWord.text = "${detoxTime / 60} мин"

        val focusMinutes = focusTime / 60
        val focusSeconds = focusTime % 60

        binding.tvSessionFocusWord.text = String.format("%02d:%02d", focusMinutes, focusSeconds)

        val (detoxPercent, focusPercent) = calculateProgress(detoxTime, focusTime)

        setPercent(binding.viewDetoxProgress, detoxPercent)
        setPercent(binding.viewFocusProgress, focusPercent)
    }

    private fun calculateProgress(
        detoxTime: Int,
        focusTime: Int
    ) : Pair<Float, Float> {

        if(detoxTime == 0 && focusTime == 0) {
            return 0f to 0f
        }

        val max = maxOf(detoxTime, focusTime)

        return (detoxTime.toFloat() / max) to (focusTime.toFloat() / max)

    }

    private fun setPercent(
        view: View,
        percent: Float
    ) {
        val params = view.layoutParams as LayoutParams

        params.matchConstraintPercentWidth = percent

        view.layoutParams = params
    }
}