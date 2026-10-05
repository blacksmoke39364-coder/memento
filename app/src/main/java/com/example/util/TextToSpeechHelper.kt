package com.example.util

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.MemoryEntity
import java.util.Locale

class TextToSpeechManager(context: Context) {
    private var tts: TextToSpeech? = null
    var isInitialized by mutableStateOf(false)
        private set

    var currentlySpeakingId by mutableStateOf<String?>(null)
        private set

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                tts?.setSpeechRate(0.95f) // Natural speech rate
                tts?.setPitch(1.0f)
                isInitialized = true

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        currentlySpeakingId = utteranceId
                    }

                    override fun onDone(utteranceId: String?) {
                        if (currentlySpeakingId == utteranceId) {
                            currentlySpeakingId = null
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        if (currentlySpeakingId == utteranceId) {
                            currentlySpeakingId = null
                        }
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        if (currentlySpeakingId == utteranceId) {
                            currentlySpeakingId = null
                        }
                    }
                })
            }
        }
    }

    fun speakMemory(memory: MemoryEntity) {
        if (!isInitialized) return

        if (currentlySpeakingId == memory.id) {
            stop()
            return
        }

        val speechBuilder = StringBuilder()
        speechBuilder.append(memory.title).append(". ")
        if (memory.content.isNotBlank() && !memory.content.equals(memory.title, ignoreCase = true)) {
            speechBuilder.append(memory.content).append(". ")
        }
        memory.location?.let { loc ->
            speechBuilder.append("Location: ").append(loc).append(". ")
        }
        memory.person?.let { p ->
            speechBuilder.append("Person: ").append(p).append(". ")
        }

        speakText(memory.id, speechBuilder.toString())
    }

    fun speakText(id: String, text: String) {
        if (!isInitialized || text.isBlank()) return
        stop()
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, id)
        currentlySpeakingId = id
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, id)
    }

    fun stop() {
        tts?.stop()
        currentlySpeakingId = null
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}

@Composable
fun rememberTextToSpeechManager(): TextToSpeechManager {
    val context = LocalContext.current
    val manager = remember { TextToSpeechManager(context) }

    DisposableEffect(manager) {
        onDispose {
            manager.shutdown()
        }
    }

    return manager
}
