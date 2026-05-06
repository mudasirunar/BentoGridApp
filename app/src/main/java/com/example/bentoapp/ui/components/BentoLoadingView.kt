package com.example.bentoapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BentoLoadingView(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    // Animate alpha for a soft "breathing" effect
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaPulse"
    )
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ── Skeleton Grid ──
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LoadingTile(size = 80.dp, alpha = alpha)
                LoadingTile(size = 80.dp, alpha = alpha)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LoadingTile(size = 80.dp, alpha = alpha)
                LoadingTile(size = 80.dp, alpha = alpha)
            }
        }
        Spacer(Modifier.height(24.dp))
        // ── Animated Loading Text ──
        Text(
            text = "Loading your grid...",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
            letterSpacing = 1.2.sp
        )
    }
}
@Composable
private fun LoadingTile(size: androidx.compose.ui.unit.Dp, alpha: Float) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha * 0.3f)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = alpha * 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
    )
}