package com.example.bentoapp.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.bentoapp.data.ProjectCounts
import com.example.bentoapp.data.ProjectEntity
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageLocksDialog(
    projects: List<ProjectEntity>,
    projectCounts: Map<Int, ProjectCounts>,
    onDismiss: () -> Unit,
    onConfirmLocks: (lockedIds: Set<Int>) -> Unit,
    triggerHaptic: (String) -> Unit
) {
    val initialLockedIds = remember(projects) {
        projects.filter { it.isLocked }.map { it.id }.toSet()
    }

    var selectedLockedIds by remember { mutableStateOf(initialLockedIds) }
    val isAllSelected = projects.isNotEmpty() && selectedLockedIds.size == projects.size

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    var visible by remember { mutableStateOf(false) }
    val dialogScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "dialogScale"
    )
    LaunchedEffect(Unit) { visible = true }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = screenHeight * 0.82f)
                    .scale(dialogScale)
                    .clickable(enabled = false) {}, // Prevent dismiss on card tap
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // ── Header Row ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Collection Locks",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${selectedLockedIds.size} of ${projects.size} locked",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Select All / Deselect All Action Row ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.SelectAll,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isAllSelected) "Deselect All" else "Select All",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Checkbox(
                            checked = isAllSelected,
                            onCheckedChange = { check ->
                                triggerHaptic("TICK")
                                selectedLockedIds = if (check) {
                                    projects.map { it.id }.toSet()
                                } else {
                                    emptySet()
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF7C3AED),
                                checkmarkColor = Color.White,
                                uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                            )
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // ── Collections List ──
                    if (projects.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No collections created yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(projects, key = { it.id }) { project ->
                                val isLocked = project.id in selectedLockedIds

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isLocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                                        )
                                        .border(
                                            width = 0.5.dp,
                                            color = if (isLocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                            else Color.Transparent,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            triggerHaptic("TICK")
                                            selectedLockedIds = if (isLocked) {
                                                selectedLockedIds - project.id
                                            } else {
                                                selectedLockedIds + project.id
                                            }
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Thumbnail
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (project.imageUrl.isNotEmpty()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(File(project.imageUrl))
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    // Name & Subtitle
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = project.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val counts = projectCounts[project.id]
                                        val tiles = counts?.tiles ?: 0
                                        Text(
                                            text = if (tiles == 0) "Empty collection" else "$tiles tiles",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    // Switch
                                    Switch(
                                        checked = isLocked,
                                        onCheckedChange = { check ->
                                            triggerHaptic("TICK")
                                            selectedLockedIds = if (check) {
                                                selectedLockedIds + project.id
                                            } else {
                                                selectedLockedIds - project.id
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF7C3AED),
                                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                            uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                            uncheckedBorderColor = Color.Transparent
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Confirm / Save Button ──
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF7C3AED),
                                        Color(0xFF9333EA)
                                    )
                                )
                            )
                            .clickable {
                                triggerHaptic("CONFIRM")
                                onConfirmLocks(selectedLockedIds)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Save Lock Settings",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
