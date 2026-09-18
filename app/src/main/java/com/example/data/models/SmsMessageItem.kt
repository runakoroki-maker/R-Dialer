package com.example.data.models

data class SmsMessageItem(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val date: Long,
    val type: Int // 1 = inbox (received), 2 = sent
)
