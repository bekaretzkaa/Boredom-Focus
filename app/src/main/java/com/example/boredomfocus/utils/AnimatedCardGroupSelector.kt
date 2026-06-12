package com.example.boredomfocus.utils

import android.animation.ValueAnimator
import android.content.Context
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView

class AnimatedCardGroupSelector(
    private val context: Context,
    private val cards: List<MaterialCardView>,
    @ColorRes private val selectedStrokeColorRes: Int,
    @ColorRes private val unselectedStrokeColorRes: Int,
    private val selectedStrokeWidthDp: Int = 2,
    private val unselectedStrokeWidthDp: Int = 1,
    private val selectedScale: Float = 1.04f,
    private val unselectedScale: Float = 1f,
    private val duration: Long = 120L,
    private val onSelected: ((Int) -> Unit)? = null
) {

    private var selectedIndex: Int = -1

    fun select(index: Int) {
        if (index !in cards.indices) return
        if (selectedIndex == index) return

        selectedIndex = index

        cards.forEachIndexed { cardIndex, card ->
            if (cardIndex == index) {
                selectCard(card)
            } else {
                unselectCard(card)
            }
        }

        onSelected?.invoke(index)
    }

    private fun selectCard(card: MaterialCardView) {
        val selectedColor = ContextCompat.getColor(context, selectedStrokeColorRes)

        card.strokeWidth = dpToPx(selectedStrokeWidthDp)

        animateStrokeColor(
            card = card,
            toColor = selectedColor
        )

        animateScale(
            card = card,
            scale = selectedScale
        )
    }

    private fun unselectCard(card: MaterialCardView) {
        val unselectedColor = ContextCompat.getColor(context, unselectedStrokeColorRes)

        card.strokeWidth = dpToPx(unselectedStrokeWidthDp)

        animateStrokeColor(
            card = card,
            toColor = unselectedColor
        )

        animateScale(
            card = card,
            scale = unselectedScale
        )
    }

    private fun animateStrokeColor(
        card: MaterialCardView,
        toColor: Int
    ) {
        ValueAnimator.ofArgb(card.strokeColor, toColor).apply {
            duration = this@AnimatedCardGroupSelector.duration

            addUpdateListener { animator ->
                card.strokeColor = animator.animatedValue as Int
            }

            start()
        }
    }

    private fun animateScale(
        card: MaterialCardView,
        scale: Float
    ) {
        card.animate().cancel()

        card.animate()
            .scaleX(scale)
            .scaleY(scale)
            .setDuration(duration)
            .start()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}