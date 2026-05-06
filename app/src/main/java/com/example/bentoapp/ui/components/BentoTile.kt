package com.example.bentoapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.bentoapp.data.BentoEntity
import com.example.bentoapp.data.TileShape
import com.example.bentoapp.ui.theme.BentoPalette


@Composable
fun BentoTile(
    tile: BentoEntity,
    shapeIndex: Int = 1,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val tileShape = tile.shape
    val isCircular = shapeIndex == 2
    val cardCornerShape = when(shapeIndex) {
        0 -> RoundedCornerShape(4.dp)
        1 -> RoundedCornerShape(24.dp)
        else -> androidx.compose.foundation.shape.CircleShape // Circular (becomes Capsule for rects)
    }
    val backgroundBrush = BentoPalette.getBrush(tile.backgroundColor)
    val horizontalPadding = when {
        isCircular && tileShape.colSpan == 1 -> 28.dp // 1x1 Circle needs deep padding
        isCircular -> 40.dp                          // Wide/Tall capsules need deep side padding
        tileShape.heightDp <= 80 || tileShape.colSpan == 1 -> 12.dp
        else -> 18.dp
    }

    val verticalPadding = when {
        isCircular -> 20.dp                           // Clear the top/bottom arcs
        tileShape.heightDp <= 80 || tileShape.colSpan == 1 -> 12.dp
        else -> 18.dp
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(cardCornerShape)
            .background(backgroundBrush)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        if (!tile.imageUri.isNullOrEmpty()) {
            AsyncImage(
                model = tile.imageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            // RESTORED: SpaceBetween keeps things in their corners properly
            verticalArrangement = if (tile.textAlignment == 1) Arrangement.Center else Arrangement.SpaceBetween,
            horizontalAlignment = when (tile.textAlignment) {
                1 -> Alignment.CenterHorizontally
                2 -> Alignment.End
                else -> Alignment.Start
            }
        ) {
            // ── THE SWAP ENGINE ──
            val UIBlocks = remember(
                tile.title,
                tile.content,
                tile.isReversed,
                tile.textSizeOffset,
                tile.isBold,
                tile.isItalic,
                tile.isUnderline,
                tile.textColor,
                tile.contentTextColor,
                tile.isContentBold,
                tile.isContentItalic,
                tile.isContentUnderline,
                tile.contentSizeOffset
            ) {
                val blocks = mutableListOf<@Composable ColumnScope.() -> Unit>()
                blocks.add { TitleWithScrim(tile, tileShape, tile.textAlignment == 1) }
                blocks.add { ContentArea(tile, tileShape, tile.textAlignment == 1) }

                if (tile.isReversed) blocks.reversed() else blocks
            }

            UIBlocks.forEachIndexed { index, block ->
                block()
                if (index == 0 && tile.title.isNotEmpty() && tile.content.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.TitleWithScrim(
    tile: BentoEntity,
    shape: TileShape,
    isCenter: Boolean
) {
    val displayTitle = when {
        tile.title.isNotEmpty() -> tile.title
        tile.id == 0 -> "Tile Title"
        else -> ""
    }
    if (displayTitle.isEmpty()) return

    val baseFontSize = when {
        shape == TileShape.WIDE || shape == TileShape.TALL -> 24.sp
        shape == TileShape.SMALL_V -> 16.sp
        shape == TileShape.SMALL_H -> 18.sp
        else -> 20.sp
    }
    val finalFontSize = (baseFontSize.value + (tile.textSizeOffset * 4)).sp

    Text(
        text = displayTitle,
        modifier = Modifier
            .weight(1f, fill = false)
            .heightIn(min = 0.dp),
        style = MaterialTheme.typography.titleMedium.copy(
            fontSize = finalFontSize,
            lineHeight = (finalFontSize.value * 1.1).sp,
            fontWeight = if (tile.isBold) FontWeight.Black else FontWeight.Light,
            fontFamily = if (tile.isItalic) androidx.compose.ui.text.font.FontFamily.Serif
            else androidx.compose.ui.text.font.FontFamily.SansSerif,
            fontStyle = if (tile.isItalic) androidx.compose.ui.text.font.FontStyle.Italic
            else androidx.compose.ui.text.font.FontStyle.Normal,
            textDecoration = if (tile.isUnderline) androidx.compose.ui.text.style.TextDecoration.Underline else null,
            letterSpacing = if (tile.isBold) (-0.8).sp else 0.sp
        ),
        color = Color(tile.textColor).copy(alpha = 1f),
        textAlign = if (isCenter) TextAlign.Center else if (tile.textAlignment == 2) TextAlign.End else TextAlign.Start,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun ColumnScope.ContentArea(
    tile: BentoEntity,
    shape: TileShape,
    isCenter: Boolean
) {
    if (tile.content.isEmpty() && !tile.isReversed) return

    val baseFontSize = if (shape.colSpan == 1) 12.sp else 14.sp
    val finalFontSize = (baseFontSize.value + (tile.contentSizeOffset * 2)).sp

    Text(
        text = tile.content,
        modifier = Modifier.weight(1f, fill = false).heightIn(min = 0.dp),
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = finalFontSize,
            lineHeight = (finalFontSize.value * 1.4).sp,
            fontWeight = if (tile.isContentBold) FontWeight.Bold else FontWeight.Normal,
            fontFamily = if (tile.isContentItalic) androidx.compose.ui.text.font.FontFamily.Serif
            else androidx.compose.ui.text.font.FontFamily.SansSerif,
            fontStyle = if (tile.isContentItalic) androidx.compose.ui.text.font.FontStyle.Italic
            else androidx.compose.ui.text.font.FontStyle.Normal,
            textDecoration = if (tile.isContentUnderline) androidx.compose.ui.text.style.TextDecoration.Underline else null,
            letterSpacing = if (tile.isContentItalic) 0.4.sp else 0.sp
        ),
        color = Color(tile.contentTextColor).copy(alpha = if (tile.isContentItalic) 0.8f else 0.9f),
        textAlign = if (isCenter) TextAlign.Center else if (tile.textAlignment == 2) TextAlign.End else TextAlign.Start,
        overflow = TextOverflow.Ellipsis
    )
}