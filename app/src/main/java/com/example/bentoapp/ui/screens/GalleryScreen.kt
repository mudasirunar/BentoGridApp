package com.example.bentoapp.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.bentoapp.data.BentoEntity
import com.example.bentoapp.ui.components.BentoLightbox
import com.example.bentoapp.ui.components.SimpleTopBar
import com.example.bentoapp.viewmodel.BentoViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ─── 1. CORE UNIFIED GRID REPRESENTATIONS ──────────────────────────────────
sealed class LayoutBlock {
    abstract val keyId: String
    data class UniformRow(val items: List<BentoEntity>, val maxColumns: Int, override val keyId: String) : LayoutBlock()
    data class FeaturedSquareLeft(val bigItem: BentoEntity, val smallItems: List<BentoEntity>, val maxColumns: Int, override val keyId: String) : LayoutBlock()
    data class FeaturedSquareCenter(val bigItem: BentoEntity, val smallItems: List<BentoEntity>, val maxColumns: Int, override val keyId: String) : LayoutBlock()
    data class FeaturedSquareRight(val bigItem: BentoEntity, val smallItems: List<BentoEntity>, val maxColumns: Int, override val keyId: String) : LayoutBlock()
    data class FeaturedPortraitLeft(val bigItem: BentoEntity, val smallItems: List<BentoEntity>, val maxColumns: Int, override val keyId: String) : LayoutBlock()
    data class FeaturedPortraitCenter(val bigItem: BentoEntity, val smallItems: List<BentoEntity>, val maxColumns: Int, override val keyId: String) : LayoutBlock()
    data class FeaturedPortraitRight(val bigItem: BentoEntity, val smallItems: List<BentoEntity>, val maxColumns: Int, override val keyId: String) : LayoutBlock()
}

