package com.example.boredomfocus.feature.statistics.presentation.model

sealed class SessionListItem {

    data class Header(
        val date: String
    ) : SessionListItem()

    data class Session(
        val detoxTime: Int,
        val focusTime: Int
    ) : SessionListItem()

}