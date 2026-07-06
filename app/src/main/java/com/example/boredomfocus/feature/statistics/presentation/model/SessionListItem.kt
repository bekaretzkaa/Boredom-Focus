package com.example.boredomfocus.feature.statistics.presentation.model

sealed class SessionListItem {

    data class Header(
        val date: String
    ) : SessionListItem()

    data class Session(
        val detoxSelected: Int,
        val detoxTime: Int,
        val focusTime: Int,
        val time: String,
        val completed: Boolean
    ) : SessionListItem()

}