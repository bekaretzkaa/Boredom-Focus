package com.example.boredomfocus.ui

import android.animation.LayoutTransition
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.example.boredomfocus.ChartItem
import com.example.boredomfocus.R
import com.example.boredomfocus.SessionAdapter
import com.example.boredomfocus.SessionListItem
import com.example.boredomfocus.databinding.FragmentStatisticsBinding

class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionAdapter: SessionAdapter

    private var cardVisible = false

    private var currentDetoxMinutes = 0
    private var currentFocusMinutes = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStatisticsBinding.bind(view)

        val sessions = listOf(
            SessionListItem.Header("Сегодня"),

            SessionListItem.Session(
                300,
                935
            ),
            SessionListItem.Session(
                600,
                1233
            ),

            SessionListItem.Header("9 июня"),

            SessionListItem.Session(
                300,
                2100
            ),
            SessionListItem.Session(
                600,
                780
            ),

            SessionListItem.Header("8 июня"),

            SessionListItem.Session(
                300,
                633
            ),
            SessionListItem.Session(
                900,
                4256
            ),
        )

        sessionAdapter = SessionAdapter()

        binding.cardChosenChartItem.visibility = View.GONE

        binding.rvSessionsStatistics.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = sessionAdapter
        }

        sessionAdapter.submitList(sessions)

        binding.detoxFocusBarChartView.submitData(
            listOf(
                ChartItem("ПН", 120, 200, 2),
                ChartItem("ВТ", 180, 220, 1),
                ChartItem("СР", 90, 210, 3),
                ChartItem("ЧТ", 250, 220, 5),
                ChartItem("ПТ", 150, 180, 1),
                ChartItem("СБ", 190, 200, 2),
                ChartItem("ВС", 320, 210, 1)
            )
        )

        binding.detoxFocusBarChartView.onBarSelected = { item ->

            if(!cardVisible) {
                showDetailsCard(item)
                cardVisible = true
            } else {

                animateCardContentChange {
                    updateChartCard(item)
                }

            }
        }

        binding.detoxFocusBarChartView.onSelectionCleared = {

            hideDetailsCard()
            cardVisible = false

        }
    }

    private fun updateChartCard(item: ChartItem) {
        binding.tvChartItemLabel.text = item.label

        animateMinutesText(
            binding.tvChartItemFocusTime,
            currentFocusMinutes,
            item.focusMinutes
        )

        animateMinutesText(
            binding.tvChartItemDetoxTime,
            currentDetoxMinutes,
            item.detoxMinutes
        )

        currentFocusMinutes = item.focusMinutes
        currentDetoxMinutes = item.detoxMinutes

        binding.tvChartItemSessionCount.text = "${item.sessionsCount} сессий"
    }

    private fun formatMinutes(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        if(hours == 0) return "$minutes мин"
        return "$hours ч $minutes мин"
    }

    private fun showDetailsCard(item: ChartItem) {

        currentFocusMinutes = 0
        currentDetoxMinutes = 0

        binding.tvChartItemLabel.text = item.label
        binding.tvChartItemSessionCount.text =
            "${item.sessionsCount} сессий"

        binding.tvChartItemFocusTime.text = "0 мин"
        binding.tvChartItemDetoxTime.text = "0 мин"

        binding.cardChosenChartItem.apply {

            alpha = 0f
            translationY = 24f
            scaleX = 0.96f
            scaleY = 0.96f
        }

        TransitionManager.beginDelayedTransition(
            binding.cardChosenChartItem.parent as ViewGroup,
            AutoTransition()
        )

        binding.cardChosenChartItem.visibility = View.VISIBLE

        binding.cardChosenChartItem.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setInterpolator(OvershootInterpolator(1.1f))
            .setDuration(280)
            .withEndAction {

                binding.cardChosenChartItem.postDelayed({
                    updateChartCard(item)
                }, 300)

            }
            .start()
    }

    private fun hideDetailsCard() {

        binding.cardChosenChartItem.animate()
            .alpha(0f)
            .translationY(12f)
            .scaleX(0.98f)
            .scaleY(0.98f)
            .setDuration(150)
            .withEndAction {

                TransitionManager.beginDelayedTransition(
                    binding.cardChosenChartItem.parent as ViewGroup,
                    AutoTransition().apply {
                        duration = 200
                    }
                )

                binding.cardChosenChartItem.visibility = View.GONE

                binding.cardChosenChartItem.translationY = 0f
                binding.cardChosenChartItem.scaleX = 1f
                binding.cardChosenChartItem.scaleY = 1f
            }
            .start()
    }

    private fun animateCardContentChange(
        update: () -> Unit
    ) {

        binding.cardChosenChartItem.animate()
            .alpha(0.6f)
            .setDuration(100)
            .withEndAction {

                update()

                binding.cardChosenChartItem.animate()
                    .alpha(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun animateMinutesText(
        textView: TextView,
        fromMinutes: Int,
        toMinutes: Int
    ) {
        ValueAnimator.ofInt(
            fromMinutes,
            toMinutes
        ).apply {
            duration = 400
            addUpdateListener {
                val value = it.animatedValue as Int

                textView.text = formatMinutes(value)
            }
            start()
        }
    }
}