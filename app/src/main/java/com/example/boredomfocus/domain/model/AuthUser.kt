package com.example.boredomfocus.domain.model

data class AuthUser(
    val uid: String,
    val name: String?,
    val email: String?
)