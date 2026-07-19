package com.example.boredomfocus.core.ui.customview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getString
import com.example.boredomfocus.R
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
        detoxSelectedMinutes: Int,
        detoxElapsedSeconds: Int,
        focusTime: Int,
        time: String,
        completed: Boolean
    ) {
        val focusMinutes = focusTime / 60
        val focusSeconds = focusTime % 60

        binding.tvTime.text = time

        if(completed) {
            if(detoxElapsedSeconds == 0) {
                binding.tvSessionDetoxWord.apply {
                    text = String.format("%02d:%02d", focusMinutes, focusSeconds)
                    setTextColor(ContextCompat.getColor(context, R.color.green_basic))
                }
                binding.tvSessionFocusWord.visibility = GONE
                binding.cardDescription.apply {
                    visibility = VISIBLE
                    setCardBackgroundColor(ContextCompat.getColor(context, R.color.green_bg))
                }
                binding.tvDescription.apply {
                    setTextColor(ContextCompat.getColor(context, R.color.green_basic))
                }
                binding.tvDescription.setText(R.string.statistics_focus)
                binding.viewDetox.setBackgroundResource(R.drawable.gray_empty_square)
                binding.viewFocus.setBackgroundResource(R.drawable.green_focus_square)
            } else if(focusTime == 0) {
                binding.tvSessionDetoxWord.apply {
                    text = context.getString(R.string.statistics_minutes, detoxElapsedSeconds / 60)
                    setTextColor(ContextCompat.getColor(context, R.color.red_basic))
                }
                binding.tvSessionFocusWord.visibility = GONE
                binding.cardDescription.apply {
                    visibility = VISIBLE
                    setCardBackgroundColor(ContextCompat.getColor(context, R.color.difficulty_red_bg))
                }
                binding.tvDescription.apply {
                    text = context.getString(R.string.statistics_detox)
                    setTextColor(ContextCompat.getColor(context, R.color.red_basic))
                }
                binding.viewDetox.setBackgroundResource(R.drawable.red_detox_square)
                binding.viewFocus.setBackgroundResource(R.drawable.gray_empty_square)
            } else {
                binding.tvSessionDetoxWord.apply {
                    text = context.getString(R.string.statistics_minutes, detoxElapsedSeconds / 60)
                    setTextColor(ContextCompat.getColor(context, R.color.red_basic))
                }
                binding.tvSessionFocusWord.apply {
                    visibility = VISIBLE
                    text = String.format("%02d:%02d", focusMinutes, focusSeconds)
                }
                binding.cardDescription.visibility = GONE
                binding.viewDetox.setBackgroundResource(R.drawable.red_detox_square)
                binding.viewFocus.setBackgroundResource(R.drawable.green_focus_square)
            }
        } else {
            binding.tvSessionDetoxWord.apply {
                text = "${String.format("%02d:%02d", detoxElapsedSeconds / 60, detoxElapsedSeconds % 60)}/${detoxSelectedMinutes}:00"
                setTextColor(ContextCompat.getColor(context, R.color.red_basic))
            }
            binding.tvSessionFocusWord.visibility = GONE
            binding.cardDescription.apply {
                visibility = VISIBLE
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.difficulty_red_bg))
            }
            binding.tvDescription.apply {
                text = context.getString(R.string.statistics_interrupted)
                setTextColor(ContextCompat.getColor(context, R.color.red_basic))
            }

            binding.viewDetox.setBackgroundResource(R.drawable.red_detox_square)
            binding.viewFocus.setBackgroundResource(R.drawable.gray_empty_square)
        }

        val (detoxPercent, focusPercent) = calculateProgress(detoxSelectedMinutes, detoxElapsedSeconds, focusTime)

        setPercent(binding.viewDetoxProgress, detoxPercent)
        setPercent(binding.viewFocusProgress, focusPercent)
    }

    private fun calculateProgress(
        detoxSelectedMinutes: Int,
        detoxElapsedSeconds: Int,
        focusTime: Int
    ) : Pair<Float, Float> {

        if(detoxSelectedMinutes * 60 != detoxElapsedSeconds) {
            return (detoxElapsedSeconds.toFloat() / (detoxSelectedMinutes * 60)) to 0f
        } else {
            if(focusTime == 0) {
                return 0.8f to 0f
            } else if (detoxElapsedSeconds == 0){
                return 0f to 0.8f
            } else {
                val max = maxOf(detoxElapsedSeconds, focusTime)
                return (detoxElapsedSeconds.toFloat() / max) to (focusTime.toFloat() / max)
            }
        }
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