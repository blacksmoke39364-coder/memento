package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
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
import com.example.data.model.EvidenceLevel
import com.example.ui.theme.EvidenceConfirmed
import com.example.ui.theme.EvidenceConfirmedBg
import com.example.ui.theme.EvidenceInferred
import com.example.ui.theme.EvidenceInferredBg
import com.example.ui.theme.EvidenceObserved
import com.example.ui.theme.EvidenceObservedBg

@Composable
fun EvidenceBadge(
    evidenceLevel: EvidenceLevel,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val (label, dotColor, bgColor, icon) = when (evidenceLevel) {
        EvidenceLevel.CONFIRMED -> Quad(
            "CONFIRMED",
            EvidenceConfirmed,
            EvidenceConfirmedBg.copy(alpha = 0.35f),
            Icons.Default.CheckCircle
        )
        EvidenceLevel.OBSERVED -> Quad(
            "OBSERVED",
            EvidenceObserved,
            EvidenceObservedBg.copy(alpha = 0.35f),
            Icons.Default.Visibility
        )
        EvidenceLevel.INFERRED -> Quad(
            "INFERRED",
            EvidenceInferred,
            EvidenceInferredBg.copy(alpha = 0.35f),
            Icons.Default.AutoAwesome
        )
    }

    Row(
        modifier = modifier
            .testTag("evidence_badge_${evidenceLevel.name.lowercase()}")
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = dotColor,
            modifier = Modifier.size(11.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = dotColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
