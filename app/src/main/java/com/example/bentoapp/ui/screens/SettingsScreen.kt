package com.example.bentoapp.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bentoapp.utils.ThemeMode
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    currentThemeMode: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.Start, // Align to left
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 110.dp) // Below TopBar
                .verticalScroll(rememberScrollState())
        ) {
            // ── Theme Section ──
            ThemeSettingsSection(
                currentThemeMode = currentThemeMode,
                onThemeSelected = onThemeSelected
            )

            Spacer(Modifier.height(48.dp + bottomPadding))
        }
    }
}

@Composable
private fun ThemeSettingsSection(
    currentThemeMode: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    
    // Dynamic colors for the toggle components based on current theme luminance
    val trackColor = if (isDark) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f) // More visible track in dark mode
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    }
    
    val pillColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant // Lighter pill for dark mode
    } else {
        MaterialTheme.colorScheme.surface // Standard white pill for light mode
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start // Start from left
    ) {
        // Header with mature branding
        Column(modifier = Modifier.padding(bottom = 20.dp)) {
            Text(
                text = "App Appearance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Refine the app's visual atmosphere",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        }

        // Modern Sliding Toggle Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(
                    color = trackColor,
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(6.dp)
        ) {
            val tabs = listOf(
                ThemeTabItem(ThemeMode.SYSTEM, "System", Icons.Rounded.BrightnessAuto, Color(0xFF8B5CF6)),
                ThemeTabItem(ThemeMode.LIGHT, "Light", Icons.Rounded.LightMode, Color(0xFFF59E0B)),
                ThemeTabItem(ThemeMode.DARK, "Dark", Icons.Rounded.DarkMode, Color(0xFF38BDF8))
            )

            val alignmentBias by animateFloatAsState(
                targetValue = when (currentThemeMode) {
                    ThemeMode.SYSTEM -> -1f
                    ThemeMode.LIGHT -> 0f
                    ThemeMode.DARK -> 1f
                },
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "themeBias"
            )

            // Premium Floating Active Indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.333f)
                    .fillMaxHeight()
                    .align(BiasAlignment(horizontalBias = alignmentBias, verticalBias = 0f))
                    .shadow(
                        elevation = 6.dp, 
                        shape = RoundedCornerShape(18.dp),
                        ambientColor = Color.Black.copy(alpha = 0.2f),
                        spotColor = Color.Black.copy(alpha = 0.3f)
                    )
                    .background(
                        color = pillColor,
                        shape = RoundedCornerShape(18.dp)
                    )
            )

            // Interactive Content
            Row(modifier = Modifier.fillMaxSize()) {
                tabs.forEach { tab ->
                    val isSelected = currentThemeMode == tab.mode
                    
                    // Icon Color Animation
                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected) tab.color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        animationSpec = tween(300)
                    )
                    
                    // Text Color Animation
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface 
                                      else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        animationSpec = tween(300)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onThemeSelected(tab.mode) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = iconColor,
                                modifier = Modifier.size(if (isSelected) 22.dp else 20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = textColor,
                                letterSpacing = 0.2.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class ThemeTabItem(
    val mode: ThemeMode,
    val label: String,
    val icon: ImageVector,
    val color: Color
)
