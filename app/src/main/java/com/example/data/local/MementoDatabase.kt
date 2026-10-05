package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.EvidenceLevel
import com.example.data.model.MemoryEntity
import com.example.data.model.MemoryStatus
import com.example.data.model.MemoryTimelineEvent
import com.example.data.model.MemoryType
import com.example.data.model.SecurityAuditLog
import com.example.data.model.SourceType
import com.example.data.model.UserSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        MemoryEntity::class,
        MemoryTimelineEvent::class,
        UserSession::class,
        SecurityAuditLog::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MementoDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: MementoDatabase? = null

        fun getDatabase(context: Context): MementoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MementoDatabase::class.java,
                    "memento_secure.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
