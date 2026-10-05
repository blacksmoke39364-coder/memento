package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.MemoryEntity
import com.example.data.model.MemoryStatus
import com.example.data.model.MemoryTimelineEvent
import com.example.data.model.MemoryType
import com.example.data.model.SecurityAuditLog
import com.example.data.model.UserSession
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    // --- Memory Operations (Strict User Isolation) ---
    @Query("SELECT * FROM memories WHERE userId = :userId AND status != 'ARCHIVED' ORDER BY updatedAt DESC")
    fun getAllActiveMemories(userId: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getAllMemories(userId: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE userId = :userId AND id = :memoryId LIMIT 1")
    suspend fun getMemoryById(userId: String, memoryId: String): MemoryEntity?

    @Query("SELECT * FROM memories WHERE userId = :userId AND type = :type AND status = :status ORDER BY updatedAt DESC")
    fun getMemoriesByType(userId: String, type: MemoryType, status: MemoryStatus = MemoryStatus.ACTIVE): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE userId = :userId AND (LOWER(title) LIKE '%' || LOWER(:query) || '%' OR LOWER(content) LIKE '%' || LOWER(:query) || '%' OR LOWER(COALESCE(location, '')) LIKE '%' || LOWER(:query) || '%' OR LOWER(COALESCE(person, '')) LIKE '%' || LOWER(:query) || '%') ORDER BY updatedAt DESC")
    fun searchMemories(userId: String, query: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE userId = :userId AND (LOWER(title) = LOWER(:entityName) OR LOWER(content) LIKE '%' || LOWER(:entityName) || '%') ORDER BY updatedAt DESC LIMIT 5")
    suspend fun findMatchingMemories(userId: String, entityName: String): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE userId = :userId AND person IS NOT NULL AND person != ''")
    fun getMemoriesWithPeople(userId: String): Flow<List<MemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemories(memories: List<MemoryEntity>)

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE userId = :userId AND id = :memoryId")
    suspend fun deleteMemoryById(userId: String, memoryId: String)

    @Query("DELETE FROM memories WHERE userId = :userId AND type = :type")
    suspend fun deleteMemoriesByType(userId: String, type: MemoryType)

    @Query("DELETE FROM memories WHERE userId = :userId")
    suspend fun deleteAllMemories(userId: String)

    // --- Timeline Operations ---
    @Query("SELECT * FROM timeline_events WHERE userId = :userId AND memoryId = :memoryId ORDER BY timestamp ASC")
    fun getTimelineForMemory(userId: String, memoryId: String): Flow<List<MemoryTimelineEvent>>

    @Query("SELECT * FROM timeline_events WHERE userId = :userId AND (LOWER(entityTitle) = LOWER(:title) OR memoryId = :memoryId) ORDER BY timestamp ASC")
    fun getTimelineForEntity(userId: String, title: String, memoryId: String = ""): Flow<List<MemoryTimelineEvent>>

    @Query("SELECT * FROM timeline_events WHERE userId = :userId ORDER BY timestamp ASC")
    fun getAllTimelineEvents(userId: String): Flow<List<MemoryTimelineEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimelineEvent(event: MemoryTimelineEvent)

    @Query("DELETE FROM timeline_events WHERE userId = :userId AND memoryId = :memoryId")
    suspend fun deleteTimelineForMemory(userId: String, memoryId: String)

    @Query("DELETE FROM timeline_events WHERE userId = :userId")
    suspend fun deleteAllTimelineEvents(userId: String)

    // --- User Session ---
    @Query("SELECT * FROM user_sessions WHERE userId = :userId LIMIT 1")
    suspend fun getUserSession(userId: String): UserSession?

    @Query("SELECT * FROM user_sessions ORDER BY lastLoginTimestamp DESC LIMIT 1")
    fun getCurrentUserSession(): Flow<UserSession?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserSession(session: UserSession)

    @Query("DELETE FROM user_sessions WHERE userId = :userId")
    suspend fun deleteUserSession(userId: String)

    // --- Security Audit Logs ---
    @Insert
    suspend fun insertAuditLog(log: SecurityAuditLog)

    @Query("SELECT * FROM security_audit_logs WHERE userId = :userId ORDER BY timestamp DESC LIMIT 50")
    fun getAuditLogs(userId: String): Flow<List<SecurityAuditLog>>

    @Query("DELETE FROM security_audit_logs WHERE userId = :userId")
    suspend fun clearAuditLogs(userId: String)
}
