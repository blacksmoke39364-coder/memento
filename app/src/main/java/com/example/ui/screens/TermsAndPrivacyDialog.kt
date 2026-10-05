package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.MementoCyan

@Composable
fun TermsAndPrivacyDialog(
    isPrivacyPolicy: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isPrivacyPolicy) Icons.Default.Security else Icons.Default.Gavel,
                        contentDescription = null,
                        tint = MementoCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isPrivacyPolicy) "Privacy Policy" else "Terms of Service",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_legal_dialog_btn")) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (isPrivacyPolicy) {
                        Text(
                            text = "MEMENTO PRIVACY POLICY",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MementoCyan
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = """
1. PRIVACY BY DESIGN
Memento is an intentional personal external memory system. We operate under the core principle: "You control your memories."

2. WHAT WE COLLECT & PROCESS
• Memories: Text, objects, people, locations, and promises you intentionally record.
• Media: Photos or voice notes you explicitly submit. Memento does not continuously monitor camera, microphone, or GPS.
• Session Data: Authenticated user identifier for strict sandbox isolation.

3. SENSORS & PERMISSIONS
• Microphone: Accessed strictly during tap-to-record voice sessions.
• Camera: Accessed strictly during intentional image capture.
• No continuous listening, recording, or background telemetry.

4. USER DATA ISOLATION & STORAGE
• All memory records are guarded by strict user isolation. User A can never access User B's memories.
• Stored memories are treated strictly as data, never as system instructions (prompt-injection protection).

5. RETENTION & RIGHT TO ERASURE
• Users may export their entire memory vault as JSON at any time.
• Users can edit, update, or permanently delete individual memories, categories, or purge the entire vault.
• Deletion is permanent and irreversible.

6. COMPLIANCE & LEGAL
Designed with applicable data protection requirements in mind, including Indian Digital Personal Data Protection Act (DPDPA) principles and global security baselines (OWASP ASVS).
                            """.trimIndent(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    } else {
                        Text(
                            text = "MEMENTO TERMS OF SERVICE",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MementoCyan
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = """
1. ACCEPTANCE OF TERMS
By accessing or using Memento, you agree to be bound by these Terms of Service.

2. CORE FUNCTIONALITY & PURPOSE
Memento is designed as an external personal memory vault. Memento distinguishes between:
• Confirmed: Information you explicitly stated.
• Observed: Information detected from permitted user-provided media.
• Inferred: Derived estimates.
If Memento does not possess sufficient evidence, it will state "I don't know."

3. USER RESPONSIBILITIES
You are responsible for the content you record in your vault. Do not store malicious code or use the service for prohibited activities.

4. AI LIMITATIONS & DISCLAIMER
• AI can make errors. Inferred information is not confirmed fact.
• Important real-world, financial, or medical decisions should always be independently verified.
• Memento is provided on an "as is" and "as available" basis.

5. DATA SOVEREIGNTY
You retain full ownership of all memories, texts, and media recorded in your Memento account. You may export or delete your data at any time.
                            """.trimIndent(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().testTag("legal_accept_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MementoCyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
