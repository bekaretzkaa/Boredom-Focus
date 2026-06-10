package com.example.boredomfocus

sealed class SessionListItem {

    data class Header(
        val date: String
    ) : SessionListItem()

    data class Session(
        val detoxTime: Int,
        val focusTime: Int
    ) : SessionListItem()

}