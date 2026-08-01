package com.example.bentoapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.example.bentoapp.data.BentoEntity
import com.example.bentoapp.data.ProjectEntity
import android.graphics.BitmapFactory
import java.io.File
import kotlin.math.roundToInt

@Composable
fun ProjectCard(
    project: ProjectEntity,
    counts: com.example.bentoapp.data.ProjectCounts?,
    projectTiles: List<BentoEntity> = emptyList(),
    isCollapsed: Boolean = false,
    onClick: () -> Unit,
    onDeleteRequest: () -> Unit,
    onEditRequest: () -> Unit,
    onToggleLockRequest: (() -> Unit)? = null,
    onToggleExpand: ((Boolean) -> Unit)? = null,
    onTileClick: ((tile: BentoEntity, collectionTiles: List<BentoEntity>) -> Unit)? = null,
    onHaptic: (type: String) -> Unit
) {
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val maxDrag = -220f
    val triggerThreshold = -160f
    var hasVibrated by remember { mutableStateOf(false) }
    var isLongPress by remember { mutableStateOf(false) }
    var lastClickTime by remember { androidx.compose.runtime.mutableLongStateOf(0L) }

    val imageTiles = remember(projectTiles) {
        projectTiles.filter { !it.imageUri.isNullOrEmpty() }
    }
    val hasImages = imageTiles.isNotEmpty() || (counts?.images ?: 0) > 0
    val isChevronEnabled = !project.isLocked && hasImages

    var isExpanded by remember(project.id, isChevronEnabled, isCollapsed) {
        mutableStateOf(isChevronEnabled && !isCollapsed)
    }

    LaunchedEffect(isChevronEnabled, isCollapsed) {
        if (!isChevronEnabled) {
            isExpanded = false
        } else {
            isExpanded = !isCollapsed
        }
    }

    val animatedCardHeight by animateDpAsState(
        targetValue = if (isExpanded && isChevronEnabled) 240.dp else 104.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "animatedCardHeight"
    )

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded && isChevronEnabled) 90f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chevronRotation"
    )

    val animatedOffset by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "swipeOffset"
    )

    val progress = (-animatedOffset / -maxDrag).coerceIn(0f, 1f)
    val revealAlpha = ((progress - 0.15f) / 0.85f).coerceIn(0f, 1f)
    val iconScale by animateFloatAsState(
        targetValue = if (progress > 0.4f) 1.2f else 0.5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "iconScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(animatedCardHeight)
    ) {
        // Delete background
        if (progress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = revealAlpha * 0.9f)
                    ),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(end = 28.dp)
                        .size(24.dp)
                        .graphicsLayer(
                            alpha = revealAlpha,
                            scaleX = iconScale,
                            scaleY = iconScale
                        )
                )
            }
        }

        // Card Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 0.5.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (dragOffset < triggerThreshold) onDeleteRequest()
                                    dragOffset = 0f
                                    hasVibrated = false
                                },
                                onDragCancel = {
                                    dragOffset = 0f
                                    hasVibrated = false
                                },
                                onHorizontalDrag = { _, delta ->
                                    dragOffset = (dragOffset + delta).coerceIn(maxDrag, 0f)
                                    if (dragOffset <= triggerThreshold && !hasVibrated) {
                                        onHaptic("TRIGGER")
                                        hasVibrated = true
                                    } else if (dragOffset > triggerThreshold) {
                                        hasVibrated = false
                                    }
                                }
                            )
                        }
                        .combinedClickable(
                            onClick = {
                                if (!isLongPress) {
                                    val now = System.currentTimeMillis()
                                    if (now - lastClickTime > 500) {
                                        lastClickTime = now
                                        onClick()
                                    }
                                }
                                isLongPress = false
                            },
                            onLongClick = {
                                isLongPress = true
                                onHaptic("LONG_PRESS")
                                onEditRequest()
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .padding(10.dp)
                            .size(84.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onEditRequest() }
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    )
                                )
                            ),
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
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        if (project.isLocked) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = "Locked",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Text Info
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = project.name,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(Modifier.height(4.dp))
                        if (project.isLocked) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "•••••••• • •••••••",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.blur(6.dp)
                                )
                            }
                        } else {
                            val tilesCount = counts?.tiles ?: 0
                            val imagesCount = counts?.images ?: 0
                            val countText = if (tilesCount == 0) "Empty collection"
                            else {
                                val tStr = if (tilesCount == 1) "1 tile" else "$tilesCount tiles"
                                if (imagesCount == 0) tStr
                                else {
                                    val iStr = if (imagesCount == 1) "1 image" else "$imagesCount images"
                                    "$tStr • $iStr"
                                }
                            }

                            Text(
                                text = countText,
                                modifier = Modifier.padding(start = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Lock Toggle Action Button
                    if (onToggleLockRequest != null) {
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (project.isLocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                )
                                .clickable { onToggleLockRequest() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (project.isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                contentDescription = if (project.isLocked) "Unlock Collection" else "Lock Collection",
                                tint = if (project.isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Repurposed Chevron Button (Expand Quick Preview)
                    Box(
                        modifier = Modifier
                            .padding(end = 18.dp)
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isChevronEnabled) {
                                    if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)
                                }
                            )
                            .clickable(enabled = isChevronEnabled) {
                                onHaptic("TICK")
                                val newExpanded = !isExpanded
                                isExpanded = newExpanded
                                onToggleExpand?.invoke(!newExpanded)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = if (isExpanded) "Collapse preview" else "Expand preview",
                            modifier = Modifier
                                .size(18.dp)
                                .graphicsLayer { rotationZ = chevronRotation },
                            tint = if (isChevronEnabled) {
                                if (isExpanded) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                            }
                        )
                    }
                }

                // Expanded Quick Image Preview Reel Carousel
                if (isExpanded && isChevronEnabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(132.dp)
                            .padding(bottom = 8.dp)
                    ) {
                        LazyRow(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(imageTiles, key = { it.id }) { tile ->
                                val aspectRatio = remember(tile.imageUri) {
                                    if (!tile.imageUri.isNullOrEmpty()) {
                                        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                        BitmapFactory.decodeFile(tile.imageUri, options)
                                        if (options.outHeight > 0 && options.outWidth > 0) {
                                            (options.outWidth.toFloat() / options.outHeight.toFloat()).coerceIn(0.5f, 2.5f)
                                        } else 1f
                                    } else 1f
                                }

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                    modifier = Modifier
                                        .height(118.dp)
                                        .aspectRatio(aspectRatio, matchHeightConstraintsFirst = true)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable {
                                            onHaptic("TICK")
                                            onTileClick?.invoke(tile, imageTiles)
                                        }
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(tile.imageUri?.let { File(it) })
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}