package com.example.data.models

data class SmsConversation(
    val threadId: Long,
    val address: String,
    val snippet: String,
    val date: Long,
    val read: Boolean,
    val contactName: String?
)
