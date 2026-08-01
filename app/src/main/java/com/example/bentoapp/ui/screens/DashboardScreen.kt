package com.example.bentoapp.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import com.example.bentoapp.data.ProjectEntity
import com.example.bentoapp.data.BentoEntity
import com.example.bentoapp.data.ProjectCounts
import com.example.bentoapp.ui.components.AddProjectDialog
import com.example.bentoapp.ui.components.ManageLocksDialog
import com.example.bentoapp.ui.components.BentoLightbox
import com.example.bentoapp.ui.components.BentoFab
import com.example.bentoapp.ui.components.BentoBottomNavigation
import com.example.bentoapp.utils.PreferenceManager
import com.example.bentoapp.viewmodel.BentoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: BentoViewModel,
    projects: List<ProjectEntity>,
    projectCounts: Map<Int, ProjectCounts>,
    preferenceManager: PreferenceManager,
    currentThemeMode: com.example.bentoapp.utils.ThemeMode,
    onProjectClick: (ProjectEntity) -> Unit,
    onProjectCreated: suspend (String, String, Boolean, Int, Boolean) -> ProjectEntity,
    onProjectDeletedImmediate: (ProjectEntity, (List<BentoEntity>) -> Unit) -> Unit,
    onUndoProjectDelete: (ProjectEntity, List<BentoEntity>) -> Unit,
    onProjectDeleteConfirm: (ProjectEntity, List<BentoEntity>) -> Unit,
    onProjectUpdated: (ProjectEntity, String, Boolean, Int, Boolean) -> Unit,
    onToggleProjectLock: ((ProjectEntity) -> Unit)? = null,
    isBiometricLockEnabled: Boolean = false,
    onBiometricLockToggle: (Boolean) -> Unit = {},
    onRequireBiometricAuth: ((String, String, () -> Unit) -> Unit)? = null,
    onOpenManageLocksDialog: () -> Unit = {},
    onUnlockAllCollections: () -> Unit = {},
    showManageLocksDialog: Boolean = false,
    onDismissManageLocksDialog: () -> Unit = {},
    onConfirmBatchLocks: (Set<Int>) -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf("collections") }
    
    // ── Search State ──
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }

    val ProjectSaver: Saver<ProjectEntity?, *> = mapSaver(
        save = { project ->
            if (project == null) emptyMap()
            else mapOf("id" to project.id, "name" to project.name, "imageUrl" to project.imageUrl)
        },
        restore = { map ->
            if (map.isEmpty()) null
            else ProjectEntity(
                id = map["id"] as Int,
                name = map["name"] as String,
                imageUrl = map["imageUrl"] as String
            )
        }
    )

    var projectToEdit: ProjectEntity? by rememberSaveable(stateSaver = ProjectSaver) { mutableStateOf(null) }
    var projectToDelete by rememberSaveable(stateSaver = ProjectSaver) { mutableStateOf(null) }

    val pendingDeletions = remember { mutableStateMapOf<Int, ProjectEntity>() }
    val cachedProjectTiles = remember { mutableStateMapOf<Int, List<BentoEntity>>() }
    var activeDeletionJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // ── List & FAB State ──
    val collectionsListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val galleryListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val settingsScrollState = androidx.compose.foundation.rememberScrollState()
    var lastScrollIndex by remember { mutableStateOf(0) }
    var fabVisible by remember { mutableStateOf(true) }

    LaunchedEffect(collectionsListState.firstVisibleItemIndex, collectionsListState.firstVisibleItemScrollOffset) {
        val currentIndex = collectionsListState.firstVisibleItemIndex
        fabVisible = when {
            currentIndex < lastScrollIndex -> true
            currentIndex > lastScrollIndex -> false
            else -> fabVisible
        }
        lastScrollIndex = currentIndex
    }

    // ── Dynamic Padding State ──
    val density = LocalDensity.current
    var bottomBarHeight by remember { mutableStateOf(94.dp) }
    var fabHeight by remember { mutableStateOf(64.dp) }


    val navInsets = androidx.compose.foundation.layout.WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()

    // 2. Exact math: NavInsets + 16dp padding + 68dp Bottom Nav height
    val trueBottomBarHeight = navInsets + 84.dp

    // 3. Tighten the spacing so it doesn't float into the middle of the screen
    val exactFabHeight = 56.dp
    val gapBetweenNavAndFab = 6.dp

    val snackbarBottomPadding by animateDpAsState(
        targetValue = if (fabVisible && selectedTab == "collections") {
            trueBottomBarHeight + gapBetweenNavAndFab + exactFabHeight + 16.dp
        } else {
            trueBottomBarHeight + 16.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "snackbarPadding"
    )

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val vibrator = remember {
        context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
    }

    val triggerHaptic = { effectType: String ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when (effectType) {
                "LONG_PRESS" -> vibrator.vibrate(android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_HEAVY_CLICK))
                "DELETE_CONFIRM" -> vibrator.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                "TRIGGER" -> vibrator.vibrate(android.os.VibrationEffect.createOneShot(20, 180))
                "CONFIRM" -> vibrator.vibrate(android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_CLICK))
                "TICK" -> haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            }
        } else {
            vibrator.vibrate(15)
        }
    }

    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    LaunchedEffect(projectToDelete) {
        if (projectToDelete != null) {
            keyboardController?.hide()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activeDeletionJob?.cancel()
            pendingDeletions.forEach { (id, project) ->
                val tiles = cachedProjectTiles[id] ?: emptyList()
                onProjectDeleteConfirm(project, tiles)
            }
            pendingDeletions.clear()
            cachedProjectTiles.clear()
        }
    }

    val visibleProjects by remember(projects, pendingDeletions.size, searchQuery, isSearchActive, selectedTab) {
        derivedStateOf {
            val filtered = projects.filter { it.id !in pendingDeletions.keys }

            if (isSearchActive && selectedTab == "collections" && searchQuery.isNotEmpty()) {
                filtered.filter { 
                    it.name.contains(searchQuery, ignoreCase = true) 
                }.sortedWith(
                    compareBy<ProjectEntity> { 
                        val index = it.name.indexOf(searchQuery, ignoreCase = true)
                        if (index == -1) Int.MAX_VALUE else index
                    }.thenBy { it.name.lowercase() }
                )
            } else {
                filtered
            }
        }
    }

    val isLoading by viewModel.isLoading.collectAsState()
    val allGalleryImages by viewModel.allGalleryImages.collectAsState()
    val projectTilesMap = remember(allGalleryImages) {
        allGalleryImages.groupBy { it.projectId }
    }
    val collapsedProjectIds by preferenceManager.collapsedProjectIds.collectAsState(initial = emptySet())
    var previewLightboxState by remember { mutableStateOf<Pair<BentoEntity, List<BentoEntity>>?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val indexMap = mapOf("collections" to 0, "gallery" to 1, "settings" to 2)
                    val targetIndex = indexMap[targetState] ?: 0
                    val initialIndex = indexMap[initialState] ?: 0

                    if (targetIndex > initialIndex) {
                        (slideInHorizontally { width -> width / 3 } + fadeIn(tween(350))).togetherWith(
                            slideOutHorizontally { width -> -width / 3 } + fadeOut(tween(350))
                        )
                    } else {
                        (slideInHorizontally { width -> -width / 3 } + fadeIn(tween(350))).togetherWith(
                            slideOutHorizontally { width -> width / 3 } + fadeOut(tween(350))
                        )
                    }
                },
                label = "dashboardTab"
            ) { tab ->
                when (tab) {
                    "collections" -> {
                        CollectionsScreen(
                            visibleProjects = visibleProjects,
                            projectCounts = projectCounts,
                            projectTilesMap = projectTilesMap,
                            collapsedProjectIds = collapsedProjectIds,
                            isLoading = isLoading,
                            searchQuery = searchQuery,
                            isSearchActive = isSearchActive,
                            onSearchQueryChange = { searchQuery = it },
                            onSearchActiveChange = { isSearchActive = it },
                            onProjectClick = onProjectClick,
                            onProjectDeleteRequest = { projectToDelete = it },
                            onProjectEditRequest = { project ->
                                if (project.isLocked && onRequireBiometricAuth != null) {
                                    onRequireBiometricAuth("Unlock ${project.name}", "Authenticate fingerprint to edit collection") {
                                        projectToEdit = project
                                    }
                                } else {
                                    projectToEdit = project
                                }
                            },
                            onToggleLockRequest = onToggleProjectLock,
                            onToggleExpand = { projId, isCollapsed ->
                                scope.launch { preferenceManager.setProjectCollapsed(projId, isCollapsed) }
                            },
                            onTileClick = { tile, collectionTiles ->
                                previewLightboxState = Pair(tile, collectionTiles)
                            },
                            triggerHaptic = triggerHaptic,
                            bottomBarHeight = trueBottomBarHeight,
                            listState = collectionsListState,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    "gallery" -> {
                        GalleryScreen(
                            viewModel = viewModel,
                            bottomPadding = trueBottomBarHeight,
                            listState = galleryListState,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    "settings" -> {
                        SettingsScreen(
                            currentThemeMode = currentThemeMode,
                            onThemeSelected = { mode ->
                                scope.launch { preferenceManager.setThemeMode(mode) }
                            },
                            projects = projects,
                            onOpenManageLocksDialog = onOpenManageLocksDialog,
                            onUnlockAllCollections = onUnlockAllCollections,
                            bottomPadding = trueBottomBarHeight,
                            scrollState = settingsScrollState,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // ── Floating UI Layer ──

        // 0. Dynamic Scrim
        if (!isLoading) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(trueBottomBarHeight)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.4f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.85f)
                            )
                        )
                    )
            )
        }

        // 1. FAB
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = trueBottomBarHeight + gapBetweenNavAndFab)
        ) {
            BentoFab(
                onClick = {
                    triggerHaptic("CONFIRM")
                    showAddDialog = true
                },
                visible = fabVisible && selectedTab == "collections" && !isLoading
            )
        }

        // 2. Bottom Nav
        AnimatedVisibility(
            visible = !isLoading,
            enter = fadeIn(tween(350)) + slideInVertically { it },
            exit = fadeOut(tween(350)) + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .zIndex(15f)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp, start = 24.dp, end = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                BentoBottomNavigation(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        triggerHaptic("TICK")
                        if (selectedTab == tab) {
                            scope.launch {
                                when (tab) {
                                    "collections" -> collectionsListState.animateScrollToItem(0)
                                    "gallery" -> galleryListState.animateScrollToItem(0)
                                    "settings" -> settingsScrollState.animateScrollTo(0)
                                }
                            }
                        } else {
                            selectedTab = tab
                        }
                    }
                )
            }
        }

        // ── Snackbar ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = snackbarBottomPadding), // Perfect mathematical offset
            contentAlignment = Alignment.BottomCenter
        ) {
            SnackbarHost(
                hostState = snackbarHostState,
            ) { data ->
                val isDelete = data.visuals.message.contains("removed")
                Snackbar(
                    snackbarData = data,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    containerColor = if (isDelete) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (isDelete) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onPrimaryContainer,
                    actionColor = if (isDelete) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }
        }

        // ── Add Dialog ──
        if (showAddDialog) {
            AddProjectDialog(
                onDismiss = { showAddDialog = false },
                triggerHaptic = triggerHaptic,
                onRequireBiometricAuth = onRequireBiometricAuth,
                onConfirm = { name, image, isBackground, shapeIndex, isLocked ->
                    scope.launch {
                        val newProject = onProjectCreated(name, image, isBackground, shapeIndex, isLocked)
                        showAddDialog = false
                        onProjectClick(newProject)
                    }
                }
            )
        }

        // ── Edit Dialog ──
        projectToEdit?.let { project ->
            key(project.id) {
                AddProjectDialog(
                    onDismiss = { projectToEdit = null },
                    triggerHaptic = triggerHaptic,
                    onRequireBiometricAuth = onRequireBiometricAuth,
                    onConfirm = { name, imageUri, isBackground, shapeIndex, isLocked ->
                        onProjectUpdated(project.copy(name = name), imageUri, isBackground, shapeIndex, isLocked)
                        projectToEdit = null
                    },
                    existingName = project.name,
                    existingImageUri = project.imageUrl,
                    existingIsBackground = project.isBackground,
                    existingShapeIndex = project.shapeIndex,
                    existingIsLocked = project.isLocked,
                    isEditMode = true
                )
            }
        }

        // ── Manage Locks Dialog ──
        if (showManageLocksDialog) {
            ManageLocksDialog(
                projects = projects,
                projectCounts = projectCounts,
                onDismiss = onDismissManageLocksDialog,
                onConfirmLocks = onConfirmBatchLocks,
                triggerHaptic = triggerHaptic
            )
        }

        // ── Delete Confirmation Sheet ──
        projectToDelete?.let { project ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(100f)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .imePadding(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { projectToDelete = null }
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
                            text = "Delete this Collection?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = project.name,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Everything inside will be permanently removed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(Modifier.height(28.dp))

                        Button(
                            onClick = {
                                triggerHaptic("CONFIRM")
                                val deletedProject = project
                                projectToDelete = null

                                activeDeletionJob?.cancel()
                                pendingDeletions.forEach { (id, pending) ->
                                    val tiles = cachedProjectTiles[id] ?: emptyList()
                                    onProjectDeleteConfirm(pending, tiles)
                                }
                                pendingDeletions.clear()
                                cachedProjectTiles.clear()

                                onProjectDeletedImmediate(deletedProject) { tiles ->
                                    cachedProjectTiles[deletedProject.id] = tiles
                                }
                                pendingDeletions[deletedProject.id] = deletedProject

                                activeDeletionJob = scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "\"${deletedProject.name}\" removed",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    when (result) {
                                        SnackbarResult.ActionPerformed -> {
                                            triggerHaptic("TICK")
                                            val tiles = cachedProjectTiles[deletedProject.id] ?: emptyList()
                                            onUndoProjectDelete(deletedProject, tiles)
                                            pendingDeletions.remove(deletedProject.id)
                                            cachedProjectTiles.remove(deletedProject.id)
                                        }
                                        SnackbarResult.Dismissed -> {
                                            val tiles = cachedProjectTiles[deletedProject.id] ?: emptyList()
                                            onProjectDeleteConfirm(deletedProject, tiles)
                                            pendingDeletions.remove(deletedProject.id)
                                            cachedProjectTiles.remove(deletedProject.id)
                                        }
                                    }
                                    activeDeletionJob = null
                                }
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
                            onClick = { projectToDelete = null },
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

        // ── Preview Reel Lightbox ──
        previewLightboxState?.let { (clickedTile, collectionTiles) ->
            val initialIndex = collectionTiles.indexOfFirst { it.id == clickedTile.id }.coerceAtLeast(0)
            BentoLightbox(
                titles = collectionTiles.map { it.title ?: "" },
                imagePaths = collectionTiles.map { it.imageUri ?: "" },
                initialIndex = initialIndex,
                initialStatusBarVisible = true,
                onUiVisibilityChange = {},
                onEdit = {},
                onDelete = {},
                onDismiss = { previewLightboxState = null }
            )
        }
    }
}
