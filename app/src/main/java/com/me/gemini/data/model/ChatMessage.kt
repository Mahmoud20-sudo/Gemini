package com.me.gemini.data.model

import java.util.UUID

// Updated ChatMessage class
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val messageType: MessageType = MessageType.TEXT,
    val voiceUri: String? = null,
    val isProcessing: Boolean = false,
    val lastResponseForTTS: String? = null,
    val voiceDuration: Long = 0L  // Duration in milliseconds
) {
    enum class MessageType {
        TEXT,
        VOICE_INPUT,
        VOICE_RESPONSE,
        ERROR,
        SYSTEM  // Added system message type

    }
}