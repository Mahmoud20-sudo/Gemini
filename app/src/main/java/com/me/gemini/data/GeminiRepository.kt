package com.me.gemini.data

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import com.me.gemini.BuildConfig
import com.me.gemini.extension.parseGeminiError
import javax.inject.Inject

class GeminiRepository @Inject constructor(private val context: Context) {
    // For multimodal (text + image)
    private val visionModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
    )

    suspend fun generateResponse(prompt: String): String {
        return try {
            visionModel.generateContent(prompt).text
                ?: "Sorry, I couldn't generate a response."
        } catch (e: Exception) {
            "Error: ${e.localizedMessage ?: e.parseGeminiError()}"
        }
    }

    suspend fun processVoiceInput(prompt: String): String {
        return try {
            val response = visionModel.generateContent(prompt)
            response.text ?: "Sorry, I couldn't generate a response."
        } catch (e: Exception) {
            "Error: ${e.localizedMessage ?: e.parseGeminiError()}"
        }
    }
}