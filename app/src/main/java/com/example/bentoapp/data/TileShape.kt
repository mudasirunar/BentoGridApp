package com.example.bentoapp.data

/**
 * 5 fixed tile shapes on a 4-COLUMN grid.
 *
 * Using 4 columns lets us express half-width tiles properly:
 *   SQUARE   = 2 of 4 cols  →  normal tile
 *   WIDE     = 4 of 4 cols  →  full row, same height as square
 *   TALL     = 2 of 4 cols  →  same width as square, double height
 *   SMALL_H  = 2 of 4 cols  →  same width as square, half height
 *   SMALL_V  = 1 of 4 cols  →  HALF width of square, same height as square
 *
 *  Visual on the 4-col grid:
 *
 *  col:  1    2    3    4
 *       ┌─────────┐               SQUARE   (span 2, h=160)
 *       └─────────┘
 *       ┌───────────────────┐     WIDE     (span 4, h=160)
 *       └───────────────────┘
 *       ┌─────────┐               TALL     (span 2, h=320)
 *       │         │
 *       └─────────┘
 *       ┌─────────┐               SMALL_H  (span 2, h=80)
 *       └─────────┘
 *       ┌────┐                    SMALL_V  (span 1, h=160) ← half width of SQUARE
 *       │    │
 *       └────┘
 */
enum class TileShape(
    val index: Int,
    val label: String,
    val colSpan: Int,            // out of 4 columns
    val heightDp: Int,
    val previewWidthDp: Int,
    val previewHeightDp: Int
) {
    SQUARE(
        index = 0,
        label = "Square",
        colSpan = 2,
        heightDp = 160,
        previewWidthDp = 70,
        previewHeightDp = 70
    ),

    WIDE(
        index = 1,
        label = "Wide",
        colSpan = 4,
        heightDp = 160,
        previewWidthDp = 100,
        previewHeightDp = 70
    ),

    TALL(
        index = 2,
        label = "Tall",
        colSpan = 2,
        heightDp = 320,
        previewWidthDp = 70,
        previewHeightDp = 100
    ),

    SMALL_H(
        index = 3,
        label = "Small Wide",
        colSpan = 2,
        heightDp = 80,
        previewWidthDp = 80,
        previewHeightDp = 38
    ),

    // Half the column width of SQUARE — genuinely narrow
    SMALL_V(
        index = 4,
        label = "Small Tall",
        colSpan = 1,             // 1 of 4 cols = half of SQUARE's 2 cols
        heightDp = 160,          // same height as SQUARE so ratio reads as portrait
        previewWidthDp = 40,
        previewHeightDp = 80
    );

    companion object {
        fun fromIndex(index: Int) = entries.firstOrNull { it.index == index } ?: SQUARE
    }
}