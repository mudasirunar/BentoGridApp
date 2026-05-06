package com.example.bentoapp.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb

object BentoPalette {
    // ── PREMIUM GRADIENTS (High Contrast Pairs) ──
    val gradients = listOf(
        GradMagic, GradRoyal, GradDeepSpace, GradNorthern,
        GradMidnight, GradOcean, GradForest, GradFlame,
        GradRoseGold,  GradLemonade
    )

    // ── VIBRANT SOLIDS (Distinct from Gradient Starts) ──
    val vibrantSolids = listOf(
        TileDeepPurple, TileIndigo, TileViolet, TileBlue, TilePureGreen,
        TileEmerald, TileTeal, TileMint, TileLime, TileLavender, TileRed,
        TilePink, TileCrimson, BentoError, TileOrange, TilePeach, TileAmber,
        TileBanana, TileCoffee
    )

    // ── MONOCHROME & DEPTH ──
    val monochrome = listOf(
        Color.Black, TileObsidian, TileDeepNavy, TileCharcoal, TileSlateDark,
        TileSlate, TileIron, TileSilver, Color.White
    )

    fun isGradient(colorInt: Int): Boolean {
        return gradients.any { it.first().toArgb() == colorInt }
    }

    /**
     * Identifies if the chosen color belongs to a gradient.
     * To make this "bulletproof", every gradient start color must be unique
     * and NOT present in the Solids list.
     */
    fun getBrush(colorInt: Int?): Brush {
        val rawColor = colorInt?.let { Color(it) } ?: TileViolet
        val matchingGradient = gradients.find { it.first().toArgb() == rawColor.toArgb() }

        return if (matchingGradient != null) {
            Brush.linearGradient(
                colors = matchingGradient,
                start = androidx.compose.ui.geometry.Offset.Zero,
                end = androidx.compose.ui.geometry.Offset.Infinite
            )
        } else {
            SolidColor(rawColor)
        }
    }
}




// ─────────────────────────────────────────────
//  LIGHT MODE
// ─────────────────────────────────────────────

// Primary — rich violet
val BentoPrimary       = Color(0xFF7C3AED)   // deep violet
val BentoPrimaryContainer = Color(0xFFEDE9FE) // lavender wash

// Secondary — warm amber
val BentoSecondary     = Color(0xFFF59E0B)   // amber gold
val BentoSecondaryContainer = Color(0xFFFEF3C7) // soft amber tint

// Surface / Background
val BentoBackground    = Color(0xFFF5F3FF)   // barely-there lavender
val BentoSurface       = Color(0xFFFFFFFF)
val BentoSurfaceVariant = Color(0xFFEDE9FE)  // soft violet card bg

// On colors
val BentoOnPrimary     = Color(0xFFFFFFFF)
val BentoOnSecondary   = Color(0xFF1C1917)
val BentoOnBackground  = Color(0xFF1E1B4B)   // deep indigo text
val BentoOnSurface     = Color(0xFF1E1B4B)
val BentoOnSurfaceVariant = Color(0xFF5B4B8A)

// Error
val BentoError         = Color(0xFFDC2626)
val BentoErrorContainer = Color(0xFFFFE4E4)
val BentoOnError       = Color(0xFFFFFFFF)
val BentoOnErrorContainer = Color(0xFF991B1B)

// Outline
val BentoOutline       = Color(0xFFDDD6FE)   // violet-tinted border


// ─────────────────────────────────────────────
//  DARK MODE
// ─────────────────────────────────────────────

// Primary — electric violet
val BentoPrimaryDark   = Color(0xFFA78BFA)   // bright soft violet
val BentoPrimaryContainerDark = Color(0xFF2D1B69) // deep violet container

// Secondary — glowing amber
val BentoSecondaryDark = Color(0xFFFBBF24)   // warm gold
val BentoSecondaryContainerDark = Color(0xFF2D1F00) // dark amber

