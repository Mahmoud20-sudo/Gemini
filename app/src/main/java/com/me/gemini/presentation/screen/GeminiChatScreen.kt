package com.me.gemini.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.me.gemini.helper.VoiceChatHandler
import com.me.gemini.presentation.component.MessageBubble
import com.me.gemini.presentation.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@Composable
fun GeminiChatScreen(
    viewModel: ChatViewModel = hiltViewModel<ChatViewModel>(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val chatState by viewModel.chatState.collectAsState()
    val focusManager = LocalFocusManager.current
    var messageText by remember { mutableStateOf("") }

    val voiceChatHandler = remember {
        VoiceChatHandler(
            context = context,
            onTextReceived = { text ->
                messageText = text
            },
            onError = { error ->
                viewModel.sendMessage("Voice error: $error")
            }
        )
    }

    // Handle TTS responses
    LaunchedEffect(chatState.lastResponseForTTS) {
        chatState.lastResponseForTTS?.let { response ->
            voiceChatHandler.speak(response)
            viewModel.clearTtsResponse()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Message list
        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = true
        ) {
            items(chatState.messages.reversed()) { message ->
                MessageBubble(message)
            }
        }

        // Input area
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        scope.launch {
                            viewModel.sendMessageToJson(messageText)
                        }
                        focusManager.clearFocus()
                    }
                )
            )

            IconButton(
                onClick = { voiceChatHandler.startListening() }
            ) {
                Icon(Icons.Default.Mic, "Voice input")
            }

            IconButton(
                onClick = {
                    viewModel.processVoiceInput(messageText)
                }
            ) {
                Icon(Icons.Default.Check, "Stop")
            }
        }
    }
}