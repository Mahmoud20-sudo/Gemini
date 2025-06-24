package com.me.gemini.helper

// AudioRecorder.kt
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.IOException

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    var outputFile: File? = null

    fun startRecording(): Result<String> {
        return try {
            outputFile = File(
                context.cacheDir,
                "recording_${System.currentTimeMillis()}.mp4"
            ).apply { createNewFile() }

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile?.absolutePath)
                prepare()
                start()
            }
            Result.success(outputFile?.absolutePath ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun stopRecording() {
        try {
            recorder?.stop()
        } catch (e: Exception) {
            // Handle cases where stop is called without valid recording
        } finally {
            recorder?.release()
            recorder = null
        }
    }

    fun cleanup() {
        try {
            outputFile?.delete()
        } catch (e: Exception) {
            // Handle file deletion errors
        }
    }
}