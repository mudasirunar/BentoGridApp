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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BentoLoadingView(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

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
fun GalleryShimmer(
    columns: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.22f, 
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val boxColor = MaterialTheme.colorScheme.onSurface.copy(alpha = pulseAlpha)

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Box(
            modifier = Modifier
                .padding(vertical = 24.dp, horizontal = 4.dp)
                .width(140.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(boxColor)
        )

        val spacing = 8.dp
        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            repeat(4) {
                BentoShimmerRow(columns, boxColor, spacing, 0)
                BentoShimmerRow(columns, boxColor, spacing, 1)
                BentoShimmerRow(columns, boxColor, spacing, 2)
                BentoShimmerRow(columns, boxColor, spacing, 0)
            }
        }
    }
}

@Composable
private fun BentoShimmerRow(columns: Int, color: Color, spacing: androidx.compose.ui.unit.Dp, type: Int) {
    val shape = RoundedCornerShape(4.dp)

    when (type) {
        0 -> { // Row of 1x1 Squares
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                repeat(columns) {
                    Box(Modifier.weight(1f).aspectRatio(1f).clip(shape).background(color))
                }
            }
        }
        1 -> { // 2x2 Feature on Left + 1x1 Stacks
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                Box(Modifier.weight(2f).fillMaxHeight().clip(shape).background(color))
                
                repeat(columns - 2) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing)) {
                        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(shape).background(color))
                        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(shape).background(color))
                    }
                }
            }
        }
        2 -> { // 2x2 Feature on Right + 1x1 Stacks
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                repeat(columns - 2) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing)) {
                        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(shape).background(color))
                        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(shape).background(color))
                    }
                }
                Box(Modifier.weight(2f).fillMaxHeight().clip(shape).background(color))
            }
        }
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
