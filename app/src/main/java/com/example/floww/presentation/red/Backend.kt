package com.example.floww.presentation.red
import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Backend(private val context: Context) {

    suspend fun uploadAndSave(file: File, filesDir: File, onResult: (String) -> Unit) {
        try {
            val response = URL("http://192.168.0.139:4000/whisper-transcribe/")
                .openConnection().run {
                this as HttpURLConnection
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "audio/wav")
                file.inputStream().use { it.copyTo(outputStream) }
                inputStream.bufferedReader().use { it.readText() }
            }

            saveTranscription(response, filesDir, onResult)

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun saveTranscription(response: String, filesDir:
    File, onResult: (String) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject(response)
                val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                    .format(Date())
                val file = File(filesDir, "transcripcion_$timestamp.json")
                file.writeText(json.toString(4))
                withContext(Dispatchers.Main) {
                    onResult(json.optString("texto", ""))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error al guardar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}