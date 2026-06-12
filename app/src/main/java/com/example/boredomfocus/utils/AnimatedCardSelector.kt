package com.example.boredomfocus.utils

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView

class AnimatedCardSelector(
    private val context: Context,
    private val items: List<Item>,
    @ColorRes private val unselectedStrokeColorRes: Int,
    private val onSelected: ((Int) -> Unit)? = null
) {

    data class Item(
        val card: MaterialCardView,
        val title: TextView,
        @ColorRes val selectedColorRes: Int,
        @ColorRes val selectedBackgroundColorRes: Int
    )

    private val duration = 200L

    fun select(selectedIndex: Int) {
        items.forEachIndexed { index, item ->

            if (index == selectedIndex) {
                selectItem(item)
            } else {
                unselectItem(item)
            }
        }

        onSelected?.invoke(selectedIndex)
    }

    private fun selectItem(item: Item) {
        val selectedColor = ContextCompat.getColor(context, item.selectedColorRes)
        val selectedBgColor = ContextCompat.getColor(context, item.selectedBackgroundColorRes)

        item.card.strokeWidth = dpToPx(3)

        animateCard(
            card = item.card,
            title = item.title,
            strokeColor = selectedColor,
            backgroundColor = selectedBgColor,
            titleColor = selectedColor,
            scale = 1.03f
        )
    }

    private fun unselectItem(item: Item) {
        val unselectedColor = ContextCompat.getColor(context, unselectedStrokeColorRes)

        item.card.strokeWidth = dpToPx(2)

        animateCard(
            card = item.card,
            title = item.title,
            strokeColor = unselectedColor,
            backgroundColor = Color.TRANSPARENT,
            titleColor = Color.WHITE,
            scale = 1f
        )
    }

    private fun animateCard(
        card: MaterialCardView,
        title: TextView,
        strokeColor: Int,
        backgroundColor: Int,
        titleColor: Int,
        scale: Float
    ) {
        animateColor(card.strokeColor, strokeColor) { color ->
            card.strokeColor = color
        }

        animateColor(card.cardBackgroundColor.defaultColor, backgroundColor) { color ->
            card.setCardBackgroundColor(color)
        }

        animateColor(title.currentTextColor, titleColor) { color ->
            title.setTextColor(color)
        }

        card.animate()
            .scaleX(scale)
            .scaleY(scale)
            .setDuration(duration)
            .start()
    }

    private fun animateColor(
        fromColor: Int,
        toColor: Int,
        onUpdate: (Int) -> Unit
    ) {
        ValueAnimator.ofArgb(fromColor, toColor).apply {
            duration = this@AnimatedCardSelector.duration
            addUpdateListener { animator ->
                onUpdate(animator.animatedValue as Int)
            }
            start()
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

}