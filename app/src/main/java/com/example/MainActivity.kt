package com.example

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.example.ui.navigation.MementoApp

class MainActivity : ComponentActivity() {

    companion object {
        val triggerGeminiVoice = mutableStateOf(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleVoiceIntent(intent)
        setContent {
            MementoApp(
                externalVoiceTrigger = triggerGeminiVoice.value,
                onResetVoiceTrigger = { triggerGeminiVoice.value = false }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleVoiceIntent(intent)
    }

    private fun handleVoiceIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_ASSIST ||
            intent?.action == "android.intent.action.VOICE_ASSIST" ||
            intent?.getBooleanExtra("EXTRA_START_VOICE", false) == true
        ) {
            triggerGeminiVoice.value = true
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Intercept Power Button, Assist Key, Headset Hook, or Voice Assist Key
        if (keyCode == KeyEvent.KEYCODE_POWER ||
            keyCode == KeyEvent.KEYCODE_VOICE_ASSIST ||
            keyCode == KeyEvent.KEYCODE_ASSIST ||
            keyCode == KeyEvent.KEYCODE_HEADSETHOOK
        ) {
            triggerGeminiVoice.value = true
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