@Composable
fun GalleryScreen(
    viewModel: BentoViewModel,
    bottomPadding: Dp = 0.dp,
    onImageClick: (BentoEntity) -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    val galleryImages by viewModel.allGalleryImages.collectAsState()

    val sortedGalleryImages = remember(galleryImages) {
        galleryImages.sortedByDescending { it.createdAt }
    }

    var selectedViewerIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    val handleImageClick: (BentoEntity) -> Unit = { tile ->
        onImageClick(tile)
        val index = sortedGalleryImages.indexOfFirst { it.id == tile.id }
        if (index != -1) {
            selectedViewerIndex = index
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val totalColumns = if (isLandscape) 8 else 4
    val spacing = 2.dp

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            SimpleTopBar(title = "Gallery")

            if (galleryImages.isEmpty()) {
                GalleryEmptyState(bottomPadding)
            } else {
                val gallerySections = remember(sortedGalleryImages, totalColumns) {
                    val grouped = sortedGalleryImages.groupBy { formatGalleryDate(it.createdAt) }
                    grouped.map { (dateHeader, imagesForDate) ->
                        Pair(dateHeader, calculateSequentialBlocks(imagesForDate, totalColumns))
                    }
                }

                val screenWidth = configuration.screenWidthDp.dp
                val itemSize = (screenWidth - (spacing * (totalColumns - 1))) / totalColumns

                val squareFeaturedSize = (itemSize * 2) + spacing
                val portraitFeaturedWidth = (itemSize * 2) + spacing
                val portraitFeaturedHeight = (itemSize * 3) + (spacing * 2)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = bottomPadding + 16.dp)
                    ) {
                        gallerySections.forEach { (dateHeader, blocksForDate) ->
                            item(key = "header_$dateHeader") {
                                Text(
                                    text = dateHeader,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, top = 24.dp, bottom = 12.dp)
                                )
                            }

                            items(
                                items = blocksForDate,
                                key = { it.keyId }
                            ) { block ->
                                when (block) {
                                    is LayoutBlock.UniformRow -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = spacing),
                                            horizontalArrangement = Arrangement.spacedBy(spacing)
                                        ) {
                                            block.items.forEach { tile ->
                                                GalleryImageTile(tile, Modifier.size(itemSize)) { handleImageClick(tile) }
                                            }
                                            if (block.items.size < totalColumns) {
                                                repeat(totalColumns - block.items.size) {
                                                    Spacer(Modifier.size(itemSize))
                                                }
                                            }
                                        }
                                    }

                                    is LayoutBlock.FeaturedSquareLeft -> {
                                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = spacing), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                            GalleryImageTile(block.bigItem, Modifier.size(squareFeaturedSize)) { handleImageClick(block.bigItem) }
                                            Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                                                val sideColumns = totalColumns - 2
                                                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                                    block.smallItems.take(sideColumns).forEach { tile ->
                                                        GalleryImageTile(tile, Modifier.size(itemSize)) { handleImageClick(tile) }
                                                    }
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                                    block.smallItems.drop(sideColumns).take(sideColumns).forEach { tile ->
                                                        GalleryImageTile(tile, Modifier.size(itemSize)) { handleImageClick(tile) }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    is LayoutBlock.FeaturedSquareCenter -> {
                                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = spacing), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                            val side = (totalColumns - 2) / 2
                                            Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                                    block.smallItems.take(side).forEach { GalleryImageTile(it, Modifier.size(itemSize)) { handleImageClick(it) } }
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                                    block.smallItems.drop(side).take(side).forEach { GalleryImageTile(it, Modifier.size(itemSize)) { handleImageClick(it) } }
                                                }
                                            }
                                            GalleryImageTile(block.bigItem, Modifier.size(squareFeaturedSize)) { handleImageClick(block.bigItem) }
                                            Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                                    block.smallItems.drop(side * 2).take(side).forEach { GalleryImageTile(it, Modifier.size(itemSize)) { handleImageClick(it) } }
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                                    block.smallItems.drop(side * 3).take(side).forEach { GalleryImageTile(it, Modifier.size(itemSize)) { handleImageClick(it) } }
                                                }
                                            }
                                        }
                                    }

                                    is LayoutBlock.FeaturedSquareRight -> {
                                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = spacing), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                            Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                                                val sideColumns = totalColumns - 2
                                                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                                    block.smallItems.take(sideColumns).forEach { tile ->
                                                        GalleryImageTile(tile, Modifier.size(itemSize)) { handleImageClick(tile) }
                                                    }
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                                    block.smallItems.drop(sideColumns).take(sideColumns).forEach { tile ->
                                                        GalleryImageTile(tile, Modifier.size(itemSize)) { handleImageClick(tile) }
                                                    }
                                                }
                                            }
                                            GalleryImageTile(block.bigItem, Modifier.size(squareFeaturedSize)) { handleImageClick(block.bigItem) }
                                        }
                                    }

                                    is LayoutBlock.FeaturedPortraitLeft -> {
                                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = spacing), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                            GalleryImageTile(block.bigItem, Modifier.size(width = portraitFeaturedWidth, height = portraitFeaturedHeight)) { handleImageClick(block.bigItem) }
                                            Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                                                val sideColumns = totalColumns - 2
                                                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                                    block.smallItems.take(sideColumns).forEach { tile ->
                                                        GalleryImageTile(tile, Modifier.size(itemSize)) { handleImageClick(tile) }
                                                    }
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                                    block.smallItems.drop(sideColumns).take(sideColumns).forEach { tile ->
                                                        GalleryImageTile(tile, Modifier.size(itemSize)) { handleImageClick(tile) }
                                                    }
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                                    block.smallItems.drop(sideColumns * 2).take(sideColumns).forEach { tile ->
                                                        GalleryImageTile(tile, Modifier.size(itemSize)) { handleImageClick(tile) }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    is LayoutBlock.FeaturedPortraitCenter -> {
                                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = spacing), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                            val side = (totalColumns - 2) / 2
                                            Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                                    block.smallItems.take(side).forEach { GalleryImageTile(it, Modifier.size(itemSize)) { handleImageClick(it) } }
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                                    block.smallItems.drop(side).take(side).forEach { GalleryImageTile(it, Modifier.size(itemSize)) { handleImageClick(it) } }
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                                    block.smallItems.drop(side * 2).take(side).forEach { GalleryImageTile(it, Modifier.size(itemSize)) { handleImageClick(it) } }
                                                }
                                            }
                                            GalleryImageTile(block.bigItem, Modifier.size(width = portraitFeaturedWidth, height = portraitFeaturedHeight)) { handleImageClick(block.bigItem) }
                                            Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                                    block.smallItems.drop(side * 3).take(side).forEach { GalleryImageTile(it, Modifier.size(itemSize)) { handleImageClick(it) } }
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                                    block.smallItems.drop(side * 4).take(side).forEach { GalleryImageTile(it, Modifier.size(itemSize)) { handleImageClick(it) } }
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                                    block.smallItems.drop(side * 5).take(side).forEach { GalleryImageTile(it, Modifier.size(itemSize)) { handleImageClick(it) } }
                                                }
                                            }
                                        }
                                    }

                                    is LayoutBlock.FeaturedPortraitRight -> {
                                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = spacing), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                            Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                                                val sideColumns = totalColumns - 2
                                                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                                    block.smallItems.take(sideColumns).forEach { tile ->
                                                        GalleryImageTile(tile, Modifier.size(itemSize)) { handleImageClick(tile) }
                                                    }
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                                    block.smallItems.drop(sideColumns).take(sideColumns).forEach { tile ->
                                                        GalleryImageTile(tile, Modifier.size(itemSize)) { handleImageClick(tile) }
                                                    }
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                                    block.smallItems.drop(sideColumns * 2).take(sideColumns).forEach { tile ->
                                                        GalleryImageTile(tile, Modifier.size(itemSize)) { handleImageClick(tile) }
                                                    }
                                                }
                                            }
                                            GalleryImageTile(block.bigItem, Modifier.size(width = portraitFeaturedWidth, height = portraitFeaturedHeight)) { handleImageClick(block.bigItem) }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    GalleryFastScroller(
                        listState = listState,
                        bottomPadding = bottomPadding,
                        estimatedRowHeight = itemSize,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }

        // Full Screen Lightbox Overlay
        if (selectedViewerIndex != null && sortedGalleryImages.isNotEmpty()) {
            val safeIndex = selectedViewerIndex!!.coerceIn(0, sortedGalleryImages.lastIndex)

            androidx.compose.ui.window.Dialog(
                onDismissRequest = { selectedViewerIndex = null },
                properties = androidx.compose.ui.window.DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                BentoLightbox(
                    titles = emptyList(),
                    imagePaths = sortedGalleryImages.map { it.imageUri ?: "" },
                    initialIndex = safeIndex,
                    initialStatusBarVisible = true,
                    onUiVisibilityChange = {},
                    onEdit = {},
                    onDelete = {},
                    onDismiss = { selectedViewerIndex = null },
                    isGalleryMode = true
                )
            }
        }
    }
}

