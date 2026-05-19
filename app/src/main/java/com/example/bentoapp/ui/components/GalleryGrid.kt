package com.example.bentoapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.bentoapp.data.BentoEntity

data class GalleryPackedTile(
    val tile: BentoEntity,
    val startCol: Int,
    val startRow: Int,
    val colSpan: Int,
    val rowSpan: Int
)

fun packGalleryTiles(images: List<BentoEntity>, columns: Int): List<GalleryPackedTile> {
    val blocks = mutableListOf<GalleryPackedTile>()
    var i = 0
    var patternCounter = 0
    var currentRow = 0

    while (i < images.size) {
        val itemsLeft = images.size - i
        val currentImage = images[i]

        val isPortraitFeature = currentImage.id % 3 == 0
        val isSquareFeature = currentImage.id % 2 == 0

        val itemsNeededForSquare = 1 + (columns - 2) * 2
        val itemsNeededForPortrait = 1 + (columns - 2) * 3

        if (isPortraitFeature && itemsLeft >= itemsNeededForPortrait) {
            val patternType = patternCounter % 3
            val bigStartCol = when (patternType) {
                0 -> 0 // Left
                1 -> (columns - 2) / 2 // Center
                else -> columns - 2 // Right
            }
            
            // Add Big Tile
            blocks.add(GalleryPackedTile(currentImage, bigStartCol, currentRow, 2, 3))
            
            // Add small tiles
            var smallRowOffset = 0
            var smallColOffset = 0
            for (j in 1 until itemsNeededForPortrait) {
                val smallTile = images[i + j]
                // Skip the 2 columns taken by the big tile
                var actualCol = smallColOffset
                if (actualCol >= bigStartCol) actualCol += 2

                blocks.add(GalleryPackedTile(smallTile, actualCol, currentRow + smallRowOffset, 1, 1))

                smallColOffset++
                if (smallColOffset >= columns - 2) {
                    smallColOffset = 0
                    smallRowOffset++
                }
            }

            i += itemsNeededForPortrait
            currentRow += 3
            patternCounter++
        }
        else if (isSquareFeature && itemsLeft >= itemsNeededForSquare) {
            val patternType = patternCounter % 3
            val bigStartCol = when (patternType) {
                0 -> 0 // Left
                1 -> (columns - 2) / 2 // Center
                else -> columns - 2 // Right
            }

            // Add Big Tile
            blocks.add(GalleryPackedTile(currentImage, bigStartCol, currentRow, 2, 2))
            
            // Add small tiles
            var smallRowOffset = 0
            var smallColOffset = 0
            for (j in 1 until itemsNeededForSquare) {
                val smallTile = images[i + j]
                // Skip the 2 columns taken by the big tile
                var actualCol = smallColOffset
                if (actualCol >= bigStartCol) actualCol += 2

                blocks.add(GalleryPackedTile(smallTile, actualCol, currentRow + smallRowOffset, 1, 1))

                smallColOffset++
                if (smallColOffset >= columns - 2) {
                    smallColOffset = 0
                    smallRowOffset++
                }
            }

            i += itemsNeededForSquare
            currentRow += 2
            patternCounter++
        }
        else {
            val sliceSize = minOf(columns, itemsLeft)
            for (j in 0 until sliceSize) {
                blocks.add(GalleryPackedTile(images[i + j], j, currentRow, 1, 1))
            }
            i += sliceSize
            currentRow += 1
        }
    }
    return blocks
}

@Composable
fun GalleryGrid(
    tiles: List<BentoEntity>,
    columns: Int,
    spacingDp: Int = 2,
    onTileClick: (BentoEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val spacing = spacingDp.dp
        val unitWidth = (maxWidth - (spacing * (columns - 1))) / columns
        
        val packedTiles = remember(tiles, columns) {
            packGalleryTiles(tiles, columns)
        }

        val totalRows = packedTiles.maxOfOrNull { it.startRow + it.rowSpan } ?: 0
        val totalHeight = if (totalRows > 0) {
            (unitWidth * totalRows) + (spacing * (totalRows - 1))
        } else 0.dp

        Box(modifier = Modifier.fillMaxWidth().height(totalHeight)) {
            packedTiles.forEach { pt ->
                val width = (unitWidth * pt.colSpan) + (spacing * (pt.colSpan - 1))
                val height = (unitWidth * pt.rowSpan) + (spacing * (pt.rowSpan - 1))
                val xOffset = (unitWidth * pt.startCol) + (spacing * pt.startCol)
                val yOffset = (unitWidth * pt.startRow) + (spacing * pt.startRow)

                GalleryImageTileFlat(
                    tile = pt.tile,
                    modifier = Modifier
                        .offset(x = xOffset, y = yOffset)
                        .size(width = width, height = height),
                    onClick = onTileClick
                )
            }
        }
    }
}

@Composable
fun GalleryImageTileFlat(tile: BentoEntity, modifier: Modifier, onClick: (BentoEntity) -> Unit) {
    val context = LocalContext.current
    val imageRequest = remember(tile.imageUri) {
        ImageRequest.Builder(context)
            .data(tile.imageUri)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .size(400)
            .allowHardware(true)
            .build()
    }
    
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.clip(RoundedCornerShape(0.dp)).clickable { onClick(tile) }
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
