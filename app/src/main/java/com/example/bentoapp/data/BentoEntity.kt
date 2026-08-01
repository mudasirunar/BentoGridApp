package com.example.bentoapp.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val imageUrl: String = "",
    val isBackground: Boolean = false,
    val shapeIndex: Int = 1,
    val isLocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bento_tiles")
data class BentoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val title: String = "",
    val content: String = "",
    val imageUri: String? = null,
    val shapeIndex: Int = 0,
    val backgroundColor: Int? = null,
    val textAlignment: Int = 0,
    val textColor: Int = Color.White.toArgb(),
    // ── Title styling ──
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val textSizeOffset: Int = 0,
    val isReversed: Boolean = false,
    // ── Content styling ──
    val contentTextColor: Int = Color.White.toArgb(),
    val isContentBold: Boolean = false,
    val isContentItalic: Boolean = false,
    val isContentUnderline: Boolean = false,
    val contentSizeOffset: Int = 0,
    val originalImageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val shape: TileShape get() = TileShape.fromIndex(shapeIndex)
}