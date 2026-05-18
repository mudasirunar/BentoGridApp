package com.example.bentoapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bentoapp.R

/**
 * @param compact  When true: removes bottom padding and float animation
 *                 so the FAB sits flush inside a Row with other buttons.
 *                 Breathing + press animations still run.
 * @param label    Button label text. Default = "New Collection" (dashboard).
 *                 Pass "Add Tile" for the collection detail screen.
 */
@Composable
fun BentoFab(
    onClick: () -> Unit,
    visible: Boolean = true,
    label: String = "New Collection",
    compact: Boolean = false
) {
    var isPressed by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "fabLoop")
    val density = androidx.compose.ui.platform.LocalDensity.current

    // Breathing scale — always runs
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // Float Y — only in standalone (dashboard) mode
    val floatY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (compact) 0f else with(density) { -12.dp.toPx() },
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    // Visibility slide — only used in standalone mode
    val visibilityOffset by animateFloatAsState(
        targetValue = if (!compact && !visible) with(density) { 100.dp.toPx() } else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "slideAnimation"
    )

    val visibilityAlpha by animateFloatAsState(
        targetValue = if (!compact && !visible) 0f else 1f,
        animationSpec = tween(300),
        label = "alphaAnimation"
    )

    // Icon rotation on press
    val iconRotation by animateFloatAsState(
        targetValue = if (isPressed) 45f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconRotation"
    )

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "pressScale"
    )

    Box(
        modifier = Modifier
            // Bottom padding only in standalone dashboard mode
            .then(if (!compact) Modifier.padding(bottom = 4.dp) else Modifier)
            .graphicsLayer {
                scaleX = pressScale * breathingScale
                scaleY = pressScale * breathingScale
                translationY = floatY + visibilityOffset
                alpha = visibilityAlpha
            },
        contentAlignment = Alignment.Center
    ) {
        if (visibilityAlpha > 0f) {
            Row(
                modifier = Modifier
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(50),
                        ambientColor = Color(0xFF7C3AED).copy(alpha = 0.5f),
                        spotColor = Color(0xFF7C3AED).copy(alpha = 0.6f)
                    )
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF7C3AED),
                                Color(0xFF9333EA),
                                Color(0xFFA855F7)
                            )
                        )
                    )
                    .then(
                        if (if (compact) true else visible) {
                            Modifier.clickable {
                                isPressed = true
                                onClick()
                            }
                        } else Modifier
                    )
                    .padding(
                        horizontal = if (compact) 32.dp else 28.dp,
                        vertical   = if (compact) 8.dp else 9.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(if (compact) 34.dp else 38.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .size(if (compact) 30.dp else 35.dp)
                            .graphicsLayer { rotationZ = iconRotation }
                    )
                }

                Text(
                    text = label,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 15.sp else 17.sp,
                    letterSpacing = 0.2.sp
                )
            }
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(400)
            isPressed = false
        }
    }
}