package com.example.floww.presentation

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File
import java.io.FileOutputStream

class VoskRecognizerManager(
    private val onCommandDetected: (String) -> Unit,
    private val onError: (String) -> Unit
) {

    private var model: Model? = null
    private var speechService: SpeechService? = null

    fun initialize(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val modelPath = copyAssetFolderToInternalStorage(
                    context,
                    "model-es/vosk-model-small-es-0.42"
                )
                model = Model(modelPath.absolutePath)
                startListening()
            } catch (e: Exception) {
                onError("Error cargando modelo Vosk: ${e.message}")
            }
        }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onPartialResult(hypothesis: String?) {}

        override fun onResult(hypothesis: String?) {
            val command = JSONObject(hypothesis ?: "{}").optString("text", "").lowercase()
            if (command in listOf("iniciar", "pausar", "detener", "traducir","deslizar","volver","controles")) {
                onCommandDetected(command)
            }
        }

        override fun onFinalResult(hypothesis: String?) {}
        override fun onError(e: Exception?) {
            onError("Error Vosk: ${e?.message}")
        }

        override fun onTimeout() {}
    }

    private fun startListening() {
        val recognizer = Recognizer(model, 16000f)
        speechService = SpeechService(recognizer, 16000.0f)
        speechService?.startListening(recognitionListener)
    }

    fun stop() {
        speechService?.stop()

    }

    private fun copyAssetFolderToInternalStorage(context: Context, assetFolderName: String): File {
        val outputDir = File(context.filesDir, assetFolderName)
        if (outputDir.exists()) return outputDir

        outputDir.mkdirs()
        val assetManager = context.assets
        val files = assetManager.list(assetFolderName) ?: return outputDir

        for (filename in files) {
            val fullPath = "$assetFolderName/$filename"
            val subFiles = assetManager.list(fullPath)

            if (subFiles?.isNotEmpty() == true) {

                copyAssetFolderToInternalStorage(context, fullPath)
            } else {

                val inputStream = assetManager.open(fullPath)
                val outFile = File(context.filesDir, fullPath)
                outFile.parentFile?.mkdirs()
                val outputStream = FileOutputStream(outFile)

                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()
            }
        }

        return outputDir
    }
}
