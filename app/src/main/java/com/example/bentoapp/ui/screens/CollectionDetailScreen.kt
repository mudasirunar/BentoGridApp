package com.example.bentoapp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.bentoapp.data.BentoEntity
import com.example.bentoapp.data.ProjectEntity
import com.example.bentoapp.ui.components.BentoEmptyAnimation
import com.example.bentoapp.ui.components.BentoFab
import com.example.bentoapp.ui.components.BentoGrid
import com.example.bentoapp.ui.components.BentoLightbox
import com.example.bentoapp.ui.components.BentoLoadingView
import com.example.bentoapp.ui.theme.BentoSelectGreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailScreen(
    project: ProjectEntity,
    projectName: String,
    tiles: List<BentoEntity>?,
    onBackClick: () -> Unit,
    onAddTileClick: (Int) -> Unit,
    onEditTileClick: (Int, Int) -> Unit,
    onDeleteTileImmediate: (BentoEntity) -> Unit,
    onUndoDeleteTile: (BentoEntity) -> Unit,
    onDeleteTileConfirm: (BentoEntity) -> Unit,
    navController: NavController
) {
    var isFirstTimeEntry by rememberSaveable { mutableStateOf(true) }
    var fabsVisible by rememberSaveable { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var snackbarJob by remember { mutableStateOf<Job?>(null) }

    // STATE FOR LIGHTBOX
    var selectedLightboxIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var reopenViewerForTileId by rememberSaveable { mutableStateOf<Int?>(null) }
    var reopenViewerFallbackIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var editedFromViewer by rememberSaveable { mutableStateOf(false) }
    var isViewerUiActive by remember { mutableStateOf(false) }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val snackbarBottomPadding by animateDpAsState(
        targetValue = when {
            // If Viewer is open, follow the viewer's UI state
            selectedLightboxIndex != null -> {
                if (isLandscape) {
                    // Adjust these for Landscape Viewer
                    if (isViewerUiActive) 32.dp else 24.dp
                } else {
                    // Adjust these for Portrait Viewer (existing values)
                    if (isViewerUiActive) 100.dp else 36.dp
                }
            }
            // If Viewer is closed, follow the FABs
            fabsVisible -> 76.dp
            else -> 24.dp
        },
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "snackbarPadding"
    )

    // --- HAPTIC ENGINE ---
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val vibrator = remember {
        context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
    }

    val triggerHaptic = { effectType: String ->
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            when (effectType) {
                "CONFIRM" -> vibrator.vibrate(android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_CLICK))
                "TICK" -> haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            }
        } else {
            vibrator.vibrate(15)
        }
    }

    // ── Status bar controller ─────────────────────────────────────────────
    val view = LocalView.current
    val window = remember(view) {
        var context = view.context
        while (context is android.content.ContextWrapper) {
            if (context is android.app.Activity) {
                return@remember context.window
            }
            context = context.baseContext
        }
        null
    }

    // --- SMART ACTION OBSERVER  ---
    val navBackStackEntry = navController.currentBackStackEntry
    val tileAction by navBackStackEntry?.savedStateHandle
        ?.getStateFlow<String?>("tile_action", null)
        ?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(tileAction) {
        tileAction?.let { action ->
            when (action) {
                "created" -> {
                    snackbarHostState.showSnackbar(
                        message = "New tile created",
                        duration = SnackbarDuration.Short
                    )
                }
                "updated" -> {
                    snackbarHostState.showSnackbar(
                        message = "Tile settings updated",
                        duration = SnackbarDuration.Short
                    )
                }
            }
            // CRITICAL: Reset the signal immediately so it doesn't fire again on rotation
            navBackStackEntry?.savedStateHandle?.set("tile_action", null)
        }
    }

    val navigateAway: (() -> Unit) -> Unit = { action ->
        if (window != null) {
            WindowInsetsControllerCompat(window, view).show(WindowInsetsCompat.Type.statusBars())
        }
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarJob?.cancel()
        action()
    }

    var tileOptionsTarget by remember { mutableStateOf<BentoEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val pendingDeletions = remember { mutableStateMapOf<Int, BentoEntity>() }
    var activeDeletionJob by remember { mutableStateOf<Job?>(null) }

    var isSelectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedTileIds by rememberSaveable { mutableStateOf(emptySet<Int>()) }
    var showMultiDeleteDialog by remember { mutableStateOf(false) }


    // FILTER TILES WITH IMAGES
    val imageTiles = remember(tiles, pendingDeletions.size) {
        tiles?.filter {
            !it.imageUri.isNullOrEmpty() && it.id !in pendingDeletions.keys
        } ?: emptyList()
    }

    val imagePaths = remember(imageTiles) {
        imageTiles.map { it.imageUri!! }
    }

    LaunchedEffect(imageTiles, tiles) {
        if (tiles != null && imageTiles.isEmpty() && selectedLightboxIndex != null && !editedFromViewer) {
            selectedLightboxIndex = null
        }
    }

    // Reopen lightbox if we edited a tile FROM THE VIEWER
    LaunchedEffect(imageTiles, reopenViewerForTileId) {
        if (reopenViewerForTileId != null && editedFromViewer) {
            val idx = imageTiles.indexOfFirst { it.id == reopenViewerForTileId }
            if (idx != -1) {
                // Same tile still has an image — reopen on it
                selectedLightboxIndex = idx
            } else if (imageTiles.isNotEmpty()) {
                // Image was removed but others exist — show nearest
                val fallback = reopenViewerFallbackIndex ?: 0
                selectedLightboxIndex = fallback.coerceIn(0, imageTiles.lastIndex)
            } else {
                // No images left — stay on collection screen, don't open viewer
                selectedLightboxIndex = null
            }
            reopenViewerForTileId = null
            reopenViewerFallbackIndex = null
            editedFromViewer = false
        }
    }


    // --- 2. CLEANUP ON DISPOSE ---
    // If user leaves the screen while a snackbar is showing, finalize the delete!
    DisposableEffect(Unit) {
        onDispose {
            activeDeletionJob?.cancel()
            // Finalize: clean up the physical image files on disk
            pendingDeletions.forEach { (_, tile) -> onDeleteTileConfirm(tile) }
            pendingDeletions.clear()
        }
    }

    var reopenTileId   by remember { mutableStateOf<Int?>(null) }
    var reopenRequest  by remember { mutableStateOf(0) }

    // ADD — runs after recomposition so imageTiles is always fresh
    LaunchedEffect(reopenRequest, imageTiles) {
        val id = reopenTileId ?: return@LaunchedEffect
        val idx = imageTiles.indexOfFirst { it.id == id }
        if (idx != -1) {
            selectedLightboxIndex = idx
            reopenTileId = null
        }
    }

    // ---  DELETE LOGIC ---
    val handleTileDeletion: (BentoEntity, Int?) -> Unit = { tile, reopenTileIdParam ->
        activeDeletionJob?.cancel()
        
        // 1. Immediately delete from DB so abrupt process closure does not resurrect it
        onDeleteTileImmediate(tile)
        
        // 2. Keep in-memory cache for Undo capability
        pendingDeletions[tile.id] = tile

        activeDeletionJob = scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Tile removed",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            when (result) {
                SnackbarResult.ActionPerformed -> {
                    triggerHaptic("TICK")
                    // Restore to DB
                    onUndoDeleteTile(tile)
                    if (reopenTileIdParam != null && selectedLightboxIndex != null) {
                        reopenTileId  = tile.id
                        reopenRequest++          // always changes → effect always re-runs
                    }
                    pendingDeletions.remove(tile.id)
                }
                SnackbarResult.Dismissed -> {
                    // Finalize: delete physical image file
                    onDeleteTileConfirm(tile)
                    kotlinx.coroutines.delay(100)
                    pendingDeletions.remove(tile.id)
                }
            }
            activeDeletionJob = null
        }
    }

    val handleMultiDeletion: (List<BentoEntity>) -> Unit = { tilesToDelete ->
        activeDeletionJob?.cancel()
        val count = tilesToDelete.size
        
        // 1. Immediately delete all selected from DB so abrupt process closure does not resurrect them
        tilesToDelete.forEach { onDeleteTileImmediate(it) }
        
        // 2. Keep in-memory cache for Undo capability
        tilesToDelete.forEach { pendingDeletions[it.id] = it }
        
        isSelectionMode = false
        selectedTileIds = emptySet()

        activeDeletionJob = scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = if (count == 1) "Tile removed" else "$count tiles removed",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            when (result) {
                SnackbarResult.ActionPerformed -> {
                    triggerHaptic("TICK")
                    // Restore all to DB
                    tilesToDelete.forEach { onUndoDeleteTile(it) }
                    tilesToDelete.forEach { pendingDeletions.remove(it.id) }
                }
                SnackbarResult.Dismissed -> {
                    // Finalize: delete physical image files
                    tilesToDelete.forEach { onDeleteTileConfirm(it) }
                    kotlinx.coroutines.delay(100)
                    tilesToDelete.forEach { pendingDeletions.remove(it.id) }
                }
            }
            activeDeletionJob = null
        }
    }

    // --- 4. DATA FILTERING ---
    val visibleTiles = remember(tiles, pendingDeletions.size) {
        tiles?.filter { it.id !in pendingDeletions.keys }
    }


    // Tie status bar visibility directly to fabsVisible and isSelectionMode
    // DisposableEffect re-runs whenever fabsVisible or isSelectionMode changes
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    DisposableEffect(fabsVisible, isSelectionMode, project.isBackground, isDarkTheme) {
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            if (fabsVisible || isSelectionMode) {
                // Controls visible — show status bar
                controller.show(WindowInsetsCompat.Type.statusBars())
            } else {
                // Controls hidden — hide status bar, full immersive
                controller.hide(WindowInsetsCompat.Type.statusBars())
            }

            if (project.isBackground) {
                // If project has a background image, we usually want light icons (dark scrim)
                controller.isAppearanceLightStatusBars = false
            } else {
                // Otherwise, follow the app's theme
                controller.isAppearanceLightStatusBars = !isDarkTheme
            }
        }
        onDispose {
            // Always restore status bar when leaving this screen
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.show(WindowInsetsCompat.Type.statusBars())
                controller.isAppearanceLightStatusBars = !isDarkTheme
            }
        }
    }

    // ── Back handler — two stage ──────────────────────────────────────────
    BackHandler {
        snackbarJob?.cancel()
        if (showMultiDeleteDialog) {
            showMultiDeleteDialog = false
        } else if (isSelectionMode) {
            isSelectionMode = false
            selectedTileIds = emptySet()
        } else if (!fabsVisible) {
            fabsVisible = true  // first back press: show controls (status bar also restores)
        } else {
            snackbarHostState.currentSnackbarData?.dismiss()
            onBackClick()       // second back press: navigate back
        }
    }

    // ── FAB row slide animation ───────────────────────────────────────────
    val rowOffsetY by animateFloatAsState(
        targetValue = if (isSelectionMode || fabsVisible) 0f else 300f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "rowSlide"
    )
    val rowAlpha by animateFloatAsState(
        targetValue = if (isSelectionMode || fabsVisible) 1f else 0f,
        animationSpec = tween(280),
        label = "rowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── STEP 1: FIRM BACKGROUND LAYER ───────────────────────────────
        if (project.isBackground && project.imageUrl.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = File(project.imageUrl),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center
                )

                // Subtle Scrim to ensure tiles and UI are readable over the image
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.5f)
                                )
                            )
                        )
                )
            }
        }
        // ── Main content ──────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = when {
                    tiles == null -> "loading"
                    visibleTiles.isNullOrEmpty() -> "empty"
                    else -> "content"
                },
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
                label = "collectionState"
            ) { state ->
                when (state) {
                    "loading" -> BentoLoadingView(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 120.dp)
                    )

                    "empty" -> CollectionEmptyView(
                        projectName = projectName,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 120.dp)
                    )

                    "content" -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        BentoGrid(
                            tiles = visibleTiles ?: emptyList(),
                            shapeIndex = project.shapeIndex,
                            initialLoad = isFirstTimeEntry,
                            isSelectionMode = isSelectionMode,
                            selectedIds = selectedTileIds,
                            onTileClick = { clickedTile ->
                                if (isSelectionMode) {
                                    if (clickedTile.id in selectedTileIds) {
                                        selectedTileIds = selectedTileIds - clickedTile.id
                                        if (selectedTileIds.isEmpty()) isSelectionMode = false
                                    } else {
                                        selectedTileIds = selectedTileIds + clickedTile.id
                                    }
                                    triggerHaptic("TICK")
                                } else if (!clickedTile.imageUri.isNullOrEmpty()) {
                                    val index = imageTiles.indexOfFirst { it.id == clickedTile.id }
                                    if (index != -1) selectedLightboxIndex = index
                                }
                            },
                            onTileLongClick = { specificTile ->
                                if (!isSelectionMode) {
                                    tileOptionsTarget = specificTile
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 12.dp, end = 12.dp,
                                    top = if (isSelectionMode || fabsVisible) 80.dp else 24.dp,
                                    bottom = if (isSelectionMode || fabsVisible) 75.dp else 10.dp,
                                ),
                            gapDp = 8
                        )
                        LaunchedEffect(Unit) {
                            if (isFirstTimeEntry) {
                                isFirstTimeEntry = false
                            }
                        }
                    }
                }
            }
        }


        // ── TILE OPTIONS BOTTOM SHEET ─────────────────────────────────────
        if (tileOptionsTarget != null) {
            ModalBottomSheet(
                onDismissRequest = { tileOptionsTarget = null },
                sheetState = sheetState,
                modifier = Modifier.widthIn(max = 480.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)) },
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                TileActionContent(
                    tile = tileOptionsTarget!!,
                    onEdit = {
                        val target = tileOptionsTarget!!
                        tileOptionsTarget = null
                        navigateAway { onEditTileClick(target.projectId, target.id) }
                    },
                    onDelete = {
                        val target = tileOptionsTarget!!
                        tileOptionsTarget = null
                        handleTileDeletion(target, null)
                    },
                    onView = {
                        val target = tileOptionsTarget!!
                        val index = imageTiles.indexOfFirst { it.id == target.id }
                        if (index != -1) {
                            selectedLightboxIndex = index
                        }
                        tileOptionsTarget = null
                    },
                    onSelect = {
                        val target = tileOptionsTarget!!
                        tileOptionsTarget = null
                        isSelectionMode = true
                        selectedTileIds = selectedTileIds + target.id
                    }
                )
            }
        }

        // ── Floating title — only when controls are visible ───────────────
        if (isSelectionMode || fabsVisible) {
            DetailTopBar(
                projectName = projectName,
                offsetY = rowOffsetY,
                alpha = rowAlpha,
                isDarkBackground = project.isBackground
            )
        }

        // ── Bottom FAB row ────────────────────────────────────────────────
        val bgColor = if (project.isBackground) Color.Black else MaterialTheme.colorScheme.background

        val whisperScrim = remember(bgColor) {
            listOf(
                Color.Transparent,
                bgColor.copy(alpha = 0.2f),
                bgColor.copy(alpha = 0.35f)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = rowOffsetY
                    alpha = rowAlpha
                }
                .drawWithContent {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = whisperScrim,
                            startY = size.height * 0.4f,
                            endY = size.height
                        )
                    )
                    drawContent()
                }
                .navigationBarsPadding()
                .padding(bottom = 14.dp)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSelectionMode) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SelectionModeButton(
                        icon = Icons.Default.Close,
                        label = "",
                        onClick = {
                            triggerHaptic("TICK")
                            selectedTileIds = emptySet()
                            isSelectionMode = false
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(52.dp)
                    )
                    val isAllSelected = selectedTileIds.size == (visibleTiles?.size ?: 0) && (visibleTiles?.isNotEmpty() == true)
                    SelectionModeButton(
                        icon = if (isAllSelected) Icons.Default.RemoveCircleOutline else Icons.Default.DoneAll,
                        label = if (isAllSelected) "Deselect All" else "Select All",
                        onClick = {
                            triggerHaptic("CONFIRM")
                            val allIds = visibleTiles?.map { it.id } ?: emptyList()
                            if (selectedTileIds.size == allIds.size && allIds.isNotEmpty()) {
                                selectedTileIds = emptySet()
                            } else {
                                selectedTileIds = allIds.toSet()
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    val hasSelections = selectedTileIds.isNotEmpty()
                    SelectionModeButton(
                        icon = Icons.Default.Delete,
                        label = if (hasSelections) "Delete (${selectedTileIds.size})" else "Delete",
                        onClick = {
                            triggerHaptic("TICK")
                            if (hasSelections) {
                                showMultiDeleteDialog = true
                            }
                        },
                        containerColor = if (hasSelections) Color(0xFFDC2626) else MaterialTheme.colorScheme.surface,
                        contentColor = if (hasSelections) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallActionFab(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = {
                            triggerHaptic("TICK")
                            navigateAway { onBackClick() }
                        }
                    )
                    BentoFab(
                        onClick = {
                            triggerHaptic("CONFIRM")
                            navigateAway { onAddTileClick(project.id) }
                        },
                        visible = true,
                        label = "Add Tile",
                        compact = true
                    )
                    SmallActionFab(
                        icon = Icons.Default.VisibilityOff,
                        contentDescription = "Hide controls",
                        onClick = {
                            triggerHaptic("TICK")
                            fabsVisible = false
                            snackbarJob = scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Press back to show controls",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    )
                }
            }
        }

        // Image Viewer
        if (selectedLightboxIndex != null && imageTiles.isNotEmpty()) {
            val safeIndex = selectedLightboxIndex!!.coerceIn(0, imageTiles.lastIndex)
            BentoLightbox(
                titles = imageTiles.map { it.title },
                imagePaths = imagePaths,
                initialIndex = safeIndex,
                initialStatusBarVisible = fabsVisible,
                onUiVisibilityChange = { isViewerUiActive = it },
                onEdit = { index ->
                    val targetTile = imageTiles[index]
                    reopenViewerForTileId = targetTile.id
                    reopenViewerFallbackIndex = index
                    editedFromViewer = true
                    // Don't null selectedLightboxIndex — keep the lightbox visible
                    // so the collection screen doesn't flash during the nav transition.
                    // The lightbox composable will naturally disappear when the
                    // navigation disposes this screen's composition.
                    navigateAway {
                        onEditTileClick(targetTile.projectId, targetTile.id)
                    }
                },
                onDelete = { index ->
                    val tileToDelete = imageTiles[index]
                    val reopenToId = tileToDelete.id      // stable ID, never goes stale
                    if (imageTiles.size <= 1) selectedLightboxIndex = null
                    handleTileDeletion(tileToDelete, reopenToId)
                },
                onDismiss = {
                    selectedLightboxIndex = null
                }
            )
        }

        // ── Snackbar ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(bottom = snackbarBottomPadding),
            contentAlignment = Alignment.BottomCenter
        ) {
            SnackbarHost(hostState = snackbarHostState) { data ->
                val isDelete = data.visuals.message.contains("removed")
                val isActionSuccess = data.visuals.message.contains("created") ||
                        data.visuals.message.contains("updated")

                Snackbar(
                    snackbarData = data,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    containerColor = when {
                        isDelete -> MaterialTheme.colorScheme.errorContainer
                        isActionSuccess -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    },
                    contentColor = when {
                        isDelete -> MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                    },
                    actionColor = if (isDelete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
        // ── Multi-Delete Confirmation Sheet ──
        if (showMultiDeleteDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { showMultiDeleteDialog = false }
                )

                Surface(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        Text(
                            text = "Delete ${selectedTileIds.size} tile${if (selectedTileIds.size > 1) "s" else ""}?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = if (selectedTileIds.size == 1) "1 tile will be removed permanently." else "${selectedTileIds.size} tiles will be removed permanently.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(Modifier.height(28.dp))

                        Button(
                            onClick = {
                                triggerHaptic("CONFIRM")
                                val tilesToDelete = visibleTiles?.filter { it.id in selectedTileIds } ?: emptyList()
                                showMultiDeleteDialog = false
                                handleMultiDeletion(tilesToDelete)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                "Delete permanently",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        TextButton(
                            onClick = { showMultiDeleteDialog = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                "Cancel",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailTopBar(
    projectName: String,
    offsetY: Float,
    alpha: Float,
    isDarkBackground: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = -offsetY
                this.alpha = alpha
            }
            .statusBarsPadding()
            .padding(top = 20.dp, bottom = 16.dp)
            .drawWithContent { drawContent() }
    ) {
        Text(
            text = projectName,
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = if (isDarkBackground) Color.White
            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
            letterSpacing = (-0.5).sp
        )
    }
}

@Composable
private fun SmallActionFab(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        onClick = onClick,
        modifier = Modifier.size(52.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun SelectionModeButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        shadowElevation = 6.dp,
        modifier = modifier.height(52.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
            if (label.isNotEmpty()) {
                Spacer(Modifier.size(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
private fun CollectionEmptyView(
    projectName: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BentoEmptyAnimation(modifier = Modifier.size(140.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                text = projectName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "This collection is empty",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
            Text(
                text = "Tap Add Tile below to get started",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                letterSpacing = 0.sp
            )
        }
    }
}

@Composable
private fun TileActionContent(
    tile: BentoEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onView: () -> Unit,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = screenHeight - 64.dp)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if(tile.title.isBlank()) "Untitled Tile" else tile.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // VIEW ACTION (Only if image exists)
        if (!tile.imageUri.isNullOrEmpty()) {
            ActionRow(
                icon = Icons.Default.Visibility,
                label = "View Full Image",
                color = MaterialTheme.colorScheme.primary,
                onClick = onView
            )
        }

        // EDIT ACTION
        ActionRow(
            icon = Icons.Default.Edit,
            label = "Edit Tile Settings",
            color = MaterialTheme.colorScheme.onSurface,
            onClick = onEdit
        )

        // SELECT ACTION
        ActionRow(
            icon = Icons.Default.CheckCircle,
            label = "Select",
            color = BentoSelectGreen,
            onClick = onSelect
        )

        // DELETE ACTION
        ActionRow(
            icon = Icons.Default.Delete,
            label = "Delete Tile",
            color = MaterialTheme.colorScheme.error,
            onClick = onDelete
        )
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}