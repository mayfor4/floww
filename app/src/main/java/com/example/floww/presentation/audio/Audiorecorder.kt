package com.example.floww.presentation.audio

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.*
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

data class AudioConfig(
    val sampleRate: Int = 16000,
    val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    val encoding: Int = AudioFormat.ENCODING_PCM_16BIT,
    val bufferSize: Int = AudioRecord.getMinBufferSize(16000,
        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
)

class AudioRecorder(
    private val cacheDir: File,
    private val config: AudioConfig = AudioConfig()
) {
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var isPaused = false

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(onFinish: suspend (File) -> Unit) {
        val pcmFile = File(cacheDir, "audio_record.pcm").also { it.deleteOnExit() }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            config.sampleRate,
            config.channelConfig,
            config.encoding,
            config.bufferSize
        ).apply { startRecording() }

        recordingJob = CoroutineScope(Dispatchers.IO).launch {
            BufferedOutputStream(FileOutputStream(pcmFile)).use { output ->
                val buffer = ByteArray(config.bufferSize)

                while (isActive) {
                    if (isPaused) {
                        delay(100)
                        continue
                    }

                    val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readBytes > 0) {
                        output.write(buffer, 0, readBytes)
                    }
                }
            }

            onFinish(pcmFile)

            audioRecord?.release()
            audioRecord = null
        }
    }

    fun pause() {
        isPaused = true
    }

    fun resume() {
        isPaused = false
    }

    fun stop() {
        recordingJob?.cancel()
    }
}