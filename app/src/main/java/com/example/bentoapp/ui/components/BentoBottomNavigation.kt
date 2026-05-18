package com.example.bentoapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bentoapp.R

data class TabItem(
    val id: String, 
    val title: String, 
    val icon: ImageVector? = null, 
    val iconRes: Int? = null
)

@Composable
fun BentoBottomNavigation(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f),
                spotColor = if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.15f)
            )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val tabs = listOf(
                TabItem("collections", "Collections", iconRes = R.drawable.ic_folder),
                TabItem("gallery", "Gallery", iconRes = R.drawable.ic_gallery),
                TabItem("settings", "Settings", iconRes = R.drawable.ic_settings)
            )

            // Animating bias from -1f (left) to 1f (right) with 0f (center) for 3 tabs
            val alignmentBias by animateFloatAsState(
                targetValue = when (selectedTab) {
                    "collections" -> -1f
                    "gallery" -> 0f
                    else -> 1f
                },
                animationSpec = spring(
                    dampingRatio = 0.78f, // organic bouncy slide
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "alignmentBias"
            )

            // Dynamic squash and stretch targets for physical fluid liquid animation
            var scaleXTarget by remember { mutableStateOf(1f) }
            LaunchedEffect(selectedTab) {
                scaleXTarget = 1.25f
                kotlinx.coroutines.delay(150)
                scaleXTarget = 1f
            }

            val pillScaleX by animateFloatAsState(
                targetValue = scaleXTarget,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "pillScaleX"
            )

            val pillScaleY by animateFloatAsState(
                targetValue = if (scaleXTarget > 1f) 0.85f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "pillScaleY"
            )

            // Sliding Active Pill Capsule (rendered behind interactive Row columns)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.33f) // Exactly 1/3 for 3 tabs
                    .height(48.dp)
                    .align(BiasAlignment(horizontalBias = alignmentBias, verticalBias = 0f))
                    .padding(horizontal = 8.dp)
                    .graphicsLayer {
                        scaleX = pillScaleX
                        scaleY = pillScaleY
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                    }
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(18.dp)
                    )
            )

            // Content row (Icons & Text)
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    androidx.compose.runtime.key(tab.id) {
                        val isSelected = selectedTab == tab.id
                        val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)

                        // --- Detailed Icon Animation States (Now Isolated) ---
                        val saturation by animateFloatAsState(
                            targetValue = if (isSelected) 1f else 0f,
                            animationSpec = tween(300),
                            label = "iconSaturation"
                        )
                        
                        val iconAlpha by animateFloatAsState(
                            targetValue = if (isSelected) 1f else 0.45f,
                            animationSpec = tween(300),
                            label = "iconAlpha"
                        )

                        val colorFilter = remember(saturation) {
                            val matrix = ColorMatrix()
                            matrix.setToSaturation(saturation)
                            ColorFilter.colorMatrix(matrix)
                        }

                        // Text specific color animation
                        val textColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f) else inactiveColor,
                            animationSpec = tween(250),
                            label = "tabTextColor"
                        )

                        // Juicy Icon Bounce (Vertical translate)
                        val iconTranslationY by animateFloatAsState(
                            targetValue = if (isSelected) -4f else 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "iconTranslation"
                        )

                        // Icon Pop Scale
                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.15f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "iconScale"
                        )

                        // Text Elastic Scale
                        val textScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.04f else 0.96f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "textScale"
                        )

                        // Interactive Tab Area - ensuring full height and width coverage
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onTabSelected(tab.id) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (tab.icon != null) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        tint = if (isSelected) Color.Unspecified else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                        modifier = Modifier
                                            .size(20.dp)
                                            .graphicsLayer {
                                                scaleX = iconScale
                                                scaleY = iconScale
                                                translationY = iconTranslationY.dp.toPx()
                                            }
                                    )
                                } else if (tab.iconRes != null) {
                                    Image(
                                        painter = painterResource(id = tab.iconRes),
                                        contentDescription = tab.title,
                                        colorFilter = colorFilter,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .graphicsLayer {
                                                scaleX = iconScale
                                                scaleY = iconScale
                                                translationY = iconTranslationY.dp.toPx()
                                                alpha = iconAlpha
                                            }
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelLarge, // Increased to LabelLarge
                                    fontWeight = FontWeight.ExtraBold,
                                    color = textColor,
                                    letterSpacing = 0.1.sp,
                                    fontSize = 13.sp, // Explicitly bumped size
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = textScale
                                        scaleY = textScale
                                    }
                                )

                            }
                        }
                    }
                }
            }
        }
    }
}
