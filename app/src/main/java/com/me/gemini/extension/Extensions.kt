package com.me.gemini.extension

// Add this to your ViewModel file or a separate ErrorHandlers.kt file
fun Exception.parseGeminiError(): String {
    return when {
        // Handle specific Gemini API errors
        this.message?.contains("404") == true -> "The requested AI model was not found"
        this.message?.contains("429") == true -> "Too many requests - please wait"
        this.message?.contains("401") == true -> "Invalid API key - please check your configuration"
        this.message?.contains("quota", ignoreCase = true) == true -> "API quota exceeded"

        // Handle network errors
        this is java.net.UnknownHostException -> "No internet connection"
        this is java.net.SocketTimeoutException -> "Request timed out"

        // Default case
        else -> "An error occurred: ${this.message ?: "Unknown error"}"
    }
}