package com.example.floww.presentation


import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun TranscriptionScreen(
    onStartTranscription: () -> Unit,
    onPauseTranscription: () -> Unit,
    onStopTranscription: () -> Unit,
    onTranslate: () -> Unit,
    transcribedText: String
) {
    var showTextScreen by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }

    // Detectar swipe
    val gestureModifier = Modifier.pointerInput(Unit) {
        detectHorizontalDragGestures { _, dragAmount ->
            if (dragAmount < -30) {
                showTextScreen = true
            } else if (dragAmount > 30) {
                showTextScreen = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(gestureModifier)
            .background(Color.White)
            .padding(12.dp)
    ) {
        if (showTextScreen) {
            // Pantalla de texto transcrito
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = transcribedText.ifBlank { "No hay transcripción aún." },
                    textAlign = TextAlign.Center,
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            // Pantalla de controles
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        isPaused = false
                        onStartTranscription()
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(34.dp)
                ) {
                    Text("Tomar nota", style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        isPaused = false
                        onTranslate()
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(34.dp)
                ) {
                    Text("Traducir", style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = when {
                        isPaused -> "Pausado"
                        transcribedText.isNotBlank() -> "Grabando..."
                        else -> ""
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isPaused) Color.Gray else MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            isPaused = !isPaused
                            onPauseTranscription()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "Reanudar transcripción" else "Pausar transcripción",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            isPaused = false
                            onStopTranscription()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Detener transcripción",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