// ─── 2. SEQUENTIAL PACKING ALGORITHM ───────────────────────────────────────
fun calculateSequentialBlocks(images: List<BentoEntity>, columns: Int): List<LayoutBlock> {
    val blocks = mutableListOf<LayoutBlock>()
    var i = 0
    var patternCounter = 0

    while (i < images.size) {
        val itemsLeft = images.size - i
        val currentImage = images[i]

        val isPortraitFeature = currentImage.id % 3 == 0
        val isSquareFeature = currentImage.id % 2 == 0

        // Accurate mathematical target layout rules tracking
        val itemsNeededForSquare = 1 + (columns - 2) * 2
        val itemsNeededForPortrait = 1 + (columns - 2) * 3

        if (isPortraitFeature && itemsLeft >= itemsNeededForPortrait) {
            val big = currentImage
            val smalls = images.subList(i + 1, i + itemsNeededForPortrait)
            when (patternCounter % 3) {
                0 -> blocks.add(LayoutBlock.FeaturedPortraitLeft(big, smalls, columns, "fpl_${big.id}"))
                1 -> blocks.add(LayoutBlock.FeaturedPortraitCenter(big, smalls, columns, "fpc_${big.id}"))
                else -> blocks.add(LayoutBlock.FeaturedPortraitRight(big, smalls, columns, "fpr_${big.id}"))
            }
            i += itemsNeededForPortrait
            patternCounter++
        }
        else if (isSquareFeature && itemsLeft >= itemsNeededForSquare) {
            val big = currentImage
            val smalls = images.subList(i + 1, i + itemsNeededForSquare)
            when (patternCounter % 3) {
                0 -> blocks.add(LayoutBlock.FeaturedSquareLeft(big, smalls, columns, "fsl_${big.id}"))
                1 -> blocks.add(LayoutBlock.FeaturedSquareCenter(big, smalls, columns, "fsc_${big.id}"))
                else -> blocks.add(LayoutBlock.FeaturedSquareRight(big, smalls, columns, "fsr_${big.id}"))
            }
            i += itemsNeededForSquare
            patternCounter++
        }
        else {
            val sliceSize = minOf(columns, itemsLeft)
            blocks.add(
                LayoutBlock.UniformRow(
                    items = images.subList(i, i + sliceSize),
                    maxColumns = columns,
                    keyId = "uni_${images[i].id}"
                )
            )
            i += sliceSize
        }
    }
    return blocks
}

