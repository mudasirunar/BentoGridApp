package com.example.bentoapp.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.example.bentoapp.ui.theme.TileAmber
import com.example.bentoapp.ui.theme.TileBlue
import com.example.bentoapp.ui.theme.TileLavender
import com.example.bentoapp.ui.theme.TileMint
import com.example.bentoapp.ui.theme.TileTeal
import com.example.bentoapp.ui.theme.TileViolet

/**
 * Reusable animated empty state illustration.
 * A bento box whose lid pops open, sending coloured tiles drifting upward — loops forever.
 * Drop into any empty state: DashboardScreen, CollectionDetailScreen, etc.
 *
 * Usage:
 *   BentoEmptyAnimation(modifier = Modifier.size(140.dp))
 */
@Composable
fun BentoEmptyAnimation(modifier: Modifier = Modifier) {

    val isDark = isSystemInDarkTheme()

    // ── Resolved colours from app theme ──────────────────────────────────
    val primary          = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val surface          = MaterialTheme.colorScheme.surface
    val onSurface        = MaterialTheme.colorScheme.onSurface

    // Box body colour — slightly distinct from primaryContainer for depth
    val boxBodyColor = if (isDark)
        Color(0xFF2D1B69)   // BentoPrimaryContainerDark
    else
        Color(0xFFEDE9FE)   // BentoPrimaryContainer light

    // Box lid — primary violet, both modes
    val lidColor = primary

    // Shadow colour
    val shadowColor = if (isDark)
        Color(0xFF000000).copy(alpha = 0.45f)
    else
        Color(0xFF7C3AED).copy(alpha = 0.14f)

    // Divider lines inside box
    val dividerColor = primary.copy(alpha = if (isDark) 0.22f else 0.13f)

    // Shine on lid
    val shineColor = Color.White.copy(alpha = if (isDark) 0.14f else 0.30f)

    // Latch dot
    val latchColor = surface.copy(alpha = 0.75f)

    // ── Tile colours — drawn from TileAccents in Color.kt ────────────────
    // 6 tiles with staggered phases
    val tileColors = listOf(
        TileLavender,   // pale violet   — tile 0
        TileViolet,     // core violet   — tile 1
        TileTeal,       // aurora teal   — tile 2
        TileAmber,      // warm amber    — tile 3
        TileBlue,       // sky blue      — tile 4
        TileMint,       // fresh green   — tile 5
    )
    // Sizes — varied so they feel organic
    val tileSizes   = listOf(13f, 10f, 15f, 11f, 12f,  9f)
    // Horizontal spread from centre of box opening
    val tileXDrift  = listOf(-20f, 9f, -6f, 17f, -14f, 3f)
    // Max rotation each tile reaches at progress=1
    val tileMaxRot  = listOf(20f, -25f, 38f, -14f, 28f, -32f)

    // ── Infinite transition ───────────────────────────────────────────────
    val transition = rememberInfiniteTransition(label = "emptyAnim")

    // Box gentle wobble left ↔ right
    val boxWobble by transition.animateFloat(
        initialValue = -2.5f,
        targetValue  =  2.5f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wobble"
    )

    // Lid: closed → snaps open → holds → closes → pause → repeat
    val lidAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue  = -24f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2800
                0f   at 0    using FastOutSlowInEasing  // closed
                -24f at 380  using FastOutSlowInEasing  // snaps open
                -20f at 650  using FastOutSlowInEasing  // tiny settle
                -24f at 900  using FastOutSlowInEasing  // holds open
                0f   at 1700 using FastOutSlowInEasing  // closes
                0f   at 2800 using LinearEasing         // pause closed
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "lid"
    )

    // 6 tile progress values, staggered 280ms apart
    @Composable
    fun tileAnim(delay: Int) = transition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1900, delayMillis = delay, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tile$delay"
    )

    val t0 by tileAnim(0)
    val t1 by tileAnim(280)
    val t2 by tileAnim(560)
    val t3 by tileAnim(840)
    val t4 by tileAnim(1120)
    val t5 by tileAnim(1400)

    val progresses = listOf(t0, t1, t2, t3, t4, t5)

    // ── Canvas ────────────────────────────────────────────────────────────
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(140.dp)) {

            val cx = size.width  / 2f
            val cy = size.height * 0.64f   // box anchored in lower portion

            val boxW    = 74.dp.toPx()
            val boxH    = 54.dp.toPx()
            val boxLeft = cx - boxW / 2f
            val boxTop  = cy - boxH / 2f
            val corner  = 13.dp.toPx()
            val lidH    = 15.dp.toPx()

            // ── 1. Floating tiles (drawn BEFORE box so they emerge from inside) ──
            progresses.forEachIndexed { i, p ->
                if (p > 0.01f) {
                    drawFloatingTile(
                        progress    = p,
                        originX     = cx + tileXDrift[i].dp.toPx(),
                        originY     = boxTop - 2.dp.toPx(),          // emerge from lid gap
                        tileSize    = tileSizes[i].dp.toPx(),
                        maxRotation = tileMaxRot[i],
                        color       = tileColors[i]
                    )
                }
            }

            // ── 2. Box — wobbles as a unit ────────────────────────────────
            rotate(degrees = boxWobble, pivot = Offset(cx, cy + 10.dp.toPx())) {

                // Drop shadow
                drawRoundRect(
                    color      = shadowColor,
                    topLeft    = Offset(boxLeft + 3.dp.toPx(), boxTop + 7.dp.toPx()),
                    size       = Size(boxW, boxH),
                    cornerRadius = CornerRadius(corner)
                )

                // Body
                drawRoundRect(
                    color      = boxBodyColor,
                    topLeft    = Offset(boxLeft, boxTop),
                    size       = Size(boxW, boxH),
                    cornerRadius = CornerRadius(corner)
                )

                // Inner vertical divider
                drawRoundRect(
                    color      = dividerColor,
                    topLeft    = Offset(cx - 1.dp.toPx(), boxTop + boxH * 0.12f),
                    size       = Size(2.dp.toPx(), boxH * 0.54f),
                    cornerRadius = CornerRadius(1.dp.toPx())
                )
                // Inner horizontal divider
                drawRoundRect(
                    color      = dividerColor,
                    topLeft    = Offset(boxLeft + boxW * 0.12f, cy - 1.dp.toPx()),
                    size       = Size(boxW * 0.76f, 2.dp.toPx()),
                    cornerRadius = CornerRadius(1.dp.toPx())
                )

                // Rim highlight strip
                drawRoundRect(
                    color      = shineColor.copy(alpha = shineColor.alpha * 0.6f),
                    topLeft    = Offset(boxLeft + 5.dp.toPx(), boxTop + 5.dp.toPx()),
                    size       = Size(boxW - 10.dp.toPx(), 3.dp.toPx()),
                    cornerRadius = CornerRadius(1.5.dp.toPx())
                )

                // ── 3. Lid — hinged at top-left corner of box ────────────
                translate(left = boxLeft, top = boxTop) {
                    rotate(degrees = lidAngle, pivot = Offset(0f, 0f)) {

                        // Lid shadow (slightly offset)
                        drawRoundRect(
                            color      = shadowColor.copy(alpha = shadowColor.alpha * 0.6f),
                            topLeft    = Offset(2.dp.toPx(), -lidH + 3.dp.toPx()),
                            size       = Size(boxW, lidH),
                            cornerRadius = CornerRadius(corner * 0.8f)
                        )

                        // Lid body
                        drawRoundRect(
                            color      = lidColor,
                            topLeft    = Offset(0f, -lidH),
                            size       = Size(boxW, lidH),
                            cornerRadius = CornerRadius(corner * 0.8f)
                        )

                        // Lid shine
                        drawRoundRect(
                            color      = shineColor,
                            topLeft    = Offset(7.dp.toPx(), -lidH + 4.dp.toPx()),
                            size       = Size(boxW - 14.dp.toPx(), 3.5.dp.toPx()),
                            cornerRadius = CornerRadius(1.5.dp.toPx())
                        )

                        // Latch dot
                        drawCircle(
                            color  = latchColor,
                            radius = 3.dp.toPx(),
                            center = Offset(boxW / 2f, -lidH / 2f)
                        )
                    }
                }
            }
        }
    }
}

