package com.example.couchbaseassignment.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class Note(
    val id: Int,
    val title: String,
    val content: String,
    val createdAt: Instant
) {
    fun getFormattedDate(): String {
        val localDateTime = createdAt.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${localDateTime.date}"
    }
} 