package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.EvidenceLevel
import com.example.data.model.MemoryStatus
import com.example.data.model.MemoryType
import com.example.data.model.SourceType

class Converters {
    @TypeConverter
    fun fromMemoryType(type: MemoryType): String = type.name

    @TypeConverter
    fun toMemoryType(value: String): MemoryType = runCatching {
        MemoryType.valueOf(value)
    }.getOrDefault(MemoryType.THING)

    @TypeConverter
    fun fromEvidenceLevel(level: EvidenceLevel): String = level.name

    @TypeConverter
    fun toEvidenceLevel(value: String): EvidenceLevel = runCatching {
        EvidenceLevel.valueOf(value)
    }.getOrDefault(EvidenceLevel.CONFIRMED)

    @TypeConverter
    fun fromSourceType(source: SourceType): String = source.name

    @TypeConverter
    fun toSourceType(value: String): SourceType = runCatching {
        SourceType.valueOf(value)
    }.getOrDefault(SourceType.TEXT)

    @TypeConverter
    fun fromMemoryStatus(status: MemoryStatus): String = status.name

    @TypeConverter
    fun toMemoryStatus(value: String): MemoryStatus = runCatching {
        MemoryStatus.valueOf(value)
    }.getOrDefault(MemoryStatus.ACTIVE)
}
