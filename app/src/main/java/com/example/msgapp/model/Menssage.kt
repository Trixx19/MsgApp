package com.example.msgapp.model

class Menssage {
    data class Message (
        val id: String = "",
        val senderId: String = "",
        val senderName: String = "",
        val text: String = "",
        val timestamp: Long = 0L

    )
}