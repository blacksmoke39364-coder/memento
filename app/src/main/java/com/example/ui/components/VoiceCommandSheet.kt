package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.EvidenceConfirmed
import com.example.ui.theme.MementoDarkCard
import com.example.ui.theme.MementoSilver
import com.example.ui.theme.MementoSilverBright
import com.example.ui.theme.MementoSilverMuted
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VoiceCommandSheet(
    title: String = "Voice Command",
    subtitle: String = "Speak naturally to record or search memory",
    mode: VoiceInputMode = VoiceInputMode.RECORD_MEMORY,
    onCommandCaptured: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var transcript by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var rmsLevel by remember { mutableFloatStateOf(0f) }
    var statusMessage by remember { mutableStateOf("Initializing microphone...") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    // Pulsing animation for listening state
    val infiniteTransition = rememberInfiniteTransition(label = "voice_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.22f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Standard Android System Speech Recognizer Intent Launcher
    val systemSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val spoken = matches[0]
                transcript = spoken
                statusMessage = "Voice command captured!"
                errorMessage = null
            } else {
                statusMessage = "No speech detected. Try again or pick a suggestion."
            }
        } else {
            statusMessage = "Ready to listen. Tap the silver mic."
        }
    }

    // Audio Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startInAppListening(
                context = context,
                onListeningStarted = {
                    isListening = true
                    statusMessage = "Listening... Speak your command now"
                    errorMessage = null
                },
                onRms = { rmsLevel = it },
                onTranscriptUpdate = { partial ->
                    transcript = partial
                },
                onFinalResult = { finalResult ->
                    transcript = finalResult
                    isListening = false
                    statusMessage = "Voice command captured!"
                },
                onError = { errCode, errText ->
                    isListening = false
                    errorMessage = errText
                    statusMessage = "Tap mic to retry or launch system voice"
                },
                onRecognizerCreated = { recognizer ->
                    speechRecognizer = recognizer
                }
            )
        } else {
            errorMessage = "Microphone permission required for speech recognition."
            statusMessage = "Permission denied. You can still tap suggestions below."
        }
    }

    fun startListeningFlow() {
        errorMessage = null
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            // Check if SpeechRecognizer service is available on this Android instance
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                startInAppListening(
                    context = context,
                    onListeningStarted = {
                        isListening = true
                        statusMessage = "Listening... Speak your command now"
                        errorMessage = null
                    },
                    onRms = { rmsLevel = it },
                    onTranscriptUpdate = { partial ->
                        transcript = partial
                    },
                    onFinalResult = { finalResult ->
                        transcript = finalResult
                        isListening = false
                        statusMessage = "Voice command captured!"
                    },
                    onError = { _, errText ->
                        isListening = false
                        errorMessage = errText
                        statusMessage = "Speech recognizer stopped. Tap to retry or use system dialog."
                    },
                    onRecognizerCreated = { recognizer ->
                        speechRecognizer = recognizer
                    }
                )
            } else {
                // If in-app speech recognizer service is not present, launch standard system speech dialog
                try {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Memento...")
                    }
                    systemSpeechLauncher.launch(intent)
                } catch (e: Exception) {
                    errorMessage = "Speech service unavailable on this device. Choose a suggestion below."
                    statusMessage = "Voice recognizer unavailable"
                }
            }
        }
    }

    // Auto-start listening when bottom sheet opens
    LaunchedEffect(Unit) {
        startListeningFlow()
    }

    // Clean up SpeechRecognizer when sheet is dismissed
    DisposableEffect(Unit) {
        onDispose {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
        }
    }

    val sampleSuggestions = when (mode) {
        VoiceInputMode.RECORD_MEMORY -> listOf(
            "I kept my college ID in my black backpack.",
            "I kept my passport in drawer 2 of the bedroom table.",
            "I gave my 65W charger to Rahul.",
            "I promised Mom I would call tonight.",
            "Waiting for Amazon refund for the headphones."
        )
        VoiceInputMode.ASK_QUESTION -> listOf(
            "Where is my college ID?",
            "Where is my passport?",
            "Who has my charger?",
            "Where are my Car Keys?",
            "What did I promise Mom?"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MementoSilverBright
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MementoSilverMuted
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_voice_sheet_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MementoSilverMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Central Animated Metallic Silver Microphone Target
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        if (isListening) MementoSilver.copy(alpha = 0.22f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        width = if (isListening) 2.5.dp else 1.5.dp,
                        color = if (isListening) MementoSilverBright else MementoSilverMuted,
                        shape = CircleShape
                    )
                    .clickable {
                        if (isListening) {
                            speechRecognizer?.stopListening()
                            isListening = false
                            statusMessage = "Processing your command..."
                        } else {
                            startListeningFlow()
                        }
                    }
                    .testTag("voice_sheet_record_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isListening) "Stop listening" else "Start listening",
                    tint = if (isListening) MementoSilverBright else MementoSilver,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Status indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isListening) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = MementoSilverBright,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isListening) MementoSilverBright else MementoSilverMuted,
                    textAlign = TextAlign.Center
                )
            }

            // Error notice if applicable
            AnimatedVisibility(visible = errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFCA5A5),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Live Transcript Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "VOICE TRANSCRIPT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        color = MementoSilverMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (transcript.isNotBlank()) "“$transcript”" else "Start speaking, or tap a suggestion below...",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (transcript.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (transcript.isNotBlank()) MementoSilverBright else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons: Use Command & Launch System Dialog
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (transcript.isNotBlank()) {
                            onCommandCaptured(transcript.trim())
                            onDismiss()
                        }
                    },
                    enabled = transcript.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("apply_voice_command_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MementoSilver,
                        contentColor = Color(0xFF0F172A),
                        disabledContainerColor = MementoSilver.copy(alpha = 0.3f),
                        disabledContentColor = Color.DarkGray
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use Command", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Memento...")
                            }
                            systemSpeechLauncher.launch(intent)
                        } catch (e: Exception) {
                            errorMessage = "Android voice dialog unavailable. Please speak into the mic or tap a sample below."
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("system_voice_dialog_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("System Voice", color = MementoSilver)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Voice Suggestions
            Text(
                text = "OR TAP A TEST VOICE PHRASE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                color = MementoSilverMuted,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sampleSuggestions.forEach { suggestion ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                            .clickable {
                                transcript = suggestion
                                statusMessage = "Command selected!"
                                errorMessage = null
                            },
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MementoSilver,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

enum class VoiceInputMode {
    RECORD_MEMORY,
    ASK_QUESTION
}

private fun startInAppListening(
    context: Context,
    onListeningStarted: () -> Unit,
    onRms: (Float) -> Unit,
    onTranscriptUpdate: (String) -> Unit,
    onFinalResult: (String) -> Unit,
    onError: (Int, String) -> Unit,
    onRecognizerCreated: (SpeechRecognizer) -> Unit
) {
    try {
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        onRecognizerCreated(recognizer)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onListeningStarted()
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {
                onRms(rmsdB)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                val errorDesc = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Check mic."
                    SpeechRecognizer.ERROR_CLIENT -> "Speech recognizer client error. Tap below to retry."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "RECORD_AUDIO permission missing."
                    SpeechRecognizer.ERROR_NETWORK -> "Network issue connecting to speech service."
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network connection timed out."
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Speak clearly or tap a phrase below."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Microphone busy. Please wait a moment."
                    SpeechRecognizer.ERROR_SERVER -> "Server error from speech recognition service."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Tap microphone to speak again."
                    else -> "Speech recognition paused (code $error). Try speaking again."
                }
                onError(error, errorDesc)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onFinalResult(matches[0])
                } else {
                    onError(SpeechRecognizer.ERROR_NO_MATCH, "No speech detected. Try again.")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onTranscriptUpdate(matches[0])
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer.startListening(intent)
    } catch (e: Exception) {
        onError(0, e.localizedMessage ?: "Failed to initialize microphone.")
    }
}
