package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EvidenceLevel
import com.example.data.remote.MemoryRetrievalResult
import com.example.ui.components.EvidenceBadge
import com.example.ui.components.HowIKnowDialog
import com.example.ui.components.VoiceCommandSheet
import com.example.ui.components.VoiceInputMode
import com.example.ui.theme.EvidenceConfirmed
import com.example.ui.theme.EvidenceInferred
import com.example.ui.theme.MementoCyan
import com.example.util.rememberTextToSpeechManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AskMementoScreen(
    onAsk: suspend (String) -> MemoryRetrievalResult,
    onCorrectLocation: (memoryId: String, newLoc: String) -> Unit,
    onDeleteMemory: (memoryId: String) -> Unit,
    onNavigateToTimeline: (entityTitle: String, memoryId: String) -> Unit
) {
    val ttsManager = rememberTextToSpeechManager()
    val coroutineScope = rememberCoroutineScope()
    var queryText by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var showVoiceSheet by remember { mutableStateOf(false) }
    var currentResult by remember { mutableStateOf<MemoryRetrievalResult?>(null) }
    var lastQueriedText by remember { mutableStateOf("") }

    var showCorrectDialog by remember { mutableStateOf(false) }
    var newLocationInput by remember { mutableStateOf("") }
    var showHowIKnow by remember { mutableStateOf(false) }
    var showConfirmationSuccess by remember { mutableStateOf(false) }

    val sampleQueries = listOf(
        "Where is my passport?",
        "Where is my college ID?",
        "Who has my charger?",
        "What did I promise Mom?",
        "What am I waiting for?",
        "Where are my Car Keys?",
        "Where is my college ID?"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = "Ask Memento",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Natural language retrieval with verified evidence. Never hallucinated.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Query Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    placeholder = {
                        Text(
                            text = "Ask anything you told Memento...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ask_memento_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    maxLines = 2
                )

                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .padding(2.dp),
                        strokeWidth = 2.dp,
                        color = MementoCyan
                    )
                } else {
                    IconButton(
                        onClick = { showVoiceSheet = true },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("ask_voice_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Ask with voice",
                            tint = MementoCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = {
                            if (queryText.isNotBlank()) {
                                isSearching = true
                                lastQueriedText = queryText
                                coroutineScope.launch {
                                    currentResult = onAsk(queryText)
                                    isSearching = false
                                    showConfirmationSuccess = false
                                }
                            }
                        },
                        enabled = queryText.isNotBlank(),
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (queryText.isNotBlank()) MementoCyan else Color.Transparent)
                            .testTag("ask_submit_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Ask",
                            tint = if (queryText.isNotBlank()) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Suggested Queries
            Text(
                text = "Natural retrieval examples:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sampleQueries.forEach { sampleQ ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                            .clickable {
                                queryText = sampleQ
                                lastQueriedText = sampleQ
                                isSearching = true
                                coroutineScope.launch {
                                    currentResult = onAsk(sampleQ)
                                    isSearching = false
                                    showConfirmationSuccess = false
                                }
                            },
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = sampleQ,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // RESULT CARD
            currentResult?.let { result ->
                if (result.isKnown) {
                    // Confirmed or Observed Memory Retrieved
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, MementoCyan.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .testTag("retrieval_result_card"),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = result.entity,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val isReading = ttsManager.currentlySpeakingId == "answer_${result.entity}"
                                    IconButton(
                                        onClick = {
                                            if (isReading) {
                                                ttsManager.stop()
                                            } else {
                                                val textToRead = "${result.entity}. ${result.answer}. ${result.location?.let { "Located in: $it." } ?: ""}"
                                                ttsManager.speakText("answer_${result.entity}", textToRead)
                                            }
                                        },
                                        modifier = Modifier.size(32.dp).testTag("read_answer_tts_btn")
                                    ) {
                                        Icon(
                                            imageVector = if (isReading) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                            contentDescription = "Read answer aloud",
                                            tint = if (isReading) MementoCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    EvidenceBadge(
                                        evidenceLevel = result.evidenceLevel,
                                        onClick = { showHowIKnow = true }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Highlight location if present
                            if (result.location != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = "Location",
                                        tint = MementoCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Last recorded location",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = result.location,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // Answer text
                            Text(
                                text = result.answer,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 22.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // How I Know explanation preview
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showHowIKnow = true },
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MementoCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = result.howIKnow,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Quick Action Buttons: [ That's right ], [ Correct ], [ Edit ], [ Forget ], [ Timeline ]
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showConfirmationSuccess = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EvidenceConfirmed.copy(alpha = 0.2f),
                                        contentColor = EvidenceConfirmed
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("action_thats_right_btn")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("That's right", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        newLocationInput = ""
                                        showCorrectDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("action_correct_btn")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Correct / Move", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }

                                Button(
                                    onClick = {
                                        onNavigateToTimeline(result.entity, result.matchingMemoryId ?: "")
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MementoCyan
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("action_timeline_btn")
                                ) {
                                    Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Timeline", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }

                                if (result.matchingMemoryId != null) {
                                    Button(
                                        onClick = {
                                            onDeleteMemory(result.matchingMemoryId)
                                            currentResult = null
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            contentColor = Color(0xFFEF4444)
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("action_forget_btn")
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Forget", fontSize = 12.sp)
                                    }
                                }
                            }

                            AnimatedVisibility(visible = showConfirmationSuccess) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(EvidenceConfirmed.copy(alpha = 0.15f))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "✓ Verified by you. Confirmed evidence retained.",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EvidenceConfirmed
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Unknown: Strict adherence to "I don't know."
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                            .testTag("unknown_memory_card"),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "I don't know.",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Memento does not have sufficient verified evidence recorded for this. Memento never invents or guesses memories.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 22.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Would you like to record it now?",
                                style = MaterialTheme.typography.labelMedium,
                                color = MementoCyan
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // How I Know Dialog
    if (showHowIKnow && currentResult != null) {
        val res = currentResult!!
        HowIKnowDialog(
            entityTitle = res.entity,
            evidenceLevel = res.evidenceLevel,
            explanation = res.howIKnow,
            sourceDetail = "Direct User Storage",
            recordedAt = "Recorded in active memory vault",
            onDismiss = { showHowIKnow = false }
        )
    }

    // Correct / Move Location Dialog
    if (showCorrectDialog && currentResult != null) {
        val res = currentResult!!
        AlertDialog(
            onDismissRequest = { showCorrectDialog = false },
            title = {
                Text("Correct Location for ${res.entity}")
            },
            text = {
                Column {
                    Text(
                        text = "e.g. “Actually, I moved it to my desk.”",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newLocationInput,
                        onValueChange = { newLocationInput = it },
                        placeholder = { Text("New location (e.g. My desk)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_location_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newLocationInput.isNotBlank() && res.matchingMemoryId != null) {
                            onCorrectLocation(res.matchingMemoryId, newLocationInput)
                            currentResult = res.copy(
                                location = newLocationInput,
                                answer = "Your ${res.entity} was moved to $newLocationInput.",
                                howIKnow = "How I know: You updated this location to $newLocationInput just now."
                            )
                            showCorrectDialog = false
                            showConfirmationSuccess = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MementoCyan,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.testTag("confirm_correct_location_btn")
                ) {
                    Text("Update Location", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCorrectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showVoiceSheet) {
        VoiceCommandSheet(
            title = "Ask Memento with Voice",
            subtitle = "Speak a question to search your memory vault",
            mode = VoiceInputMode.ASK_QUESTION,
            onCommandCaptured = { spokenQuestion ->
                if (spokenQuestion.isNotBlank()) {
                    queryText = spokenQuestion
                    isSearching = true
                    lastQueriedText = spokenQuestion
                    coroutineScope.launch {
                        currentResult = onAsk(spokenQuestion)
                        isSearching = false
                        showConfirmationSuccess = false
                    }
                }
            },
            onDismiss = { showVoiceSheet = false }
        )
    }
}
