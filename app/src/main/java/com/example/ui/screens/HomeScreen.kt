package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.EvidenceLevel
import com.example.data.model.MemoryEntity
import com.example.data.model.MemoryType
import com.example.ui.components.EvidenceBadge
import com.example.ui.components.HowIKnowDialog
import com.example.ui.components.MemoryCard
import com.example.ui.components.TellMementoInput
import com.example.ui.components.VoiceCommandSheet
import com.example.ui.components.VoiceInputMode
import com.example.ui.theme.EvidenceConfirmed
import com.example.ui.theme.MementoCyan
import com.example.ui.theme.MementoSilver
import com.example.ui.theme.MementoSilverBright
import com.example.ui.theme.MementoSilverMuted
import com.example.util.rememberTextToSpeechManager

@Composable
fun HomeScreen(
    userName: String,
    memories: List<MemoryEntity>,
    onRememberClick: () -> Unit,
    onFindClick: () -> Unit,
    onPeopleClick: () -> Unit,
    onWaitingClick: () -> Unit,
    onQuickSave: (String) -> Unit,
    onViewTimeline: (MemoryEntity) -> Unit,
    onCorrectLocation: (MemoryEntity) -> Unit,
    onDeleteMemory: (MemoryEntity) -> Unit,
    onCameraClick: () -> Unit,
    isSimpleMode: Boolean = true,
    onOpenSettings: () -> Unit,
    onOpenGeminiAssistant: () -> Unit = {}
) {
    val ttsManager = rememberTextToSpeechManager()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterType by remember { mutableStateOf<MemoryType?>(null) }

    var inputText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var showVoiceSheet by remember { mutableStateOf(false) }
    var lastSavedTitle by remember { mutableStateOf<String?>(null) }
    var inspectedMemory by remember { mutableStateOf<MemoryEntity?>(null) }

    val isSearchActive = searchQuery.isNotBlank() || selectedFilterType != null

    val filteredMemories = remember(searchQuery, selectedFilterType, memories) {
        memories.filter { mem ->
            val matchesQuery = searchQuery.isBlank() ||
                    mem.title.contains(searchQuery, ignoreCase = true) ||
                    mem.content.contains(searchQuery, ignoreCase = true) ||
                    (mem.location?.contains(searchQuery, ignoreCase = true) == true) ||
                    (mem.person?.contains(searchQuery, ignoreCase = true) == true)
            val matchesType = selectedFilterType == null || mem.type == selectedFilterType
            matchesQuery && matchesType
        }
    }

    val todayPromises = memories.filter { it.type == MemoryType.PROMISE }
    val todayWaiting = memories.filter { it.type == MemoryType.WAITING }
    val recentThings = memories.filter { it.type == MemoryType.THING }.take(3)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MementoCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_memento_logo),
                                contentDescription = "Logo",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "MEMENTO",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Settings / Mission Control",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Quick Search Bar at top of Home Screen (Room Database Local Search)
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search memories, places, people...",
                            color = MementoSilverMuted,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MementoSilverBright,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.testTag("clear_home_search_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = MementoSilverMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = onOpenGeminiAssistant,
                                modifier = Modifier.testTag("voice_home_search_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Search",
                                    tint = MementoSilverBright,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .testTag("home_search_bar"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = MementoSilverBright,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedTextColor = MementoSilverBright,
                        unfocusedTextColor = MementoSilver
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Quick Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilterType == null,
                        onClick = { selectedFilterType = null },
                        label = { Text("All (${memories.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MementoSilver,
                            selectedLabelColor = Color(0xFF0F172A),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MementoSilverMuted
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("filter_all_chip")
                    )

                    FilterChip(
                        selected = selectedFilterType == MemoryType.THING,
                        onClick = {
                            selectedFilterType = if (selectedFilterType == MemoryType.THING) null else MemoryType.THING
                        },
                        label = { Text("Things 🔑", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MementoSilver,
                            selectedLabelColor = Color(0xFF0F172A),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MementoSilverMuted
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("filter_things_chip")
                    )

                    FilterChip(
                        selected = selectedFilterType == MemoryType.PROMISE,
                        onClick = {
                            selectedFilterType = if (selectedFilterType == MemoryType.PROMISE) null else MemoryType.PROMISE
                        },
                        label = { Text("Promises 🤝", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MementoSilver,
                            selectedLabelColor = Color(0xFF0F172A),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MementoSilverMuted
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("filter_promises_chip")
                    )

                    FilterChip(
                        selected = selectedFilterType == MemoryType.WAITING,
                        onClick = {
                            selectedFilterType = if (selectedFilterType == MemoryType.WAITING) null else MemoryType.WAITING
                        },
                        label = { Text("Waiting ⏳", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MementoSilver,
                            selectedLabelColor = Color(0xFF0F172A),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MementoSilverMuted
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("filter_waiting_chip")
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            if (isSearchActive) {
                // Search Results Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "VAULT SEARCH RESULTS (${filteredMemories.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp,
                            color = MementoSilverBright
                        )
                        Text(
                            text = "Clear filter",
                            style = MaterialTheme.typography.labelSmall,
                            color = MementoSilverMuted,
                            modifier = Modifier
                                .clickable {
                                    searchQuery = ""
                                    selectedFilterType = null
                                }
                                .padding(4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (filteredMemories.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                                .padding(24.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MementoSilverMuted,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No memories found in Room vault",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MementoSilverBright
                                )
                                Text(
                                    text = "Try another search term or ask Gemini Voice Assistant",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MementoSilverMuted
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                } else {
                    items(filteredMemories, key = { it.id }) { memory ->
                        MemoryCard(
                            memory = memory,
                            onHowIKnowClick = { inspectedMemory = memory },
                            onViewTimelineClick = { onViewTimeline(memory) },
                            onCorrectLocationClick = { onCorrectLocation(memory) },
                            onDeleteClick = { onDeleteMemory(memory) },
                            onSpeakClick = { ttsManager.speakMemory(memory) },
                            isSpeaking = ttsManager.currentlySpeakingId == memory.id,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }
            } else {
                // Gemini AI Ambient Floating Light Bar (Opens like Gemini AI)
                item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .border(
                            width = 1.5.dp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MementoSilverBright,
                                    Color(0xFFA0AEC0),
                                    Color(0xFFCBD5E1),
                                    MementoSilverBright
                                )
                            ),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .clickable { onOpenGeminiAssistant() }
                        .testTag("gemini_ambient_home_bar"),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MementoSilver.copy(alpha = 0.18f))
                                .border(1.dp, MementoSilverBright.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Gemini AI",
                                tint = MementoSilverBright,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Gemini Voice Assistant",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MementoSilverBright
                            )
                            Text(
                                text = "Press Power Key or tap to speak & remember",
                                style = MaterialTheme.typography.bodySmall,
                                color = MementoSilverMuted,
                                fontSize = 11.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MementoSilverBright.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Live Mic",
                                tint = MementoSilverBright,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Hero Question
            item {
                Text(
                    text = "What should I remember?",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Intentional memory system for $userName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Central Signature Interaction: Tell Memento...
            item {
                TellMementoInput(
                    text = inputText,
                    onTextChange = { inputText = it },
                    onSend = { query ->
                        onQuickSave(query)
                        lastSavedTitle = query
                        inputText = ""
                    },
                    isRecording = isRecording,
                    onToggleRecording = {
                        onOpenGeminiAssistant()
                    },
                    onCameraClick = onCameraClick,
                    placeholderText = "Tell Memento..."
                )

                // Instant confirmation feedback
                AnimatedVisibility(visible = lastSavedTitle != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(EvidenceConfirmed.copy(alpha = 0.15f))
                            .border(1.dp, EvidenceConfirmed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = EvidenceConfirmed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Got it. I'll remember.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = EvidenceConfirmed
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "Dismiss",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable { lastSavedTitle = null }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Quick Actions: + Remember, Find, People, Waiting
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        icon = Icons.Default.Add,
                        label = "Remember",
                        isPrimary = true,
                        onClick = onRememberClick,
                        modifier = Modifier.weight(1f),
                        testTag = "home_quick_remember_btn"
                    )
                    QuickActionButton(
                        icon = Icons.Default.Search,
                        label = "Find",
                        onClick = onFindClick,
                        modifier = Modifier.weight(1f),
                        testTag = "home_quick_find_btn"
                    )
                    QuickActionButton(
                        icon = Icons.Default.Handshake,
                        label = "People",
                        onClick = onPeopleClick,
                        modifier = Modifier.weight(1f),
                        testTag = "home_quick_people_btn"
                    )
                    QuickActionButton(
                        icon = Icons.Default.HourglassTop,
                        label = "Waiting",
                        onClick = onWaitingClick,
                        modifier = Modifier.weight(1f),
                        testTag = "home_quick_waiting_btn"
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            // TODAY Section: Curated meaningful information only
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "TODAY",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp,
                        color = MementoCyan
                    )

                    Text(
                        text = "${memories.size} total in vault",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (todayPromises.isEmpty() && todayWaiting.isEmpty() && recentThings.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Your memory is calm and clear.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tell Memento something you'll want to remember later.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MementoCyan
                            )
                        }
                    }
                }
            } else {
                // Curated Highlights
                items(todayPromises.take(2)) { promise ->
                    HighlightItem(
                        icon = Icons.Default.Handshake,
                        title = promise.person?.let { "Promised $it" } ?: "Promise",
                        detail = promise.content,
                        evidenceLevel = promise.evidenceLevel,
                        onClick = { inspectedMemory = promise }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(todayWaiting.take(2)) { waiting ->
                    HighlightItem(
                        icon = Icons.Default.HourglassTop,
                        title = waiting.person?.let { "Waiting on $it" } ?: "Waiting",
                        detail = waiting.content,
                        evidenceLevel = waiting.evidenceLevel,
                        onClick = { inspectedMemory = waiting }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(recentThings) { thing ->
                    HighlightItem(
                        icon = Icons.Default.Shield,
                        title = thing.title,
                        detail = thing.location?.let { "Recorded in: $it" } ?: thing.content,
                        evidenceLevel = thing.evidenceLevel,
                        onClick = { inspectedMemory = thing }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Inspect "How I Know" Dialog
    inspectedMemory?.let { mem ->
        HowIKnowDialog(
            entityTitle = mem.title,
            evidenceLevel = mem.evidenceLevel,
            explanation = mem.sourceExplanation,
            sourceDetail = mem.sourceType.name,
            recordedAt = "Recently stored",
            onDismiss = { inspectedMemory = null }
        )
    }

    // Voice Command Sheet
    if (showVoiceSheet) {
        VoiceCommandSheet(
            title = "Voice Memory",
            subtitle = "Tell Memento what you want to remember",
            mode = VoiceInputMode.RECORD_MEMORY,
            onCommandCaptured = { spokenCommand ->
                if (spokenCommand.isNotBlank()) {
                    onQuickSave(spokenCommand)
                    lastSavedTitle = spokenCommand
                }
            },
            onDismiss = { showVoiceSheet = false }
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    testTag: String
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(
                1.dp,
                if (isPrimary) MementoCyan else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .testTag(testTag),
        color = if (isPrimary) MementoCyan.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isPrimary) MementoCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isPrimary) MementoCyan else MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HighlightItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    detail: String,
    evidenceLevel: EvidenceLevel,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MementoCyan.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MementoCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            EvidenceBadge(evidenceLevel = evidenceLevel)
        }
    }
}
