package com.example.floww.presentation.translation

import android.content.Context
import android.widget.Toast
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import org.json.JSONObject
import java.io.File

class Translator(private val context: Context) {

    fun translateLastTranscription(filesDir: File, onTranslated: (String) -> Unit) {
        val lastFile = filesDir.listFiles()
            ?.filter { it.name.endsWith(".json") }
            ?.maxByOrNull { it.lastModified() }
            ?: return

        try {
            val json = JSONObject(lastFile.readText())
            val text = json.optString("texto", "")
            val sourceLang = json.optString("idioma", "en")

            if (text.isBlank()) {
                showToast("No hay texto para traducir")
                return
            }

            val sourceMlKitLang = when (sourceLang) {
                "en" -> TranslateLanguage.ENGLISH
                "ja" -> TranslateLanguage.JAPANESE
                else -> TranslateLanguage.ENGLISH
            }

            val translator = Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(sourceMlKitLang)
                    .setTargetLanguage(TranslateLanguage.SPANISH)
                    .build()
            )

            translator.downloadModelIfNeeded(DownloadConditions.Builder().build())
                .addOnSuccessListener {
                    translator.translate(text)
                        .addOnSuccessListener { translated -> onTranslated(translated) }
                        .addOnFailureListener { showToast("Error traduciendo") }
                }
                .addOnFailureListener {
                    showToast("Error descargando modelo de traducción")
                }

        } catch (e: Exception) {
            showToast("Error leyendo archivo")
        }
    }

    fun prepareTranslator() {
        val translator = Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.SPANISH)
                .build()
        )
        translator.downloadModelIfNeeded(DownloadConditions.Builder().build())
            .addOnFailureListener {
                showToast("Error preparando traductor")
            }
    }

    private fun showToast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}