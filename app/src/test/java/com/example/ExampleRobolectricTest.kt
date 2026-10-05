package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.EvidenceLevel
import com.example.data.model.MemoryEntity
import com.example.data.model.MemoryTimelineEvent
import com.example.data.model.MemoryType
import com.example.data.model.SourceType
import com.example.data.remote.GeminiService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
// Robolectric 4.16 supports API 35; the production app still compiles against API 36.
@Config(sdk = [35])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Memento", appName)
    }

    @Test
    fun `test fallback parse for college id`() {
        val service = GeminiService("")
        val parsed = service.fallbackLocalParse("I kept my college ID in my black backpack.", SourceType.TEXT)
        assertEquals("College ID", parsed.title)
        assertEquals("Black backpack", parsed.location)
        assertEquals("CONFIRMED", parsed.evidenceLevel)
    }

    @Test
    fun `local parser never invents a location`() {
        val service = GeminiService("")
        val parsed = service.fallbackLocalParse("I need to remember my passport.", SourceType.TEXT)
        assertEquals("Passport", parsed.title)
        assertEquals(null, parsed.location)
    }

    @Test
    fun `test fallback retrieval for college id with location`() {
        val service = GeminiService("")
        val memory = MemoryEntity(
            id = "mem-1",
            userId = "test-user",
            type = MemoryType.THING,
            title = "College ID",
            content = "Kept in black backpack",
            location = "Black backpack",
            evidenceLevel = EvidenceLevel.CONFIRMED,
            sourceExplanation = "You told Memento earlier"
        )
        val result = service.fallbackLocalRetrieve("Where is my college ID?", listOf(memory), emptyList())
        assertTrue(result.isKnown)
        assertEquals("College ID", result.entity)
        assertEquals(EvidenceLevel.CONFIRMED, result.evidenceLevel)
        assertTrue(result.answer.contains("Black backpack"))
    }

    @Test
    fun `test fallback retrieval for unknown item says I don't know`() {
        val service = GeminiService("")
        val result = service.fallbackLocalRetrieve("Where is my gold watch?", emptyList(), emptyList())
        assertFalse(result.isKnown)
        assertTrue(result.answer.contains("I don't know"))
    }

    @Test
    fun `timeline location takes priority over an older memory location`() {
        val service = GeminiService("")
        val memory = MemoryEntity(
            id = "keys-1",
            userId = "test-user",
            type = MemoryType.THING,
            title = "Keys",
            content = "Keys were on the table",
            location = "Table",
            evidenceLevel = EvidenceLevel.CONFIRMED,
            sourceExplanation = "You told Memento earlier"
        )
        val moved = MemoryTimelineEvent(
            memoryId = "keys-1",
            userId = "test-user",
            entityTitle = "Keys",
            eventDescription = "Moved to the desk",
            location = "Desk"
        )

        val result = service.fallbackLocalRetrieve("Where are my keys?", listOf(memory), listOf(moved))
        assertTrue(result.isKnown)
        assertEquals("Desk", result.location)
        assertTrue(result.answer.contains("Desk"))
    }

    @Test
    fun `test room dao and repository insert memory`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.data.local.MementoDatabase::class.java
        ).allowMainThreadQueries().build()

        val dao = db.memoryDao()
        val repo = com.example.data.repository.MemoryRepository(dao, GeminiService(""))

        val memory = MemoryEntity(
            id = "mem-test-123",
            userId = "user-1",
            type = MemoryType.THING,
            title = "House Keys",
            content = "Placed on key hook near entrance",
            location = "Key hook",
            evidenceLevel = EvidenceLevel.CONFIRMED,
            sourceType = SourceType.TEXT,
            sourceExplanation = "User manual input"
        )

        repo.addMemory(memory)

        val retrieved = dao.getMemoryById("user-1", "mem-test-123")
        assertTrue(retrieved != null)
        assertEquals("House Keys", retrieved?.title)
        assertEquals("Key hook", retrieved?.location)

        db.close()
    }
}
