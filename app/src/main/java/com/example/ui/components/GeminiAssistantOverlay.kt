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
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.EvidenceLevel
import com.example.data.model.SourceType
import com.example.data.remote.MemoryRetrievalResult
import com.example.ui.theme.EvidenceConfirmed
import com.example.ui.theme.EvidenceInferred
import com.example.ui.theme.EvidenceObserved
import com.example.ui.theme.MementoSilver
import com.example.ui.theme.MementoSilverBright
import com.example.ui.theme.MementoSilverMuted
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GeminiAssistantOverlay(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSaveMemory: (rawInput: String, sourceType: SourceType) -> Unit,
    onAskQuestion: suspend (String) -> MemoryRetrievalResult,
    initialQuery: String = ""
) {
    if (!isOpen) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var modeIndex by remember { mutableIntStateOf(0) } // 0: Auto/Remember, 1: Ask Memento
    var speechText by remember { mutableStateOf(initialQuery) }
    var isListening by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Gemini Voice Ready. Tap mic or speak.") }
    var resultCard by remember { mutableStateOf<MemoryRetrievalResult?>(null) }
    var confirmationNotice by remember { mutableStateOf<String?>(null) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var rmsLevel by remember { mutableFloatStateOf(0f) }

    // Shimmering ambient gradient animation for the Gemini light bar
    val infiniteTransition = rememberInfiniteTransition(label = "gemini_ambient")
    val ambientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambient_gradient"
    )

    val waveScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_scale"
    )

    // Android Native System Speech Launcher (Fallback)
    val systemSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val captured = matches[0]
                speechText = captured
                statusText = "Voice captured! Processing..."
                processVoiceCommand(
                    input = captured,
                    mode = modeIndex,
                    coroutineScope = coroutineScope,
                    onSaveMemory = onSaveMemory,
                    onAskQuestion = onAskQuestion,
                    onProcessingState = { isProcessing = it },
                    onResultSet = { resultCard = it },
                    onConfirmNotice = { confirmationNotice = it },
                    onStatusUpdate = { statusText = it }
                )
            }
        }
    }

    // Microphone Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startInAppSpeechEngine(
                context = context,
                onStarted = {
                    isListening = true
                    statusText = "Listening live... Speak clearly"
                },
                onRms = { rmsLevel = it },
                onPartial = { speechText = it },
                onResult = { finalResult ->
                    isListening = false
                    speechText = finalResult
                    processVoiceCommand(
                        input = finalResult,
                        mode = modeIndex,
                        coroutineScope = coroutineScope,
                        onSaveMemory = onSaveMemory,
                        onAskQuestion = onAskQuestion,
                        onProcessingState = { isProcessing = it },
                        onResultSet = { resultCard = it },
                        onConfirmNotice = { confirmationNotice = it },
                        onStatusUpdate = { statusText = it }
                    )
                },
                onError = { _, msg ->
                    isListening = false
                    statusText = msg
                },
                onRecognizerReady = { speechRecognizer = it }
            )
        } else {
            statusText = "Mic permission needed. You can type or tap a sample."
        }
    }

    fun startListening() {
        resultCard = null
        confirmationNotice = null
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                startInAppSpeechEngine(
                    context = context,
                    onStarted = {
                        isListening = true
                        statusText = "Listening live... Speak clearly"
                    },
                    onRms = { rmsLevel = it },
                    onPartial = { speechText = it },
                    onResult = { finalResult ->
                        isListening = false
                        speechText = finalResult
                        processVoiceCommand(
                            input = finalResult,
                            mode = modeIndex,
                            coroutineScope = coroutineScope,
                            onSaveMemory = onSaveMemory,
                            onAskQuestion = onAskQuestion,
                            onProcessingState = { isProcessing = it },
                            onResultSet = { resultCard = it },
                            onConfirmNotice = { confirmationNotice = it },
                            onStatusUpdate = { statusText = it }
                        )
                    },
                    onError = { _, msg ->
                        isListening = false
                        statusText = msg
                    },
                    onRecognizerReady = { speechRecognizer = it }
                )
            } else {
                // Launch Google System Voice dialog
                try {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Memento (Gemini)...")
                    }
                    systemSpeechLauncher.launch(intent)
                } catch (e: Exception) {
                    statusText = "Speech recognizer unavailable. Type or tap below."
                }
            }
        }
    }

    // Auto-start microphone when opened
    LaunchedEffect(Unit) {
        startListening()
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F141E),
        dragHandle = null
    ) {
        // Metallic Gemini Light Halo Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MementoSilverBright,
                            Color(0xFFA0AEC0),
                            Color(0xFFE2E8F0),
                            Color(0xFF64748B)
                        ),
                        start = androidx.compose.ui.geometry.Offset(ambientOffset % 800f, 0f),
                        end = androidx.compose.ui.geometry.Offset((ambientOffset % 800f) + 400f, 400f)
                    ),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Gemini Brand Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MementoSilver.copy(alpha = 0.15f))
                                .border(1.dp, MementoSilverBright.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Gemini",
                                tint = MementoSilverBright,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "MEMENTO • GEMINI AI",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp,
                                color = MementoSilverBright
                            )
                            Text(
                                text = "Local Room Vault persistence & instant voice",
                                style = MaterialTheme.typography.labelSmall,
                                color = MementoSilverMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("dismiss_gemini_overlay_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MementoSilverMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mode Tabs: Auto / Remember vs Ask
                TabRow(
                    selectedTabIndex = modeIndex,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MementoSilver,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = modeIndex == 0,
                        onClick = { modeIndex = 0 },
                        text = { Text("Record Memory", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = modeIndex == 1,
                        onClick = { modeIndex = 1 },
                        text = { Text("Ask Question", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Gemini Live Audio Waveform Circle
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .scale(waveScale)
                        .clip(CircleShape)
                        .background(
                            if (isListening) Brush.radialGradient(
                                colors = listOf(MementoSilver.copy(alpha = 0.35f), Color.Transparent)
                            ) else Brush.radialGradient(
                                colors = listOf(MaterialTheme.colorScheme.surfaceVariant, Color.Transparent)
                            )
                        )
                        .border(
                            width = if (isListening) 2.dp else 1.dp,
                            color = if (isListening) MementoSilverBright else MementoSilverMuted,
                            shape = CircleShape
                        )
                        .clickable {
                            if (isListening) {
                                speechRecognizer?.stopListening()
                                isListening = false
                                if (speechText.isNotBlank()) {
                                    processVoiceCommand(
                                        input = speechText,
                                        mode = modeIndex,
                                        coroutineScope = coroutineScope,
                                        onSaveMemory = onSaveMemory,
                                        onAskQuestion = onAskQuestion,
                                        onProcessingState = { isProcessing = it },
                                        onResultSet = { resultCard = it },
                                        onConfirmNotice = { confirmationNotice = it },
                                        onStatusUpdate = { statusText = it }
                                    )
                                }
                            } else {
                                startListening()
                            }
                        }
                        .testTag("gemini_live_mic_circle"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = MementoSilverBright,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Mic",
                            tint = if (isListening) MementoSilverBright else MementoSilver,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Live Status Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isListening) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = MementoSilverBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isListening) MementoSilverBright else MementoSilverMuted,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Live Input Field / Editable Transcript
                OutlinedTextField(
                    value = speechText,
                    onValueChange = { speechText = it },
                    placeholder = {
                        Text(
                            text = if (modeIndex == 0) "e.g. I kept my college ID in my black backpack" else "e.g. Where is my college ID?",
                            color = MementoSilverMuted,
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gemini_voice_input_field"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MementoSilverBright,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedTextColor = MementoSilverBright,
                        unfocusedTextColor = MementoSilver
                    ),
                    maxLines = 2,
                    trailingIcon = {
                        if (speechText.isNotBlank() && !isProcessing) {
                            IconButton(
                                onClick = {
                                    processVoiceCommand(
                                        input = speechText,
                                        mode = modeIndex,
                                        coroutineScope = coroutineScope,
                                        onSaveMemory = onSaveMemory,
                                        onAskQuestion = onAskQuestion,
                                        onProcessingState = { isProcessing = it },
                                        onResultSet = { resultCard = it },
                                        onConfirmNotice = { confirmationNotice = it },
                                        onStatusUpdate = { statusText = it }
                                    )
                                },
                                modifier = Modifier.testTag("gemini_submit_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = MementoSilverBright
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (speechText.isNotBlank()) {
                            processVoiceCommand(
                                input = speechText,
                                mode = modeIndex,
                                coroutineScope = coroutineScope,
                                onSaveMemory = onSaveMemory,
                                onAskQuestion = onAskQuestion,
                                onProcessingState = { isProcessing = it },
                                onResultSet = { resultCard = it },
                                onConfirmNotice = { confirmationNotice = it },
                                onStatusUpdate = { statusText = it }
                            )
                        }
                    })
                )

                // Confirmation / Result Banner
                AnimatedVisibility(visible = confirmationNotice != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(EvidenceConfirmed.copy(alpha = 0.15f))
                            .border(1.dp, EvidenceConfirmed.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = EvidenceConfirmed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Saved to Local Room Vault",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EvidenceConfirmed
                                )
                                Text(
                                    text = confirmationNotice ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MementoSilverBright
                                )
                            }
                        }
                    }
                }

                // Question Answer Card
                resultCard?.let { res ->
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, MementoSilverBright.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = res.entity.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.2.sp,
                                    color = MementoSilverMuted
                                )
                                EvidenceBadge(evidenceLevel = res.evidenceLevel)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = res.answer,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MementoSilverBright
                            )

                            res.location?.let { loc ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Location: $loc",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MementoSilver
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = res.howIKnow,
                                style = MaterialTheme.typography.labelSmall,
                                color = MementoSilverMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Fast Quick Voice Prompts
                Text(
                    text = "TRY ONE-TAP VOICE COMMANDS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MementoSilverMuted,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                val suggestions = if (modeIndex == 0) listOf(
                    "I kept my college ID in my black backpack",
                    "I kept my passport in drawer 2",
                    "I gave my charger to Rahul",
                    "I placed my Car Keys on the hallway hook",
                    "Promised Mom I will call tonight"
                ) else listOf(
                    "Where is my college ID?",
                    "Where is my passport?",
                    "Who has my charger?",
                    "Where are my Car Keys?",
                    "What did I promise Mom?"
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestions.forEach { prompt ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                                .clickable {
                                    speechText = prompt
                                    processVoiceCommand(
                                        input = prompt,
                                        mode = modeIndex,
                                        coroutineScope = coroutineScope,
                                        onSaveMemory = onSaveMemory,
                                        onAskQuestion = onAskQuestion,
                                        onProcessingState = { isProcessing = it },
                                        onResultSet = { resultCard = it },
                                        onConfirmNotice = { confirmationNotice = it },
                                        onStatusUpdate = { statusText = it }
                                    )
                                },
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = prompt,
                                style = MaterialTheme.typography.bodySmall,
                                color = MementoSilver,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hardware Power Button Notice
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MementoSilverMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tip: Press Power Button / Headset key anytime to summon this mic",
                        style = MaterialTheme.typography.labelSmall,
                        color = MementoSilverMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

private fun processVoiceCommand(
    input: String,
    mode: Int,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onSaveMemory: (String, SourceType) -> Unit,
    onAskQuestion: suspend (String) -> MemoryRetrievalResult,
    onProcessingState: (Boolean) -> Unit,
    onResultSet: (MemoryRetrievalResult?) -> Unit,
    onConfirmNotice: (String?) -> Unit,
    onStatusUpdate: (String) -> Unit
) {
    if (input.isBlank()) return
    onProcessingState(true)
    onStatusUpdate("Processing with Gemini AI...")

    coroutineScope.launch {
        try {
            // Determine if question or statement
            val isQuestion = mode == 1 ||
                    input.endsWith("?") ||
                    input.startsWith("where", ignoreCase = true) ||
                    input.startsWith("who", ignoreCase = true) ||
                    input.startsWith("what", ignoreCase = true) ||
                    input.startsWith("when", ignoreCase = true) ||
                    input.startsWith("did i", ignoreCase = true)

            if (isQuestion) {
                val res = onAskQuestion(input)
                onResultSet(res)
                onConfirmNotice(null)
                onStatusUpdate("Found memory in Room database!")
            } else {
                onSaveMemory(input, SourceType.VOICE)
                onConfirmNotice("“$input”")
                onResultSet(null)
                onStatusUpdate("Recorded into local Room database!")
            }
        } catch (e: Exception) {
            onStatusUpdate("Failed: ${e.localizedMessage}")
        } finally {
            onProcessingState(false)
        }
    }
}

private fun startInAppSpeechEngine(
    context: Context,
    onStarted: () -> Unit,
    onRms: (Float) -> Unit,
    onPartial: (String) -> Unit,
    onResult: (String) -> Unit,
    onError: (Int, String) -> Unit,
    onRecognizerReady: (SpeechRecognizer) -> Unit
) {
    try {
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        onRecognizerReady(recognizer)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { onStarted() }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) { onRms(rmsdB) }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording issue. Tap mic to retry."
                    SpeechRecognizer.ERROR_CLIENT -> "Speech recognizer ready. Speak now or tap prompt."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mic permission needed."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network required for online speech. You can select a test prompt."
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Speak clearly into mic."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Mic busy, resetting..."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard. Tap mic to speak."
                    else -> "Speech paused. Tap mic to speak."
                }
                onError(error, message)
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onResult(matches[0])
                } else {
                    onError(SpeechRecognizer.ERROR_NO_MATCH, "No words heard.")
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onPartial(matches[0])
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer.startListening(intent)
    } catch (e: Exception) {
        onError(0, e.localizedMessage ?: "Could not open mic.")
    }
}