// ── Helper — draws one floating tile ─────────────────────────────────────────
private fun DrawScope.drawFloatingTile(
    progress: Float,        // 0 = just appeared at origin, 1 = fully gone
    originX: Float,         // horizontal centre point (varies per tile)
    originY: Float,         // vertical start (top of box / lid gap)
    tileSize: Float,
    maxRotation: Float,     // degrees at progress = 1
    color: Color
) {
    // Alpha envelope: fade in → hold → fade out
    val alpha = when {
        progress < 0.18f -> progress / 0.18f
        progress < 0.72f -> 1f
        else             -> 1f - (progress - 0.72f) / 0.28f
    }

    // Y rises upward from origin
    val riseTotal = 85.dp.toPx()
    val y = originY - progress * riseTotal

    // Gentle horizontal drift (S-curve feel without sin)
    val xCurve = (progress - 0.5f) * tileSize * 2.2f
    val x = originX + xCurve

    translate(left = x - tileSize / 2f, top = y - tileSize / 2f) {
        rotate(
            degrees = maxRotation * progress,
            pivot   = Offset(tileSize / 2f, tileSize / 2f)
        ) {
            drawRoundRect(
                color        = color.copy(alpha = alpha.coerceIn(0f, 1f)),
                topLeft      = Offset.Zero,
                size         = Size(tileSize, tileSize),
                cornerRadius = CornerRadius(tileSize * 0.3f)
            )
        }
    }
}