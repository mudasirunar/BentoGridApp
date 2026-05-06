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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val window = (view.context as? android.app.Activity)?.window

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


    // FILTER TILES WITH IMAGES
    val imageTiles = remember(tiles, pendingDeletions.size) {
        tiles?.filter {
            !it.imageUri.isNullOrEmpty() && it.id !in pendingDeletions.keys
        } ?: emptyList()
    }

    val imagePaths = remember(imageTiles) {
        imageTiles.map { it.imageUri!! }
    }


    // --- 2. CLEANUP ON DISPOSE ---
    // If user leaves the screen while a snackbar is showing, finalize the delete!
    DisposableEffect(Unit) {
        onDispose {
            activeDeletionJob?.cancel()
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
                    if (reopenTileIdParam != null && selectedLightboxIndex != null) {
                        reopenTileId  = tile.id
                        reopenRequest++          // always changes → effect always re-runs
                    }
                    pendingDeletions.remove(tile.id)
                }
                SnackbarResult.Dismissed -> {
                    onDeleteTileConfirm(tile)
                    kotlinx.coroutines.delay(100)
                    pendingDeletions.remove(tile.id)
                }
            }
            activeDeletionJob = null
        }
    }

    // --- 4. DATA FILTERING ---
    val visibleTiles = remember(tiles, pendingDeletions.size) {
        tiles?.filter { it.id !in pendingDeletions.keys }
    }


    // Tie status bar visibility directly to fabsVisible
    // DisposableEffect re-runs whenever fabsVisible changes
    DisposableEffect(fabsVisible, project.isBackground) {
        if (window != null) {
            val controller = WindowInsetsControllerCompat(window, view)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            if (project.isBackground) {
                controller.isAppearanceLightStatusBars = false
            }

            if (fabsVisible) {
                // Controls visible — show status bar
                controller.show(WindowInsetsCompat.Type.statusBars())
            } else {
                // Controls hidden — hide status bar, full immersive
                controller.hide(WindowInsetsCompat.Type.statusBars())
            }
        }
        onDispose {
            // Always restore status bar when leaving this screen
            if (window != null) {
                WindowInsetsControllerCompat(window, view)
                    .show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    // ── Back handler — two stage ──────────────────────────────────────────
    BackHandler {
        snackbarJob?.cancel()
        if (!fabsVisible) {
            fabsVisible = true  // first back press: show controls (status bar also restores)
        } else {
            snackbarJob?.cancel()
            snackbarHostState.currentSnackbarData?.dismiss()
            onBackClick()       // second back press: navigate back
        }
    }

    // ── FAB row slide animation ───────────────────────────────────────────
    val rowOffsetY by animateFloatAsState(
        targetValue = if (fabsVisible) 0f else 300f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "rowSlide"
    )
    val rowAlpha by animateFloatAsState(
        targetValue = if (fabsVisible) 1f else 0f,
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
                            onTileClick = { clickedTile ->
                                if (!clickedTile.imageUri.isNullOrEmpty()) {
                                    val index = imageTiles.indexOfFirst { it.id == clickedTile.id }
                                    if (index != -1) selectedLightboxIndex = index
                                }
                            },
                            onTileLongClick = { specificTile ->
                                tileOptionsTarget = specificTile
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 12.dp, end = 12.dp,
                                    top = if (fabsVisible) 80.dp else 24.dp,
                                    bottom = if (fabsVisible) 75.dp else 10.dp,
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
                    }
                )
            }
        }
        // ── Floating title — only when controls are visible ───────────────
        if (fabsVisible) {
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
                .padding(horizontal = 24.dp)
        ) {
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

        // Image Viewer
        if (selectedLightboxIndex != null) {
            BentoLightbox(
                titles = imageTiles.map { it.title },
                imagePaths = imagePaths,
                initialIndex = selectedLightboxIndex!!,
                initialStatusBarVisible = fabsVisible,
                onUiVisibilityChange = { isViewerUiActive = it },
                onEdit = { index ->
                    val targetTile = imageTiles[index]
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
    onView: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
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