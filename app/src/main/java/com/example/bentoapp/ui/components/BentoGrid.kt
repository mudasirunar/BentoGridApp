package com.example.bentoapp.ui.components

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.example.bentoapp.data.BentoEntity

data class PackedTile(
    val tile: BentoEntity,
    val startCol: Int,
    val topDp: Int
)

/**
 * RESTORED: Original first-fit packing logic.
 * Fixed heights ensure the Bento aesthetic remains identical to your original version.
 */
fun packTiles(
    tiles: List<BentoEntity>,
    numCols: Int,
    gapDp: Int,
    unitHeight: Float
): List<PackedTile> {
    if (tiles.isEmpty()) return emptyList()

    val occupied = HashSet<Long>()
    fun key(col: Int, row: Int): Long = col * 1_000_000L + row

    val result = mutableListOf<PackedTile>()

    for (tile in tiles) {
        val span = tile.shape.colSpan.coerceIn(1, numCols)
        val verticalUnits = (tile.shape.heightDp / 80f).toInt()

        var placed = false
        var currentRow = 0
        while (!placed) {
            for (startCol in 0..numCols - span) {
                // Check if space is free
                val isFree = (startCol until startCol + span).all { c ->
                    (currentRow until currentRow + verticalUnits).all { r ->
                        key(c, r) !in occupied
                    }
                }

                if (isFree) {
                    // Mark as occupied
                    for (c in startCol until startCol + span) {
                        for (r in currentRow until currentRow + verticalUnits) {
                            occupied.add(key(c, r))
                        }
                    }

                    // Calculate TopDp based on the dynamic unitHeight
                    val topDp = (currentRow * (unitHeight + gapDp)).toInt()

                    result.add(PackedTile(tile, startCol, topDp))
                    placed = true
                    break
                }
            }
            if (!placed) currentRow++
        }
    }
    return result
}

@Composable
fun BentoGrid(
    tiles: List<BentoEntity>,
    shapeIndex: Int,
    modifier: Modifier = Modifier,
    gapDp: Int = 8,
    initialLoad: Boolean,
    isSelectionMode: Boolean = false,
    selectedIds: Set<Int> = emptySet(),
    onTileClick: (BentoEntity) -> Unit,
    onTileLongClick: (BentoEntity) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val numCols = if (isLandscape) 8 else 4

    BoxWithConstraints(
        modifier = modifier.animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow))
    ) {
        val totalWidthDp = maxWidth
        val gapPadding = gapDp.dp
        val colWidthDp = (totalWidthDp - (gapPadding * (numCols - 1))) / numCols
        val unitHeight = colWidthDp

        val packed = remember(tiles, numCols, totalWidthDp) {
            packTiles(tiles, numCols, gapDp, unitHeight.value)
        }

        // --- RESTORED ANIMATION LOGIC ---
        var globalAlpha by remember { mutableStateOf(if (initialLoad) 0f else 1f) }

        LaunchedEffect(Unit) {
            if (initialLoad) {
                kotlinx.coroutines.delay(300)
                globalAlpha = 1f
            }
        }

        packed.forEachIndexed { index, p ->
            key(p.tile.id) {
                val span = p.tile.shape.colSpan
                val targetWidth = (colWidthDp * span) + (gapPadding * (span - 1))
                val hMultiplier = p.tile.shape.heightDp / 80f
                val targetHeight = (hMultiplier * unitHeight) + (hMultiplier - 1) * gapPadding

                val animatedX by animateDpAsState(targetValue = p.startCol * (colWidthDp + gapPadding))
                val animatedY by animateDpAsState(targetValue = p.topDp.dp)

                var itemAppeared by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { itemAppeared = true }

                val entryScale by animateFloatAsState(
                    targetValue = if (itemAppeared && globalAlpha > 0.5f) 1f else 0.85f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "entryScale"
                )

                val entryAlpha by animateFloatAsState(
                    targetValue = if (itemAppeared && globalAlpha > 0.5f) 1f else 0f,
                    animationSpec = tween(
                        durationMillis = 500,
                        delayMillis = if (initialLoad) index * 40 else 0
                    ),
                    label = "entryAlpha"
                )

                Box(
                    modifier = Modifier
                        .offset(x = animatedX, y = animatedY)
                        .size(width = targetWidth, height = targetHeight)
                        .graphicsLayer {
                            alpha = entryAlpha
                            scaleX = entryScale
                            scaleY = entryScale
                        }
                ) {
                    BentoTile(
                        tile = p.tile,
                        shapeIndex = shapeIndex,
                        onClick = { onTileClick(p.tile) },
                        onLongClick = { onTileLongClick(p.tile) }
                    )

                    if (isSelectionMode) {
                        val isSelected = selectedIds.contains(p.tile.id)
                        val overlayShape = when (shapeIndex) {
                            0 -> RoundedCornerShape(4.dp)
                            1 -> RoundedCornerShape(24.dp)
                            else -> CircleShape
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(overlayShape)
                                .background(Color.Black.copy(alpha = if (isSelected) 0.3f else 0.1f))
                                .clickable { onTileClick(p.tile) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        val totalHeightDp = if (packed.isEmpty()) 0.dp
        else packed.maxOf {
            val hM = it.tile.shape.heightDp / 80f
            it.topDp + (hM * unitHeight.value) + (hM - 1) * gapDp
        }.dp

        Spacer(modifier = Modifier.fillMaxWidth().height(totalHeightDp + 32.dp))
    }
}