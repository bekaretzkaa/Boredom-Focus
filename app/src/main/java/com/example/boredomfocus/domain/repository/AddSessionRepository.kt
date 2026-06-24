package com.example.boredomfocus.domain.repository

interface AddSessionRepository {

    suspend fun finishSession(
        detoxMinutes: Long,
        focusSeconds: Long,
        completed: Boolean,
        isFocusOnly: Boolean,
        streakCounted: Boolean,
    )

}