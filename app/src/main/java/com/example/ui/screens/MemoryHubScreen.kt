package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MemoryEntity
import com.example.data.model.MemoryType
import com.example.ui.components.HowIKnowDialog
import com.example.ui.components.MemoryCard
import com.example.ui.theme.MementoCyan
import com.example.util.rememberTextToSpeechManager

@Composable
fun MemoryHubScreen(
    memories: List<MemoryEntity>,
    onViewTimeline: (MemoryEntity) -> Unit,
    onCorrectLocation: (MemoryEntity) -> Unit,
    onDeleteMemory: (MemoryEntity) -> Unit,
    onRememberClick: () -> Unit
) {
    val ttsManager = rememberTextToSpeechManager()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<MemoryType?>(null) }
    var inspectedMemory by remember { mutableStateOf<MemoryEntity?>(null) }

    val filteredMemories = memories.filter { mem ->
        val matchesCategory = selectedFilter == null || mem.type == selectedFilter
        val matchesQuery = searchQuery.isBlank() ||
                mem.title.contains(searchQuery, ignoreCase = true) ||
                mem.content.contains(searchQuery, ignoreCase = true) ||
                (mem.location ?: "").contains(searchQuery, ignoreCase = true) ||
                (mem.person ?: "").contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesQuery
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Title & Search Bar
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    text = "Memory Vault",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Every preserved memory with strict evidence verification.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search things, people, places...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vault_search_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MementoCyan,
                        focusedLabelColor = MementoCyan
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilter == null,
                            onClick = { selectedFilter = null },
                            label = { Text("All (${memories.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MementoCyan.copy(alpha = 0.2f),
                                selectedLabelColor = MementoCyan
                            )
                        )
                    }

                    items(MemoryType.values()) { type ->
                        val count = memories.count { it.type == type }
                        FilterChip(
                            selected = selectedFilter == type,
                            onClick = { selectedFilter = if (selectedFilter == type) null else type },
                            label = { Text("${type.name.lowercase().replaceFirstChar { it.uppercase() }} ($count)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MementoCyan.copy(alpha = 0.2f),
                                selectedLabelColor = MementoCyan
                            )
                        )
                    }
                }
            }

            // Memory List
            if (filteredMemories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No memories matching \"$searchQuery\"" else "Your memory vault is empty.",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tell Memento something you want to remember.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MementoCyan,
                            modifier = Modifier.clickable { onRememberClick() }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredMemories, key = { it.id }) { memory ->
                        MemoryCard(
                            memory = memory,
                            onHowIKnowClick = { inspectedMemory = memory },
                            onViewTimelineClick = { onViewTimeline(memory) },
                            onCorrectLocationClick = { onCorrectLocation(memory) },
                            onDeleteClick = { onDeleteMemory(memory) },
                            onSpeakClick = { ttsManager.speakMemory(memory) },
                            isSpeaking = ttsManager.currentlySpeakingId == memory.id
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // Inspect "How I know"
    inspectedMemory?.let { mem ->
        HowIKnowDialog(
            entityTitle = mem.title,
            evidenceLevel = mem.evidenceLevel,
            explanation = mem.sourceExplanation,
            sourceDetail = mem.sourceType.name,
            recordedAt = "Stored in vault",
            onDismiss = { inspectedMemory = null }
        )
    }
}
