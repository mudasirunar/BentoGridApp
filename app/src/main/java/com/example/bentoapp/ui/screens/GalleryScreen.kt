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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bentoapp.data.BentoEntity
import com.example.bentoapp.ui.components.BentoLightbox
import com.example.bentoapp.ui.components.GalleryGrid
import com.example.bentoapp.ui.components.GalleryShimmer
import com.example.bentoapp.ui.components.SimpleTopBar
import com.example.bentoapp.ui.components.packGalleryTiles
import com.example.bentoapp.viewmodel.BentoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ─── GALLERY SCREEN INITIALIZATION FLAG ───
private var hasInitialGalleryRenderCompleted = false

@Composable
fun GalleryScreen(
    viewModel: BentoViewModel,
    bottomPadding: Dp = 0.dp,
    onImageClick: (BentoEntity) -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val totalColumns = if (isLandscape) 8 else 4

    val galleryImages by viewModel.allGalleryImages.collectAsState()

    val sortedGalleryImages = remember(galleryImages) {
        galleryImages.sortedByDescending { it.createdAt }
    }

    var selectedViewerIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var isReadyToRender by remember { mutableStateOf(hasInitialGalleryRenderCompleted) }

    val gallerySections = remember(sortedGalleryImages, totalColumns, isReadyToRender) {
        if (!isReadyToRender) return@remember emptyList()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        sortedGalleryImages.groupBy { formatGalleryDate(it.createdAt, currentYear) }.toList()
    }

    LaunchedEffect(Unit) {
        if (!hasInitialGalleryRenderCompleted) {
            delay(300) // Defer heavy layout inflation until tab slide animation completes
            hasInitialGalleryRenderCompleted = true
            isReadyToRender = true
        }
    }

    val handleImageClick: (BentoEntity) -> Unit = remember(sortedGalleryImages, onImageClick) {
        { tile ->
            onImageClick(tile)
            val index = sortedGalleryImages.indexOfFirst { it.id == tile.id }
            if (index != -1) {
                selectedViewerIndex = index
            }
        }
    }

    // 🔥 THE FIX: Calculate the EXACT pixel height of every single section upfront.
    val density = LocalDensity.current
    val spacingPx = with(density) { 2.dp.toPx() }
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val unitWidthPx = (screenWidthPx - (spacingPx * (totalColumns - 1))) / totalColumns

    // We enforce an exact 64dp header height so the math is 100% flawless
    val headerHeightPx = with(density) { 64.dp.toPx() }
    val bottomPaddingPx = with(density) { (bottomPadding + 16.dp).toPx() }

    val sectionHeightsPx = remember(gallerySections, configuration, isReadyToRender) {
        if (!isReadyToRender || gallerySections.isEmpty()) return@remember emptyList<Float>()
        gallerySections.map { (_, images) ->
            val packed = packGalleryTiles(images, totalColumns)
            val rows = packed.maxOfOrNull { it.startRow + it.rowSpan } ?: 0
            val gridHeight = if (rows > 0) (unitWidthPx * rows) + (spacingPx * (rows - 1)) else 0f
            headerHeightPx + gridHeight
        }
    }

    val totalListHeightPx = remember(sectionHeightsPx, isReadyToRender) {
        if (!isReadyToRender || sectionHeightsPx.isEmpty()) 0f
        else sectionHeightsPx.sum() + bottomPaddingPx
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            SimpleTopBar(title = "Gallery")

            if (!isReadyToRender) {
                GalleryShimmer(
                    columns = totalColumns,
                    modifier = Modifier.fillMaxSize().weight(1f)
                )
            } else if (galleryImages.isEmpty()) {
                GalleryEmptyState(bottomPadding)
            } else {
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
                        // By merging Header and Grid into one item, the indices map 1:1 perfectly with our heights
                        itemsIndexed(
                            items = gallerySections,
                            key = { _, item -> item.first },
                            contentType = { _, _ -> "DateSection" }
                        ) { index, (dateHeader, imagesForDate) ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Locked 64dp header to match our math perfectly
                                Box(
                                    modifier = Modifier.height(64.dp).fillMaxWidth().padding(start = 16.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = dateHeader,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }

                                GalleryGrid(
                                    tiles = imagesForDate,
                                    columns = totalColumns,
                                    spacingDp = 2,
                                    onTileClick = handleImageClick,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    GalleryScrollbar(
                        listState = listState,
                        sectionHeightsPx = sectionHeightsPx,
                        totalListHeightPx = totalListHeightPx,
                        bottomPadding = bottomPadding,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }

        if (selectedViewerIndex != null && sortedGalleryImages.isNotEmpty()) {
            val safeIndex = selectedViewerIndex!!.coerceIn(0, sortedGalleryImages.lastIndex)
            // 🚀 SNAPPY OPENING FIX: Wrap in a custom Dialog style to bypass default platform fade animations
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

// ─── DATE FORMATTER ───
private val sameYearFormat = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue() = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
}
private val diffYearFormat = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue() = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
}
private val calendarThreadLocal = object : ThreadLocal<Calendar>() {
    override fun initialValue() = Calendar.getInstance()
}

fun formatGalleryDate(timestamp: Long, currentYear: Int): String {
    val date = Date(timestamp)
    val calendar = calendarThreadLocal.get()!!.apply { time = date }
    val isSameYear = calendar.get(Calendar.YEAR) == currentYear
    return if (isSameYear) sameYearFormat.get()!!.format(date) else diffYearFormat.get()!!.format(date)
}

@Composable
fun GalleryScrollbar(
    listState: LazyListState,
    sectionHeightsPx: List<Float>,
    totalListHeightPx: Float,
    bottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var isShowing by remember { mutableStateOf(false) }

    val isScrollInProgress by remember { derivedStateOf { listState.isScrollInProgress } }

    LaunchedEffect(isScrollInProgress, isDragging) {
        if (isScrollInProgress || isDragging) {
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

    // 🔥 READ PROGRESS: Flawless mapping of scroll position to thumb position
    val scrollProgress by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val firstVisible = layoutInfo.visibleItemsInfo.firstOrNull() ?: return@derivedStateOf 0f

            var scrolledY = 0f
            // Add up the heights of everything above us
            for (i in 0 until firstVisible.index) {
                scrolledY += sectionHeightsPx.getOrElse(i) { 0f }
            }
            // Subtract offset (offset is negative as item scrolls up)
            scrolledY -= firstVisible.offset

            val viewportHeight = layoutInfo.viewportSize.height.toFloat()
            val maxScrollY = (totalListHeightPx - viewportHeight).coerceAtLeast(1f)

            (scrolledY / maxScrollY).coerceIn(0f, 1f)
        }
    }

    var dragProgress by remember { mutableFloatStateOf(0f) }
    val currentProgress = if (isDragging) dragProgress else scrollProgress

    val maxTrack = (trackHeightPx - thumbHeightPx).coerceAtLeast(0f)
    val thumbOffset by animateFloatAsState(
        targetValue = maxTrack * currentProgress,
        animationSpec = if (isDragging) tween(0) else spring(stiffness = Spring.StiffnessMediumLow),
        label = "thumbOffset"
    )

    val thumbColor by animateColorAsState(
        targetValue = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        label = "thumbColor"
    )
    val thumbWidth by animateDpAsState(
        targetValue = if (isDragging) 12.dp else 6.dp,
        label = "thumbWidth"
    )

    val performAbsoluteScroll: (Float) -> Unit = { progress ->
        val viewportHeight = listState.layoutInfo.viewportSize.height.toFloat()
        val maxScrollY = (totalListHeightPx - viewportHeight).coerceAtLeast(1f)
        val targetPixelY = progress * maxScrollY

        var accumulatedY = 0f
        var targetIndex = 0
        var targetOffset = 0f

        for (i in sectionHeightsPx.indices) {
            if (targetPixelY <= accumulatedY + sectionHeightsPx[i]) {
                targetIndex = i
                targetOffset = targetPixelY - accumulatedY
                break
            }
            accumulatedY += sectionHeightsPx[i]
            targetIndex = i
            targetOffset = targetPixelY - accumulatedY
        }

        coroutineScope.launch {
            // Jump exactly to the pixel depth! Bypasses all layout measuring walls.
            listState.scrollToItem(targetIndex, targetOffset.toInt())
        }
    }

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
                        val touchY = offset.y - (thumbHeightPx / 2f)
                        dragProgress = (touchY / maxTrack).coerceIn(0f, 1f)
                        performAbsoluteScroll(dragProgress)
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false }
                ) { change, dragAmount ->
                    change.consume()
                    dragProgress = (dragProgress + (dragAmount / maxTrack)).coerceIn(0f, 1f)
                    performAbsoluteScroll(dragProgress)
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

@Composable
fun GalleryEmptyState(bottomPadding: Dp) {
    // Same as before
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