// Surface / Background — deep space
val BentoBackgroundDark  = Color(0xFF0D0B1A) // near-black with violet tint
val BentoSurfaceDark     = Color(0xFF161228) // dark violet-navy
val BentoSurfaceVariantDark = Color(0xFF1E1A35) // slightly lighter card

// On colors
val BentoOnPrimaryDark   = Color(0xFF1A0050)
val BentoOnSecondaryDark = Color(0xFF1C1917)
val BentoOnBackgroundDark = Color(0xFFEDE9FE)  // soft lavender text
val BentoOnSurfaceDark   = Color(0xFFEDE9FE)
val BentoOnSurfaceVariantDark = Color(0xFFB39DDB)

// Error — vivid coral
val BentoErrorDark         = Color(0xFFFF6B6B)
val BentoErrorContainerDark = Color(0xFF3D0F0F)
val BentoOnErrorDark        = Color(0xFFFFFFFF)
val BentoOnErrorContainerDark = Color(0xFFFF8A80)

// Outline
val BentoOutlineDark     = Color(0xFF2D2550)  // dim violet border


// ─────────────────────────────────────────────
//  TILE ACCENTS  (category colors)
// ─────────────────────────────────────────────

// ──  GRADIENT DEFINITIONS  ──

// Deep Indigo to Electric Violet (Much more "Neon")
val GradMagic    = listOf(Color(0xFF4F46E5), Color(0xFFC026D3))

// Deep Cobalt to Sky Blue (Uses your new Blue and SkyBlue)
val GradOcean = listOf(Color(0xFF2563EB), Color(0xFF2DD4BF))

// Rich Emerald to Mint (Crisp and organic)
val GradForest = listOf(Color(0xFF14532D), Color(0xFF4ADE80))

// Pure Hot Red to Gold
val GradFlame    = listOf(Color(0xFFFF0000), Color(0xFFFFCC00))

// Deep Obsidian to Slate (For that "Stealth" look)
val GradMidnight = listOf(Color(0xFF020617), Color(0xFF312E81))

// Slate Blue to Cyan
val GradDeepSpace = listOf(Color(0xFF1E293B), Color(0xFF06B6D4))

// Deep Purple to Neon Lime (The "Northern Lights" effect)
val GradNorthern  = listOf(Color(0xFF6D28D9), Color(0xFF10B981))

// Deep Pink to Rose (Very vibrant)
val GradRoseGold = listOf(Color(0xFF9D174D), Color(0xFFFDE68A))

// Pitch Black to Royal Blue
val GradRoyal     = listOf(Color(0xFF000001), Color(0xFF1D4ED8))

// Deep Orange to Yellow
val GradLemonade  = listOf(Color(0xFFEA580C), Color(0xFFFACC15))

// ──  SOLID ACCENTS  ──
val TileIndigo     = Color(0xFF3949AB)
val TileDeepPurple = Color(0xFF5E35B1)
val TileViolet     = Color(0xFF8E24AA)
val TileBlue = Color(0xFF00B2FF)
val TilePureGreen = Color(0xFF00FF00)
val TileTeal       = Color(0xFF00897B)
val TileEmerald    = Color(0xFF2E7D32)
val TileMint       = Color(0xFF26A69A)
val TileLime       = Color(0xFFC0CA33)
val TileLavender   = Color(0xFF9575CD)
val TileRed        = Color(0xFFFF3B30)
val TilePink       = Color(0xFFFF69B4)
val TileCrimson    = Color(0xFFC2185B)
val TileOrange     = Color(0xFFFB8C00)
val TilePeach      = Color(0xFFFF8A65)
val TileAmber      = Color(0xFFFFA000)
val TileBanana     = Color(0xFFFDD835)
val TileCoffee     = Color(0xFF6D4C41)

// ── MONOCHROME ──
val TileObsidian = Color(0xFF0F172A)
val TileDeepNavy = Color(0xFF1B2333)
val TileCharcoal  = Color(0xFF1C1C1E)
val TileSlateDark = Color(0xFF334155)
val TileSlate     = Color(0xFF455A64)
val TileIron      = Color(0xFF707070)
val TileSilver = Color(0xFFAEB2B5)
