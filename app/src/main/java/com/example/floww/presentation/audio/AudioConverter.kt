package com.example.floww.presentation.audio

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream

object AudioConverter {

    fun convertPcmToWav(pcmFile: File, config: AudioConfig, outputDir: File): File {
        return File(outputDir, "audio_record.wav").also { wavFile ->
            FileInputStream(pcmFile).use { input ->
                FileOutputStream(wavFile).use { output ->
                    writeWavHeader(output, pcmFile.length(), config)
                    input.copyTo(output)
                }
            }
        }
    }

    private fun writeWavHeader(out: OutputStream, audioLen: Long, config: AudioConfig) {
        val header = ByteArray(44)

        "RIFF".toByteArray().copyInto(header, 0)
        writeInt(header, 4, (audioLen + 36).toInt())
        "WAVEfmt ".toByteArray().copyInto(header, 8)
        writeInt(header, 16, 16)
        writeShort(header, 20, 1)
        writeShort(header, 22, 1)
        writeInt(header, 24, config.sampleRate)
        writeInt(header, 28, config.sampleRate * 2)
        writeShort(header, 32, 2)
        writeShort(header, 34, 16)
        "data".toByteArray().copyInto(header, 36)
        writeInt(header, 40, audioLen.toInt())

        out.write(header)
    }

    private fun writeInt(header: ByteArray, offset: Int, value: Int) {
        header[offset] = (value and 0xFF).toByte()
        header[offset + 1] = (value shr 8 and 0xFF).toByte()
        header[offset + 2] = (value shr 16 and 0xFF).toByte()
        header[offset + 3] = (value shr 24 and 0xFF).toByte()
    }

    private fun writeShort(header: ByteArray, offset: Int, value: Int) {
        header[offset] = (value and 0xFF).toByte()
        header[offset + 1] = (value shr 8 and 0xFF).toByte()
    }
}