package com.example.bentoapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun rememberShimmerBrush(): Brush {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    )

    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnimation by transition.animateFloat(
        initialValue = -400f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = androidx.compose.ui.geometry.Offset(translateAnimation - 400f, 0f),
        end = androidx.compose.ui.geometry.Offset(translateAnimation, 0f)
    )
}

@Composable
fun ProjectCardSkeleton(
    shimmerBrush: Brush,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 0.5.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left thumbnail skeleton
                Box(
                    modifier = Modifier
                        .padding(10.dp)
                        .size(84.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(shimmerBrush)
                )

                // Title & Subtitle skeleton
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .height(26.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(shimmerBrush)
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .width(90.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(shimmerBrush)
                    )
                }

                // Chevron skeleton
                Box(
                    modifier = Modifier
                        .padding(end = 18.dp)
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(shimmerBrush)
                )
            }

            // Bottom Carousel skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .padding(bottom = 8.dp)
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    userScrollEnabled = false
                ) {
                    items(4) {
                        Box(
                            modifier = Modifier
                                .size(118.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(shimmerBrush)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardSkeletonView(
    topBarHeight: Dp = 110.dp,
    bottomBarHeight: Dp = 80.dp,
    modifier: Modifier = Modifier
) {
    val shimmerBrush = rememberShimmerBrush()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = topBarHeight + 8.dp,
            bottom = bottomBarHeight + 60.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false
    ) {
        items(3) {
            ProjectCardSkeleton(shimmerBrush = shimmerBrush)
        }
    }
}
