package com.me.gemini.presentation.screen

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.me.gemini.data.model.ChatMessage
import com.me.gemini.presentation.viewmodel.ChatViewModel
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.me.gemini.presentation.component.ChatInputRow
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel<ChatViewModel>(),
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    val chatState by viewModel.chatState.collectAsState()
    var textInput by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    val scrollState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    // Permission handling
    var showPermissionDialog by remember { mutableStateOf(false) }
    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && isRecording) {
            viewModel.startRecording()
        } else if (!isGranted) {
            showPermissionDialog = true
            isRecording = false
        }
    }

    // Auto-scroll to new messages
    LaunchedEffect(chatState.messages.size) {
        if (chatState.messages.isNotEmpty()) {
            scrollState.animateScrollToItem(chatState.messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Messages List
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = scrollState
        ) {
            items(chatState.messages) { message ->
                when (message.messageType) {
                    ChatMessage.MessageType.VOICE_INPUT -> VoiceMessageBubble(
                        message = message,  // Now properly passed
                        modifier = Modifier.padding(8.dp)
                    )
                    else -> TextMessageBubble(message)
                }
            }
        }

        // Input Row
        ChatInputRow(
            text = textInput,
            onTextChange = { textInput = it },
            onSend = {
                if (textInput.isNotBlank()) {
                    viewModel.sendMessage(textInput)
                    textInput = ""
                    focusManager.clearFocus()
                }
            },
            isRecording = isRecording,
            onRecordingToggle = {
                isRecording = !isRecording
                if (isRecording) {
                    // Check permission
                    when {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED -> {
                            viewModel.startRecording()
                        }
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            context as Activity,
                            Manifest.permission.RECORD_AUDIO
                        ) -> {
                            showPermissionDialog = true
                        }
                        else -> {
                            recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                } else {
                    viewModel.stopRecording()
                }
            }
        )

        // Permission Dialog
        if (showPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                title = { Text("Microphone Access Required") },
                text = { Text("Please grant microphone permission to record voice messages") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showPermissionDialog = false
                            recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showPermissionDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun TextMessageBubble(message: ChatMessage) {
    Box(
        modifier = Modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (message.isFromUser) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(16.dp)
    ) {
        Text(
            text = message.content,
            color = if (message.isFromUser) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun VoiceMessageBubble(
    message: ChatMessage,  // Now properly used
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel<ChatViewModel>()
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    val mediaPlayer = remember { MediaPlayer() }

    // Handle playback state
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            try {
                mediaPlayer.apply {
                    reset()
                    setDataSource(message.voiceUri ?: return@LaunchedEffect)
                    prepare()
                    start()
                    setOnCompletionListener {
                        isPlaying = false
                    }
                }
            } catch (e: IOException) {
                isPlaying = false
                viewModel.addSystemMessage("Couldn't play voice message")
            }
        } else {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
        }
    }

    // Clean up MediaPlayer
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (message.isFromUser) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = { isPlaying = !isPlaying },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (message.isFromUser) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Visual waveform (simplified - in real app use a library)
            if (isPlaying) {
                WaveformVisualizer(modifier = Modifier.weight(1f))
            } else {
                Text(
                    text = "Voice message",
                    modifier = Modifier.weight(1f),
                    color = if (message.isFromUser) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Duration indicator
            Text(
                text = "0:15", // In real app, calculate from actual duration
                modifier = Modifier.padding(start = 8.dp),
                color = if (message.isFromUser) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

// Simple waveform visualization (placeholder)
@Composable
fun WaveformVisualizer(modifier: Modifier = Modifier) {
    val amplitudes = remember { listOf(0.2f, 0.5f, 0.8f, 0.6f, 0.3f) } // Sample data

    Canvas(modifier = modifier.height(24.dp)) {
        val barWidth = size.width / amplitudes.size
        amplitudes.forEachIndexed { i, amplitude ->
            val barHeight = size.height * amplitude
            drawRect(
                color = Color.Gray.copy(alpha = 0.7f),
                topLeft = Offset(i * barWidth + 2.dp.toPx(), size.height - barHeight),
                size = Size(barWidth - 4.dp.toPx(), barHeight)
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun formatTimestamp(timestamp: Long): String {
    val now = Instant.now()
    val past = Instant.ofEpochMilli(timestamp)
    val duration = Duration.between(past, now)

    return when {
        duration.toMinutes() < 1 -> "Just now"
        duration.toHours() < 1 -> "${duration.toMinutes()} min ago"
        duration.toDays() < 1 -> "${duration.toHours()} hours ago"
        duration.toDays() < 2 -> "Yesterday"
        duration.toDays() < 7 -> "${duration.toDays()} days ago"
        else -> {
            val dateTime = LocalDateTime.ofInstant(past, ZoneId.systemDefault())
            if (dateTime.year == LocalDateTime.now().year) {
                dateTime.format(DateTimeFormatter.ofPattern("MMM d"))
            } else {
                dateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
            }
        }
    }
}