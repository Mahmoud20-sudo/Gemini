package com.me.gemini.presentation.viewmodel

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.me.gemini.data.DataLoader
import com.me.gemini.data.GeminiRepository
import com.me.gemini.data.model.ChatMessage
import com.me.gemini.extension.parseGeminiError
import com.me.gemini.helper.AudioRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val application: Application,
    private val jsonHelper: DataLoader,
    private val repository: GeminiRepository
) : ViewModel() {

    suspend fun sendMessageToJson(userMessage: String) {
        _chatState.update { state ->
            state.copy(
                messages = state.messages + ChatMessage(
                    content = userMessage,
                    isFromUser = true
                ),
                isLoading = true
            )
        }

        try {
            // First search the JSON file
            val jsonString = jsonHelper.loadJsonFromAssets("data.json")
            val jsonResults = jsonHelper.searchJson(userMessage, jsonString)

            // Create prompt for Gemini
            val prompt = """
                User asked: $userMessage
            
                Here's what I found in the knowledge base:
                $jsonResults
            
                Please provide a helpful answer combining this information with your general knowledge.
                If the information contradicts your knowledge, prioritize the knowledge base.
            """.trimIndent()

            val response = repository.generateResponse(prompt)

            _chatState.update { state ->
                state.copy(
                    messages = state.messages + ChatMessage(
                        content = response,
                        isFromUser = false,
                        lastResponseForTTS = response
                    ),
                    isLoading = false
                )
            }
        } catch (e: Exception) {
            addSystemMessage("Error: ${e.parseGeminiError()}")
            _chatState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun generateResponse(prompt: String) {
        try {
            val response = repository.generateResponse(prompt)
            _chatState.value = _chatState.value.copy(
                messages = _chatState.value.messages + ChatMessage(
                    content = response ?: "No response generated",
                    isFromUser = false
                )
            )
        } catch (e: Exception) {
            _chatState.value = _chatState.value.copy(
                messages = _chatState.value.messages + ChatMessage(
                    content = "Error: ${e.message}",
                    isFromUser = false
                )
            )
        }
    }


    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    private val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _chatState = MutableStateFlow(ChatState())
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()

    // For voice message recording
    private var currentVoiceUri: String? = null

    sealed class RecordingState {
        data object Idle : RecordingState()
        data class Recording(val filePath: String) : RecordingState()
        data class Error(val message: String) : RecordingState()
    }

    private val audioRecorder by lazy { AudioRecorder(application.applicationContext) }

    // In ViewModel:
    fun startRecording() {
        viewModelScope.launch {
            audioRecorder.startRecording()
                .onSuccess { path ->
                    _recordingState.value = RecordingState.Recording(path)
                }
                .onFailure { e ->
                    _recordingState.value = RecordingState.Error(
                        e.message ?: "Recording failed"
                    )
                    audioRecorder.cleanup()
                }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            val filePath = (recordingState.value as? RecordingState.Recording)?.filePath
            val result = runCatching {
                audioRecorder.stopRecording() // This may return Unit, but we need the file path
                filePath ?: throw IllegalStateException("Recording file path not found")
            }
            _recordingState.value = RecordingState.Idle
            handleVoiceRecording(result)
        }
    }

    fun clearTtsResponse() {
        _chatState.update { it.copy(lastResponseForTTS = null) }
    }

    // Add this new function for system messages
    fun addSystemMessage(content: String) {
        _chatState.update { state ->
            state.copy(
                messages = state.messages + ChatMessage(
                    content = content,
                    isFromUser = false,
                    messageType = ChatMessage.MessageType.SYSTEM
                )
            )
        }
    }

    fun getVoiceMessageDuration(uri: String): Long {
        return try {
            val mp = MediaPlayer().apply {
                setDataSource(uri)
                prepare()
            }
            val duration = mp.duration.toLong()
            mp.release()
            duration
        } catch (e: Exception) {
            -1L
        }
    }


    fun sendTextMessage(text: String) {
        _chatState.update { state ->
            state.copy(
                messages = state.messages + ChatMessage(
                    content = text,
                    isFromUser = true
                ),
                isLoading = true
            )
        }

        processMessage(text)
    }

    fun setVoiceMessageUri(uri: String) {
        currentVoiceUri = uri
        _chatState.update { state ->
            state.copy(
                messages = state.messages + ChatMessage(
                    content = "Voice message",
                    isFromUser = true,
                    messageType = ChatMessage.MessageType.VOICE_INPUT,
                    voiceUri = uri,
                    isProcessing = true
                ),
                isLoading = true
            )
        }

        // In a real app, you would transcribe the voice message here
        // For now, we'll simulate transcription
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000) // Simulate transcription time
            processMessage("[Transcript of voice message]")
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        _chatState.update { state ->
            state.copy(
                messages = state.messages + ChatMessage(
                    content = text,
                    isFromUser = true
                ),
                isLoading = true
            )
        }

        viewModelScope.launch {
            try {
                val response = repository.generateResponse(text)

                _chatState.update { state ->
                    state.copy(
                        messages = state.messages + ChatMessage(
                            content = response,
                            isFromUser = false,
                            lastResponseForTTS = response
                        ),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                addSystemMessage("Error: ${e.parseGeminiError()}")
                _chatState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun handleVoiceRecording(result: Result<String>) {
        result.onSuccess {
            val filePath = result.getOrNull() ?: return
            _chatState.update { state ->
                state.copy(
                    messages = state.messages + ChatMessage(
                        content = "Voice message",
                        isFromUser = true,
                        messageType = ChatMessage.MessageType.VOICE_INPUT,
                        voiceUri = filePath,
                        isProcessing = true
                    ),
                    isLoading = true
                )
            }
        }.onFailure {
            _chatState.update { state ->
                state.copy(
                    messages = state.messages + ChatMessage(
                        content = "Recording failed: ${result.exceptionOrNull()?.message ?: "Unknown error"}",
                        isFromUser = false,
                        messageType = ChatMessage.MessageType.ERROR
                    )
                )
            }
        }
    }

    // Voice chat integration
    fun processVoiceInput(text: String) {

        viewModelScope.launch {
            try {
                val response = repository.processVoiceInput(text)

                _chatState.update { state ->
                    state.copy(
                        messages = state.messages.map {
                            if (it.isProcessing) it.copy(isProcessing = false) else it
                        } + ChatMessage(
                            content = response,
                            isFromUser = false,
                            messageType = if (currentVoiceUri != null)
                                ChatMessage.MessageType.VOICE_RESPONSE
                            else ChatMessage.MessageType.TEXT
                        ),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _chatState.update { state ->
                    state.copy(
                        messages = state.messages.map {
                            if (it.isProcessing) it.copy(
                                content = "Error processing",
                                isProcessing = false
                            ) else it
                        },
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun processMessage(content: String) {
        viewModelScope.launch {
            try {
                val response = repository.processVoiceInput(content)

                _chatState.update { state ->
                    state.copy(
                        messages = state.messages.map {
                            if (it.isProcessing) it.copy(isProcessing = false) else it
                        } + ChatMessage(
                            content = response,
                            isFromUser = false,
                            messageType = if (currentVoiceUri != null)
                                ChatMessage.MessageType.VOICE_RESPONSE
                            else ChatMessage.MessageType.TEXT
                        ),
                        isLoading = false
                    )
                }
                currentVoiceUri = null
            } catch (e: Exception) {
                _chatState.update { state ->
                    state.copy(
                        messages = state.messages.map {
                            if (it.isProcessing) it.copy(
                                content = "Error processing",
                                isProcessing = false
                            ) else it
                        },
                        isLoading = false
                    )
                }
            }
        }
    }
}

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val lastResponseForTTS: String? = null  // Track last response for TTS
)