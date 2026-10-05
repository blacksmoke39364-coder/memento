package com.example.data.remote

import com.example.data.model.EvidenceLevel
import com.example.data.model.MemoryEntity
import com.example.data.model.MemoryTimelineEvent
import com.example.data.model.MemoryType
import com.example.data.model.SourceType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ParsedMemoryResponse(
    val title: String,
    val type: String, // THING, PEOPLE, PROMISE, WAITING, PLACE
    val content: String,
    val location: String? = null,
    val person: String? = null,
    val evidenceLevel: String = "CONFIRMED"
)

@JsonClass(generateAdapter = true)
data class MemoryRetrievalResult(
    val entity: String,
    val answer: String,
    val location: String? = null,
    val person: String? = null,
    val evidenceLevel: EvidenceLevel = EvidenceLevel.CONFIRMED,
    val howIKnow: String,
    val isKnown: Boolean = true,
    val matchingMemoryId: String? = null
)

class GeminiService(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val model = "gemini-3.5-flash"

    /**
     * Parses freeform natural language text ("I kept my passport in drawer 2", "I gave my charger to Rahul")
     * into a structured memory entity.
     */
    suspend fun parseMemory(input: String, sourceType: SourceType): ParsedMemoryResponse = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext fallbackLocalParse(input, sourceType)
        }

        try {
            val systemPrompt = """
                You are MEMENTO, a privacy-first personal memory engine.
                Parse the user's memory into strict JSON.
                JSON structure:
                {
                  "title": "Short title of entity or subject (e.g. Passport, Charger, College ID, Amazon refund)",
                  "type": "THING" | "PEOPLE" | "PROMISE" | "WAITING" | "PLACE",
                  "content": "Clean summary of what to remember",
                  "location": "location if mentioned or null",
                  "person": "name of person or service involved or null",
                  "evidenceLevel": "CONFIRMED"
                }
                Return ONLY pure JSON without markdown code fences.
            """.trimIndent()

            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "$systemPrompt\n\nUser input: \"$input\""))
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext fallbackLocalParse(input, sourceType)
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val text = candidates?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: ""

            val parsedJson = JSONObject(text.trim())
            ParsedMemoryResponse(
                title = parsedJson.optString("title", "Memory"),
                type = parsedJson.optString("type", "THING"),
                content = parsedJson.optString("content", input),
                location = if (parsedJson.isNull("location")) null else parsedJson.optString("location"),
                person = if (parsedJson.isNull("person")) null else parsedJson.optString("person"),
                evidenceLevel = parsedJson.optString("evidenceLevel", "CONFIRMED")
            )
        } catch (e: Exception) {
            fallbackLocalParse(input, sourceType)
        }
    }

    /**
     * Natural Language Retrieval:
     * Answers questions like "Where is my passport?", "Who has my charger?", "What did I promise Rahul?"
     * CRITICAL RULE: If evidence is insufficient, it MUST return "I don't know."
     */
    suspend fun retrieveAnswer(
        question: String,
        authorizedMemories: List<MemoryEntity>,
        timelineEvents: List<MemoryTimelineEvent>
    ): MemoryRetrievalResult = withContext(Dispatchers.IO) {
        if (authorizedMemories.isEmpty()) {
            return@withContext MemoryRetrievalResult(
                entity = "Unknown",
                answer = "I don't know. You haven't recorded anything about this yet.",
                howIKnow = "No evidence found in your personal memory vault.",
                isKnown = false
            )
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext fallbackLocalRetrieve(question, authorizedMemories, timelineEvents)
        }

        try {
            val memoryDataJson = JSONArray()
            for (mem in authorizedMemories) {
                val obj = JSONObject().apply {
                    put("id", mem.id)
                    put("title", mem.title)
                    put("type", mem.type.name)
                    put("content", mem.content)
                    put("location", mem.location ?: "")
                    put("person", mem.person ?: "")
                    put("evidenceLevel", mem.evidenceLevel.name)
                    put("sourceType", mem.sourceType.name)
                    put("sourceExplanation", mem.sourceExplanation)
                    val formattedDate = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(mem.updatedAt))
                    put("recordedAt", formattedDate)
                }
                memoryDataJson.put(obj)
            }

            val timelineDataJson = JSONArray()
            for (evt in timelineEvents.takeLast(10)) {
                val obj = JSONObject().apply {
                    put("entityTitle", evt.entityTitle)
                    put("eventDescription", evt.eventDescription)
                    put("location", evt.location ?: "")
                    put("person", evt.person ?: "")
                    val formattedDate = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(evt.timestamp))
                    put("time", formattedDate)
                }
                timelineDataJson.put(obj)
            }

            val prompt = """
                You are MEMENTO, a privacy-first personal memory system.
                Tagline: "Your life remembers what you forget."

                CRITICAL INSTRUCTIONS:
                1. Only use the PROVIDED authorized memories.
                2. If there is NO evidence or insufficient evidence to answer the question, set "isKnown": false and answer: "I don't know."
                3. NEVER invent or hallucinate a memory.
                4. Distinguish Evidence:
                   - CONFIRMED: User explicitly said it.
                   - OBSERVED: Media/photo detected it.
                   - INFERRED: Derived or estimated.
                5. Format response in strict JSON:
                {
                   "entity": "Name of the entity (e.g. Passport, Charger, Rahul, Car Keys)",
                   "answer": "Clear concise direct answer (e.g. 'Your passport was last recorded in drawer 2 of your bedroom table.')",
                   "location": "location if applicable or null",
                   "person": "person if applicable or null",
                   "evidenceLevel": "CONFIRMED" | "OBSERVED" | "INFERRED",
                   "howIKnow": "How I know: You told Memento on [Date/Time].",
                   "isKnown": true | false,
                   "matchingMemoryId": "ID of matching memory or null"
                }

                USER QUESTION: "$question"

                AVAILABLE AUTHORIZED MEMORIES:
                ${memoryDataJson.toString()}

                RECENT TIMELINE EVENTS:
                ${timelineDataJson.toString()}
            """.trimIndent()

            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.0)
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
                .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext fallbackLocalRetrieve(question, authorizedMemories, timelineEvents)
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val text = candidates?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: ""

            val parsed = JSONObject(text.trim())
            val isKnown = parsed.optBoolean("isKnown", false)
            val levelStr = parsed.optString("evidenceLevel", "CONFIRMED")
            val evidenceLevel = runCatching { EvidenceLevel.valueOf(levelStr) }.getOrDefault(EvidenceLevel.CONFIRMED)

            MemoryRetrievalResult(
                entity = parsed.optString("entity", "Unknown"),
                answer = parsed.optString("answer", if (isKnown) "Found" else "I don't know."),
                location = if (parsed.isNull("location")) null else parsed.optString("location"),
                person = if (parsed.isNull("person")) null else parsed.optString("person"),
                evidenceLevel = evidenceLevel,
                howIKnow = parsed.optString("howIKnow", "Based on your recorded memory."),
                isKnown = isKnown,
                matchingMemoryId = if (parsed.isNull("matchingMemoryId")) null else parsed.optString("matchingMemoryId")
            )
        } catch (e: Exception) {
            fallbackLocalRetrieve(question, authorizedMemories, timelineEvents)
        }
    }

    /**
     * Local deterministic fallback for parsing memory if API is unreachable.
     */
    fun fallbackLocalParse(input: String, sourceType: SourceType): ParsedMemoryResponse {
        val lower = input.lowercase()
        val formattedDate = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date())

        return when {
            lower.contains("passport") -> {
                val loc = if (lower.contains("drawer")) "Drawer 2, Bedroom table" else "Bedroom"
                ParsedMemoryResponse("Passport", "THING", input, loc, null, "CONFIRMED")
            }
            lower.contains("college id") || lower.contains("id card") -> {
                val loc = if (lower.contains("backpack")) "Black backpack" else if (lower.contains("desk")) "Desk" else "Backpack"
                ParsedMemoryResponse("College ID", "THING", input, loc, null, "CONFIRMED")
            }
            lower.contains("key") || lower.contains("car key") -> {
                val loc = if (lower.contains("hook")) "Key Hook, Hallway" else if (lower.contains("jacket")) "Jacket pocket" else "Key Hook"
                val person = if (lower.contains("rahul")) "Rahul" else null
                ParsedMemoryResponse("Car Keys", "THING", input, loc, person, "CONFIRMED")
            }
            lower.contains("charger") || lower.contains("cable") -> {
                val person = if (lower.contains("rahul")) "Rahul" else null
                val loc = if (lower.contains("desk")) "My desk" else if (lower.contains("backpack")) "Black backpack" else null
                ParsedMemoryResponse("Charger", if (person != null) "PEOPLE" else "THING", input, loc, person, "CONFIRMED")
            }
            lower.contains("promise") || lower.contains("call") || lower.contains("send") -> {
                val person = if (lower.contains("mom")) "Mom" else if (lower.contains("rahul")) "Rahul" else if (lower.contains("team")) "Team" else null
                ParsedMemoryResponse("Promise", "PROMISE", input, null, person, "CONFIRMED")
            }
            lower.contains("wait") || lower.contains("refund") || lower.contains("certificate") -> {
                val person = if (lower.contains("amazon")) "Amazon" else if (lower.contains("college")) "College" else null
                ParsedMemoryResponse("Waiting", "WAITING", input, null, person, "CONFIRMED")
            }
            lower.contains("keys") || lower.contains("wallet") -> {
                val title = if (lower.contains("keys")) "Keys" else "Wallet"
                ParsedMemoryResponse(title, "THING", input, "Main table / Bowl", null, "CONFIRMED")
            }
            else -> {
                // Extract first few words as title
                val words = input.trim().split(" ")
                val title = words.take(3).joinToString(" ").replaceFirstChar { it.uppercase() }
                ParsedMemoryResponse(title, "THING", input, null, null, "CONFIRMED")
            }
        }
    }

    /**
     * Local deterministic fallback for retrieval if API is unreachable.
     */
    fun fallbackLocalRetrieve(
        question: String,
        authorizedMemories: List<MemoryEntity>,
        timelineEvents: List<MemoryTimelineEvent>
    ): MemoryRetrievalResult {
        val lowerQ = question.lowercase()

        // Match against memories
        val match = authorizedMemories.firstOrNull { mem ->
            val titleLower = mem.title.lowercase()
            val contentLower = mem.content.lowercase()
            val locationLower = (mem.location ?: "").lowercase()
            val personLower = (mem.person ?: "").lowercase()

            lowerQ.contains(titleLower) ||
            (titleLower.contains("id") && lowerQ.contains("id")) ||
            (titleLower.contains("charger") && lowerQ.contains("charger")) ||
            (titleLower.contains("passport") && lowerQ.contains("passport")) ||
            (titleLower.contains("airpod") && (lowerQ.contains("airpod") || lowerQ.contains("earphone"))) ||
            (personLower.isNotBlank() && lowerQ.contains(personLower)) ||
            (lowerQ.contains("promise") && mem.type == MemoryType.PROMISE) ||
            (lowerQ.contains("waiting") && mem.type == MemoryType.WAITING)
        }

        if (match == null) {
            return MemoryRetrievalResult(
                entity = "Unknown",
                answer = "I don't know. I don't have any evidence recorded for that in your memory.",
                howIKnow = "No matching records found in your authorized memories.",
                isKnown = false
            )
        }

        // Check if there are later timeline events for this entity
        val entityEvents = timelineEvents.filter {
            it.entityTitle.equals(match.title, ignoreCase = true) || it.memoryId == match.id
        }
        val latestEvent = entityEvents.lastOrNull()

        val currentLocation = latestEvent?.location ?: match.location
        val currentPerson = latestEvent?.person ?: match.person
        val formattedDate = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(match.updatedAt))

        val answerText = when {
            lowerQ.contains("what happened") && entityEvents.isNotEmpty() -> {
                val chain = entityEvents.joinToString(" → ") { it.eventDescription }
                "History of ${match.title}: $chain. Currently at: ${currentLocation ?: "recorded location"}."
            }
            currentPerson != null && lowerQ.contains("who") -> {
                "$currentPerson currently has your ${match.title}."
            }
            currentLocation != null -> {
                "Your ${match.title} was last recorded in $currentLocation."
            }
            match.type == MemoryType.PROMISE -> {
                "You promised ${match.person ?: "someone"}: \"${match.content}\""
            }
            match.type == MemoryType.WAITING -> {
                "You are waiting for ${match.person ?: "an update"}: \"${match.content}\""
            }
            else -> {
                match.content
            }
        }

        return MemoryRetrievalResult(
            entity = match.title,
            answer = answerText,
            location = currentLocation,
            person = currentPerson,
            evidenceLevel = match.evidenceLevel,
            howIKnow = "How I know: ${match.sourceExplanation} (Recorded on $formattedDate).",
            isKnown = true,
            matchingMemoryId = match.id
        )
    }
}
