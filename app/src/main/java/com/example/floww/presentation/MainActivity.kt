/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.example.floww.presentation
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

import androidx.core.content.ContextCompat
import com.example.floww.presentation.audio.AudioConfig
import com.example.floww.presentation.audio.AudioConverter
import com.example.floww.presentation.audio.AudioRecorder
import com.example.floww.presentation.red.Backend
import com.example.floww.presentation.theme.AppTheme
import com.example.floww.presentation.translation.Translator
import kotlinx.coroutines.*
class MainActivity : ComponentActivity() {

    private lateinit var audioRecorder: AudioRecorder
    private lateinit var translatorHelper: Translator
    private lateinit var backendUploader: Backend
    private lateinit var voskRecognizer: VoskRecognizerManager

    private var isRecording = false
    private var isPaused = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) showToast("Permiso de micrófono denegado")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        translatorHelper = Translator(this)
        backendUploader = Backend(this)
        audioRecorder = AudioRecorder(cacheDir)

        requestAudioPermission()
        translatorHelper.prepareTranslator()


        val translatedText = mutableStateOf("")
        val showTextScreen = mutableStateOf(false)
        setContent {
            AppTheme {
                TranscriptionScreen(
                    onStartTranscription = {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                            == PackageManager.PERMISSION_GRANTED
                        ) {
                            startRecording(translatedText)
                        } else {
                            showToast("Permiso de micrófono no otorgado.")
                        }
                    },
                    onTranslate = {
                        translateLastTranscription(translatedText)
                    },
                    onPauseTranscription = ::togglePauseRecording,
                    onStopTranscription = ::stopRecording,
                    transcribedText = translatedText.value,
                    showTextScreen = showTextScreen,
                )
            }
        }

        //  Vosk Integration
        voskRecognizer = VoskRecognizerManager(
            onCommandDetected = { command ->
                runOnUiThread {
                    when (command) {
                        "iniciar" -> {
                            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                                == PackageManager.PERMISSION_GRANTED
                            ) {
                                startRecording(translatedText)
                            } else {
                                showToast("Permiso de micrófono no otorgado.")
                            }
                        }
                        "pausar" -> togglePauseRecording()
                        "detener" -> stopRecording()
                        "traducir" -> translateLastTranscription(translatedText)
                        "deslizar" -> {
                            showTextScreen.value = true
                        }
                        "volver", "controles" -> {
                            showTextScreen.value = false
                        }
                    }
                }
            },
            onError = { errorMsg ->
                runOnUiThread {
                    showToast(errorMsg)
                }
            }
        ).also {
            it.initialize(this)
        }
    }

    private fun requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startRecording(translatedText: MutableState<String>) {
        if (isRecording) return

        isRecording = true
        isPaused = false
        showToast("Grabación iniciada")

        audioRecorder.start { pcmFile ->
            val wavFile = AudioConverter.convertPcmToWav(pcmFile, AudioConfig(), cacheDir)
            CoroutineScope(Dispatchers.IO).launch {
                backendUploader.uploadAndSave(wavFile, filesDir) { result ->
                    translatedText.value = result
                }
            }
        }
    }

    private fun stopRecording() {
        isRecording = false
        audioRecorder.stop()
        showToast("Grabación detenida")
    }

    private fun togglePauseRecording() {
        if (isRecording) {
            isPaused = !isPaused
            if (isPaused) audioRecorder.pause() else audioRecorder.resume()
            showToast(if (isPaused) "Grabación pausada" else "Grabación reanudada")
        }
    }

    private fun translateLastTranscription(translatedText: MutableState<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            translatorHelper.translateLastTranscription(filesDir) {
                translatedText.value = it
            }
        }
    }

    private fun showToast(msg: String) {
        runOnUiThread {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioRecorder.stop()
        voskRecognizer.stop()
    }
}