package com.example.data.repository

import android.content.Context
import com.example.BuildConfig
import com.example.data.local.MemoryDao
import com.example.data.model.EvidenceLevel
import com.example.data.model.MemoryEntity
import com.example.data.model.MemoryStatus
import com.example.data.model.MemoryTimelineEvent
import com.example.data.model.MemoryType
import com.example.data.model.SecurityAuditLog
import com.example.data.model.SourceType
import com.example.data.model.UserSession
import com.example.data.remote.GeminiService
import com.example.data.remote.MemoryRetrievalResult
import com.example.data.remote.ParsedMemoryResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MemoryRepository(
    private val memoryDao: MemoryDao,
    private val geminiService: GeminiService
) {
    val currentUserSession: Flow<UserSession?> = memoryDao.getCurrentUserSession()

    suspend fun getActiveMemories(userId: String): Flow<List<MemoryEntity>> {
        return memoryDao.getAllActiveMemories(userId)
    }

    suspend fun getMemoriesByType(userId: String, type: MemoryType): Flow<List<MemoryEntity>> {
        return memoryDao.getMemoriesByType(userId, type)
    }

    suspend fun searchMemories(userId: String, query: String): Flow<List<MemoryEntity>> {
        return memoryDao.searchMemories(userId, query)
    }

    suspend fun getTimelineForEntity(userId: String, title: String, memoryId: String = ""): Flow<List<MemoryTimelineEvent>> {
        return memoryDao.getTimelineForEntity(userId, title, memoryId)
    }

    suspend fun getAuditLogs(userId: String): Flow<List<SecurityAuditLog>> {
        return memoryDao.getAuditLogs(userId)
    }

    /**
     * Directly inserts or updates a memory entity into local Room storage.
     */
    suspend fun addMemory(memory: MemoryEntity) {
        memoryDao.insertMemory(memory)
        logAudit(memory.userId, "MEMORY_ADDED", "Added memory '${memory.title}'.")
    }

    /**
     * Batch inserts memory entities into local Room storage.
     */
    suspend fun addMemories(memories: List<MemoryEntity>) {
        memoryDao.insertMemories(memories)
        if (memories.isNotEmpty()) {
            logAudit(memories.first().userId, "BATCH_MEMORIES_ADDED", "Added ${memories.size} memories.")
        }
    }

    /**
     * Seeds initial memories for the user so they can test the core loop right away!
     */
    suspend fun seedInitialMemoriesIfEmpty(userId: String) {
        val existing = memoryDao.getAllActiveMemories(userId).firstOrNull()
        if (existing.isNullOrEmpty()) {
            val now = System.currentTimeMillis()
            val hourAgo = now - 3600_000 * 2
            val dayAgo = now - 86400_000

            // 1. Passport in Drawer 2
            val passportId = UUID.randomUUID().toString()
            val passportMem = MemoryEntity(
                id = passportId,
                userId = userId,
                type = MemoryType.THING,
                title = "Passport",
                content = "Kept in drawer 2 of bedroom table",
                location = "Drawer 2, Bedroom",
                person = null,
                evidenceLevel = EvidenceLevel.CONFIRMED,
                sourceType = SourceType.TEXT,
                sourceExplanation = "You told Memento earlier",
                timestamp = hourAgo,
                updatedAt = hourAgo
            )
            memoryDao.insertMemory(passportMem)
            memoryDao.insertTimelineEvent(
                MemoryTimelineEvent(
                    memoryId = passportId,
                    userId = userId,
                    entityTitle = "Passport",
                    eventDescription = "Placed in drawer 2, Bedroom",
                    location = "Drawer 2, Bedroom",
                    timestamp = hourAgo,
                    evidenceLevel = EvidenceLevel.CONFIRMED
                )
            )

            // 2. Charger given to Rahul
            val chargerId = UUID.randomUUID().toString()
            val chargerMem = MemoryEntity(
                id = chargerId,
                userId = userId,
                type = MemoryType.PEOPLE,
                title = "Charger",
                content = "Gave 65W fast charger to Rahul",
                location = null,
                person = "Rahul",
                evidenceLevel = EvidenceLevel.CONFIRMED,
                sourceType = SourceType.VOICE,
                sourceExplanation = "You told Memento via voice note",
                timestamp = dayAgo,
                updatedAt = dayAgo
            )
            memoryDao.insertMemory(chargerMem)
            memoryDao.insertTimelineEvent(
                MemoryTimelineEvent(
                    memoryId = chargerId,
                    userId = userId,
                    entityTitle = "Charger",
                    eventDescription = "Gave to Rahul",
                    person = "Rahul",
                    timestamp = dayAgo,
                    evidenceLevel = EvidenceLevel.CONFIRMED
                )
            )

            // 3. Promise: Mom
            val momPromiseId = UUID.randomUUID().toString()
            memoryDao.insertMemory(
                MemoryEntity(
                    id = momPromiseId,
                    userId = userId,
                    type = MemoryType.PROMISE,
                    title = "Call Mom",
                    content = "Promised Mom I would call tonight",
                    location = null,
                    person = "Mom",
                    evidenceLevel = EvidenceLevel.CONFIRMED,
                    sourceType = SourceType.TEXT,
                    sourceExplanation = "You told Memento",
                    timestamp = now - 1800_000,
                    updatedAt = now - 1800_000
                )
            )

            // 4. Waiting: Amazon refund
            val amazonId = UUID.randomUUID().toString()
            memoryDao.insertMemory(
                MemoryEntity(
                    id = amazonId,
                    userId = userId,
                    type = MemoryType.WAITING,
                    title = "Amazon Refund",
                    content = "Waiting for ₹1,499 refund for returned headphones",
                    location = null,
                    person = "Amazon",
                    evidenceLevel = EvidenceLevel.CONFIRMED,
                    sourceType = SourceType.TEXT,
                    sourceExplanation = "You recorded this expectation",
                    timestamp = dayAgo,
                    updatedAt = dayAgo
                )
            )

            // 5. Car Keys history demo
            val carKeysId = UUID.randomUUID().toString()
            val carKeysMem = MemoryEntity(
                id = carKeysId,
                userId = userId,
                type = MemoryType.THING,
                title = "Car Keys",
                content = "Car keys on Key Hook near entrance",
                location = "Key Hook, Hallway",
                person = null,
                evidenceLevel = EvidenceLevel.CONFIRMED,
                sourceType = SourceType.TEXT,
                sourceExplanation = "You updated the location",
                timestamp = now - 86400_000 * 4,
                updatedAt = now
            )
            memoryDao.insertMemory(carKeysMem)

            // Car Keys timeline events
            val d4 = now - 86400_000 * 4
            val d3 = now - 86400_000 * 3
            val d2 = now - 86400_000 * 2
            val d1 = now - 86400_000 * 1

            memoryDao.insertTimelineEvent(
                MemoryTimelineEvent(
                    memoryId = carKeysId,
                    userId = userId,
                    entityTitle = "Car Keys",
                    eventDescription = "New duplicate set made",
                    location = "Hallway",
                    timestamp = d4
                )
            )
            memoryDao.insertTimelineEvent(
                MemoryTimelineEvent(
                    memoryId = carKeysId,
                    userId = userId,
                    entityTitle = "Car Keys",
                    eventDescription = "Placed in jacket pocket",
                    location = "Jacket Pocket",
                    timestamp = d3
                )
            )
            memoryDao.insertTimelineEvent(
                MemoryTimelineEvent(
                    memoryId = carKeysId,
                    userId = userId,
                    entityTitle = "Car Keys",
                    eventDescription = "Borrowed by roommate",
                    person = "Rahul",
                    timestamp = d2
                )
            )
            memoryDao.insertTimelineEvent(
                MemoryTimelineEvent(
                    memoryId = carKeysId,
                    userId = userId,
                    entityTitle = "Car Keys",
                    eventDescription = "Returned to key hook",
                    location = "Key Hook, Hallway",
                    timestamp = d1
                )
            )

            logAudit(userId, "INITIAL_MEMORIES_SEEDED", "Sample demonstration vault populated.")
        }
    }

    /**
     * Remember action:
     * Takes natural language text or voice transcript, parses it, stores it,
     * and logs the timeline event.
     */
    suspend fun remember(
        userId: String,
        rawInput: String,
        sourceType: SourceType,
        imageUri: String? = null,
        evidenceOverride: EvidenceLevel? = null
    ): MemoryEntity {
        val parsed = geminiService.parseMemory(rawInput, sourceType)
        val memType = runCatching { MemoryType.valueOf(parsed.type) }.getOrDefault(MemoryType.THING)
        val level = evidenceOverride ?: runCatching { EvidenceLevel.valueOf(parsed.evidenceLevel) }.getOrDefault(
            if (sourceType == SourceType.CAMERA) EvidenceLevel.OBSERVED else EvidenceLevel.CONFIRMED
        )

        val memoryId = UUID.randomUUID().toString()
        val formattedDate = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date())
        val sourceExp = when (sourceType) {
            SourceType.TEXT -> "You told Memento on $formattedDate"
            SourceType.VOICE -> "You recorded this voice memory on $formattedDate"
            SourceType.CAMERA -> "Observed from photo capture on $formattedDate"
        }

        // Check if there's an existing memory with this title to update instead of duplicate
        val existing = memoryDao.findMatchingMemories(userId, parsed.title).firstOrNull()
        val finalMemory = if (existing != null) {
            existing.copy(
                content = parsed.content,
                location = parsed.location ?: existing.location,
                person = parsed.person ?: existing.person,
                evidenceLevel = level,
                sourceType = sourceType,
                sourceExplanation = sourceExp,
                updatedAt = System.currentTimeMillis(),
                imageUri = imageUri ?: existing.imageUri
            )
        } else {
            MemoryEntity(
                id = memoryId,
                userId = userId,
                type = memType,
                title = parsed.title,
                content = parsed.content,
                location = parsed.location,
                person = parsed.person,
                evidenceLevel = level,
                sourceType = sourceType,
                sourceExplanation = sourceExp,
                timestamp = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                imageUri = imageUri
            )
        }

        memoryDao.insertMemory(finalMemory)

        // Add timeline event
        val desc = when {
            finalMemory.location != null -> "Recorded at: ${finalMemory.location}"
            finalMemory.person != null -> "Connected with: ${finalMemory.person}"
            else -> finalMemory.content
        }
        memoryDao.insertTimelineEvent(
            MemoryTimelineEvent(
                memoryId = finalMemory.id,
                userId = userId,
                entityTitle = finalMemory.title,
                eventDescription = desc,
                location = finalMemory.location,
                person = finalMemory.person,
                evidenceLevel = level
            )
        )

        logAudit(userId, "MEMORY_STORED", "Stored memory for entity '${finalMemory.title}'.")
        return finalMemory
    }

    /**
     * Memory Correction / Movement:
     * User says "Actually, I moved it to my desk"
     */
    suspend fun correctMemoryLocation(
        userId: String,
        memoryId: String,
        newLocation: String,
        explanation: String = "Location corrected by user"
    ) {
        val memory = memoryDao.getMemoryById(userId, memoryId) ?: return
        val updated = memory.copy(
            location = newLocation,
            updatedAt = System.currentTimeMillis(),
            sourceExplanation = "You updated this location to $newLocation"
        )
        memoryDao.updateMemory(updated)

        memoryDao.insertTimelineEvent(
            MemoryTimelineEvent(
                memoryId = memory.id,
                userId = userId,
                entityTitle = memory.title,
                eventDescription = "Moved → $newLocation",
                location = newLocation,
                timestamp = System.currentTimeMillis(),
                evidenceLevel = EvidenceLevel.CONFIRMED
            )
        )

        logAudit(userId, "MEMORY_CORRECTED", "Updated location of '${memory.title}' to '$newLocation'.")
    }

    /**
     * Ask Memento: Natural-language retrieval with strict evidence check.
     */
    suspend fun askMemento(userId: String, question: String): MemoryRetrievalResult {
        val allMemories = memoryDao.getAllActiveMemories(userId).firstOrNull() ?: emptyList()
        val timelineEvents = memoryDao.getAllTimelineEvents(userId).firstOrNull() ?: emptyList()

        val result = geminiService.retrieveAnswer(question, allMemories, timelineEvents)
        logAudit(userId, "MEMORY_RETRIEVED", "Queried memory: '$question', isKnown=${result.isKnown}.")
        return result
    }

    suspend fun updateMemory(memory: MemoryEntity) {
        memoryDao.updateMemory(memory)
        logAudit(memory.userId, "MEMORY_EDITED", "Edited memory '${memory.title}'.")
    }

    suspend fun deleteMemory(userId: String, memoryId: String) {
        memoryDao.deleteMemoryById(userId, memoryId)
        memoryDao.deleteTimelineForMemory(userId, memoryId)
        logAudit(userId, "MEMORY_DELETED", "Deleted memory $memoryId.")
    }

    suspend fun deleteMemoriesByType(userId: String, type: MemoryType) {
        memoryDao.deleteMemoriesByType(userId, type)
        logAudit(userId, "CATEGORY_DELETED", "Deleted all memories of type $type.")
    }

    suspend fun deleteAllMemories(userId: String) {
        memoryDao.deleteAllMemories(userId)
        memoryDao.deleteAllTimelineEvents(userId)
        logAudit(userId, "ALL_MEMORIES_DELETED", "Cleared all memories permanently.")
    }

    /**
     * Privacy Export: Generates JSON of all authorized memories & timeline.
     */
    suspend fun exportDataJson(userId: String): String {
        val memories = memoryDao.getAllMemories(userId).firstOrNull() ?: emptyList()
        val root = JSONObject()
        root.put("app", "Memento")
        root.put("version", "1.0")
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
        root.put("userId", userId)

        val memArray = JSONArray()
        for (m in memories) {
            val obj = JSONObject().apply {
                put("id", m.id)
                put("type", m.type.name)
                put("title", m.title)
                put("content", m.content)
                put("location", m.location ?: "")
                put("person", m.person ?: "")
                put("evidenceLevel", m.evidenceLevel.name)
                put("sourceType", m.sourceType.name)
                put("sourceExplanation", m.sourceExplanation)
                put("timestamp", m.timestamp)
                put("updatedAt", m.updatedAt)
            }
            memArray.put(obj)
        }
        root.put("memories", memArray)

        logAudit(userId, "DATA_EXPORTED", "Exported ${memories.size} memories to JSON.")
        return root.toString(2)
    }

    suspend fun saveSession(session: UserSession) {
        memoryDao.saveUserSession(session)
        logAudit(session.userId, "SESSION_SAVED", "User session established (${session.authProvider}).")
    }

    suspend fun logAudit(userId: String, eventType: String, details: String) {
        memoryDao.insertAuditLog(
            SecurityAuditLog(
                userId = userId,
                eventType = eventType,
                details = details
            )
        )
    }
}
