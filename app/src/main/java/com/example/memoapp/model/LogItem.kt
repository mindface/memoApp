package com.example.memoapp.model

/** A locally stored log entry. */
data class LogItem(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: String,
    val updatedAt: String
)