// ─── 3. FAST SCROLLER ─────────────────────────────────────────────────────
@Composable
fun GalleryFastScroller(
    listState: LazyListState,
    bottomPadding: Dp,
    estimatedRowHeight: Dp,
    modifier: Modifier = Modifier
) {
    val layoutInfo = listState.layoutInfo
    val isScrollable = layoutInfo.totalItemsCount > layoutInfo.visibleItemsInfo.size

    if (!isScrollable) return

    var isDragging by remember { mutableStateOf(false) }
    var isShowing by remember { mutableStateOf(false) }

    LaunchedEffect(listState.isScrollInProgress, isDragging) {
        if (listState.isScrollInProgress || isDragging) {
            isShowing = true
        } else {
            delay(1500)
            isShowing = false
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isShowing) 1f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "scrollbarAlpha"
    )

    val translationX by animateFloatAsState(
        targetValue = if (isShowing) 0f else 60f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scrollbarTranslation"
    )

    var trackHeightPx by remember { mutableFloatStateOf(0f) }
    val thumbHeight = 64.dp
    val thumbHeightPx = with(LocalDensity.current) { thumbHeight.toPx() }
    val maxOffset = (trackHeightPx - thumbHeightPx).coerceAtLeast(0f)

    val totalItems = layoutInfo.totalItemsCount
    val visibleItems = layoutInfo.visibleItemsInfo.size
    val maxIndex = (totalItems - visibleItems).coerceAtLeast(1)

    var dragProgress by remember { mutableFloatStateOf(0f) }

    val targetProgress = if (isDragging) {
        dragProgress
    } else {
        (listState.firstVisibleItemIndex.toFloat() / maxIndex).coerceIn(0f, 1f)
    }

    val thumbOffset by animateFloatAsState(
        targetValue = maxOffset * targetProgress,
        animationSpec = if (isDragging) tween(0) else spring(stiffness = Spring.StiffnessMediumLow),
        label = "thumbOffset"
    )

    val coroutineScope = rememberCoroutineScope()
    val fallbackHeightPx = with(LocalDensity.current) { estimatedRowHeight.toPx() }

    val thumbColor by animateColorAsState(
        targetValue = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        label = "thumbColor"
    )
    val thumbWidth by animateDpAsState(
        targetValue = if (isDragging) 12.dp else 6.dp,
        label = "thumbWidth"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(bottom = bottomPadding + 16.dp, top = 24.dp, end = 4.dp)
            .width(36.dp)
            .graphicsLayer {
                this.alpha = alpha
                this.translationX = translationX
            }
            .onGloballyPositioned { trackHeightPx = it.size.height.toFloat() }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragProgress = (offset.y / trackHeightPx).coerceIn(0f, 1f)
                        val exactIndex = dragProgress * maxIndex
                        coroutineScope.launch {
                            listState.scrollToItem(exactIndex.toInt(), 0)
                        }
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false }
                ) { change, dragAmount ->
                    change.consume()
                    if (trackHeightPx > 0) {
                        dragProgress = (change.position.y / trackHeightPx).coerceIn(0f, 1f)
                        val visibleHeight = layoutInfo.viewportSize.height.toFloat()
                        val avgHeight = if (layoutInfo.visibleItemsInfo.isNotEmpty()) {
                            layoutInfo.visibleItemsInfo.sumOf { it.size }.toFloat() / layoutInfo.visibleItemsInfo.size
                        } else {
                            fallbackHeightPx
                        }

                        val estimatedTotalHeight = layoutInfo.totalItemsCount * avgHeight
                        val listScrollablePx = (estimatedTotalHeight - visibleHeight).coerceAtLeast(1f)
                        val trackScrollablePx = (trackHeightPx - thumbHeightPx).coerceAtLeast(1f)

                        val scrollRatio = listScrollablePx / trackScrollablePx
                        listState.dispatchRawDelta(dragAmount * scrollRatio)
                    }
                }
            },
        contentAlignment = Alignment.TopEnd
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer { translationY = thumbOffset }
                .size(width = thumbWidth, height = thumbHeight)
                .clip(RoundedCornerShape(50))
                .background(thumbColor)
        )
    }
}

fun formatGalleryDate(timestamp: Long): String {
    val date = Date(timestamp)
    val calendar = Calendar.getInstance().apply { time = date }
    val currentCalendar = Calendar.getInstance()

    val isSameYear = calendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR)

    val pattern = if (isSameYear) "EEE, MMM d" else "EEE, MMM d, yyyy"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(date)
}

// ─── 4. UI COMPONENTS ─────────────────────────────────────────────────────
@Composable
fun GalleryImageTile(tile: BentoEntity, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(0.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.clip(RoundedCornerShape(0.dp)).clickable { onClick() }
    ) {
        AsyncImage(
            model = tile.imageUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun GalleryEmptyState(bottomPadding: Dp) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Box(modifier = Modifier.size(130.dp), contentAlignment = Alignment.Center) {
                Surface(shape = RoundedCornerShape(40.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f), modifier = Modifier.fillMaxSize()) {}
                Surface(shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), modifier = Modifier.size(90.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(38.dp))
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
            Text("It's empty here", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground, letterSpacing = (-0.5).sp)
            Spacer(Modifier.height(8.dp))
            Text("No images uploaded yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("All images added to your collections will be aggregated here as a centralized, chronological feed.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), textAlign = TextAlign.Center, lineHeight = 22.sp)
                }
            }
            Spacer(Modifier.height(48.dp + bottomPadding))
        }
    }
}