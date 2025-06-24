package com.me.gemini.presentation.screen

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.me.gemini.helper.AudioRecorder
import com.me.gemini.presentation.viewmodel.ChatViewModel.RecordingState
import kotlinx.coroutines.launch

@Composable
fun VoiceRecorder(
    onRecordingComplete: (Result<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val audioRecorder = remember { AudioRecorder(context) }
    val scope = rememberCoroutineScope()

    // Error dialog
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Recording Error") },
            text = { Text(errorMessage ?: "Unknown error") },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isRecording) {
                audioRecorder.stopRecording()
            }
            audioRecorder.cleanup()
        }
    }

    IconButton(
        onClick = {
            isRecording = !isRecording
            scope.launch {
                if (isRecording) {
                    audioRecorder.startRecording()
                        .onSuccess { path ->
                            if (path.isEmpty()) {
                                errorMessage = "Recording failed to start"
                                isRecording = false
                            }
                        }
                        .onFailure { e ->
                            errorMessage = e.message
                            isRecording = false
                            audioRecorder.cleanup()
                        }

                } else {
                    audioRecorder.stopRecording()
                    onRecordingComplete(Result.success(audioRecorder.outputFile?.absolutePath ?: ""))
                }
            }
        },
        modifier = modifier
    ) {
        Icon(
            imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
            contentDescription = if (isRecording) "Stop recording" else "Start recording",
            tint = if (isRecording) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary
        )
    }
}