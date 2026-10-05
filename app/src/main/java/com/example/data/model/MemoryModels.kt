package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class MemoryType {
    THING,
    PEOPLE,
    PROMISE,
    WAITING,
    PLACE
}

enum class EvidenceLevel {
    CONFIRMED, // 🟢 explicitly told by user
    OBSERVED,  // 🟡 detected from permitted uploaded media
    INFERRED   // 🔵 AI-derived information
}

enum class SourceType {
    TEXT,
    VOICE,
    CAMERA
}

enum class MemoryStatus {
    ACTIVE,
    RESOLVED,
    ARCHIVED
}

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val type: MemoryType,
    val title: String,
    val content: String,
    val location: String? = null,
    val person: String? = null,
    val evidenceLevel: EvidenceLevel = EvidenceLevel.CONFIRMED,
    val sourceType: SourceType = SourceType.TEXT,
    val sourceExplanation: String,
    val timestamp: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val status: MemoryStatus = MemoryStatus.ACTIVE,
    val isPinned: Boolean = false,
    val imageUri: String? = null
)

@Entity(tableName = "timeline_events")
data class MemoryTimelineEvent(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val memoryId: String,
    val userId: String,
    val entityTitle: String,
    val eventDescription: String,
    val location: String? = null,
    val person: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val evidenceLevel: EvidenceLevel = EvidenceLevel.CONFIRMED
)

@Entity(tableName = "user_sessions")
data class UserSession(
    @PrimaryKey val userId: String,
    val displayName: String,
    val emailOrPhone: String,
    val authProvider: String, // "GOOGLE", "EMAIL", "PHONE"
    val isOnboarded: Boolean = true,
    val retentionDays: Int = 0, // 0 = Forever, 30, 90, 365
    val simpleMode: Boolean = true,
    val themeMode: String = "DARK", // "DARK", "LIGHT", "ADAPTIVE"
    val lastLoginTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "security_audit_logs")
data class SecurityAuditLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val eventType: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
