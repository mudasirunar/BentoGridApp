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
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
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
import com.example.bentoapp.ui.components.SimpleTopBar
import kotlinx.coroutines.launch

import androidx.compose.material.icons.filled.ChevronRight
import com.example.bentoapp.data.ProjectEntity

@Composable
fun SettingsScreen(
    currentThemeMode: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    projects: List<ProjectEntity> = emptyList(),
    onOpenManageLocksDialog: () -> Unit = {},
    onUnlockAllCollections: () -> Unit = {},
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    scrollState: ScrollState = rememberScrollState(),
    modifier: Modifier = Modifier
) {
    val lockedCount = remember(projects) { projects.count { it.isLocked } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SimpleTopBar(title = "Settings")

        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState)
                .weight(1f)
        ) {
            Spacer(Modifier.height(12.dp))
            
            // ── Theme Section ──
            ThemeSettingsSection(
                currentThemeMode = currentThemeMode,
                onThemeSelected = onThemeSelected
            )

            Spacer(Modifier.height(32.dp))

            // ── Security & Privacy Section ──
            BiometricSettingsSection(
                lockedCount = lockedCount,
                onToggle = { targetOn ->
                    if (targetOn) {
                        onOpenManageLocksDialog()
                    } else {
                        onUnlockAllCollections()
                    }
                },
                onCardClick = onOpenManageLocksDialog
            )

            Spacer(Modifier.height(48.dp + bottomPadding))
        }
    }
}

@Composable
private fun BiometricSettingsSection(
    lockedCount: Int,
    onToggle: (Boolean) -> Unit,
    onCardClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val isEnabled = lockedCount > 0

    val trackColor = if (isDark) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        // Header
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = "Security & Privacy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Protect your collections with biometric security.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        }

        // Main Toggle Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = trackColor,
                    shape = RoundedCornerShape(22.dp)
                )
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Biometric Protection",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (isEnabled) "$lockedCount ${if (lockedCount == 1) "collection" else "collections"} protected" else "Tap switch to select collections to lock",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }

            Spacer(Modifier.width(12.dp))

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }

        // ── Security Dashboard Summary Card (Shown when >0 collections are locked) ──
        if (lockedCount > 0) {
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onCardClick() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$lockedCount ${if (lockedCount == 1) "Collection" else "Collections"} Locked",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Tap to manage or lock/unlock collections",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
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
                text = "Choose your preferred appearance.",
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
