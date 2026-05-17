package com.example.bentoapp.ui.screens

import android.os.Build
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
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bentoapp.R
import com.example.bentoapp.data.ProjectEntity
import com.example.bentoapp.data.BentoEntity
import com.example.bentoapp.ui.components.AddProjectDialog
import com.example.bentoapp.ui.components.BentoEmptyAnimation
import com.example.bentoapp.ui.components.BentoFab
import com.example.bentoapp.ui.components.ProjectCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    projects: List<ProjectEntity>,
    projectCounts: Map<Int, com.example.bentoapp.data.ProjectCounts>,
    onProjectClick: (ProjectEntity) -> Unit,
    onProjectCreated: suspend (String, String, Boolean, Int) -> ProjectEntity,
    onProjectDeletedImmediate: (ProjectEntity, (List<BentoEntity>) -> Unit) -> Unit,
    onUndoProjectDelete: (ProjectEntity, List<BentoEntity>) -> Unit,
    onProjectDeleteConfirm: (ProjectEntity, List<BentoEntity>) -> Unit,
    onProjectDeleted: (ProjectEntity) -> Unit,
    onProjectUpdated: (ProjectEntity, String, Boolean, Int) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

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

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    val isScrolled = remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
                    listState.firstVisibleItemScrollOffset > 10
        }
    }

    var lastScrollIndex by remember { mutableStateOf(0) }
    var fabVisible by remember { mutableStateOf(true) }

    val isAtTop = remember {
        derivedStateOf { listState.firstVisibleItemIndex <= 1 }
    }

    val snackbarBottomPadding by animateDpAsState(
        targetValue = if (fabVisible) 66.dp else 24.dp,
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

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val currentIndex = listState.firstVisibleItemIndex
        fabVisible = when {
            currentIndex < lastScrollIndex -> true
            currentIndex > lastScrollIndex -> false
            else -> fabVisible
        }
        lastScrollIndex = currentIndex
    }

    DisposableEffect(Unit) {
        val activity = context as? android.app.Activity
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            activity?.requestedOrientation = originalOrientation
                ?: android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val visibleProjects by remember(projects, pendingDeletions.size) {
        derivedStateOf {
            projects.filter { it.id !in pendingDeletions.keys }
        }
    }

    var previousVisibleSize by remember { mutableIntStateOf(visibleProjects.size) }

    LaunchedEffect(visibleProjects.size) {
        if (visibleProjects.size > previousVisibleSize && isAtTop.value) {
            listState.animateScrollToItem(0)
        }
        previousVisibleSize = visibleProjects.size
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            snackbarHost = {},
            floatingActionButtonPosition = FabPosition.Center,
            floatingActionButton = {
                BentoFab(
                    onClick = {
                        triggerHaptic("CONFIRM")
                        showAddDialog = true
                    },
                    visible = fabVisible
                )
            }
        ) { innerPadding ->
            AnimatedContent(
                targetState = if (projects.isEmpty()) "empty" else "content",
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
                label = "dashboardState"
            ) { state ->
                when (state) {
                    "empty" -> EmptyShelfView(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )

                    "content" -> LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 100.dp,
                            bottom = 100.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(
                            items = projects.filter { it.id !in pendingDeletions.keys },
                            key = { _, project -> project.id }
                        ) { _, project ->
                            Box(
                                modifier = Modifier.animateItem(
                                    fadeInSpec = tween(400),
                                    fadeOutSpec = tween(400),
                                    placementSpec = tween(400)
                                )
                            ) {
                                ProjectCard(
                                    project = project,
                                    counts = projectCounts[project.id],
                                    onClick = { onProjectClick(project) },
                                    onDeleteRequest = { projectToDelete = project },
                                    onEditRequest = { projectToEdit = project },
                                    onHaptic = { type -> triggerHaptic(type) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── TopBar overlaid on top of everything — truly transparent when at top ──
        DashboardTopBar(isScrolled = isScrolled.value)

        // ── Snackbar ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = snackbarBottomPadding),
            contentAlignment = Alignment.BottomCenter
        ) {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = snackbarBottomPadding)
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
                onConfirm = { name, image, isBackground, shapeIndex ->
                    scope.launch {
                        val newProject = onProjectCreated(name, image, isBackground, shapeIndex)
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
                    onConfirm = { name, imageUri, isBackground, shapeIndex ->
                        onProjectUpdated(project.copy(name = name), imageUri, isBackground, shapeIndex)
                        projectToEdit = null
                    },
                    existingName = project.name,
                    existingImageUri = project.imageUrl,
                    existingIsBackground = project.isBackground,
                    existingShapeIndex = project.shapeIndex,
                    isEditMode = true
                )
            }
        }

        // ── Delete Confirmation Sheet ──
        projectToDelete?.let { project ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { projectToDelete = null }
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp),
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

                                // 1. Immediately delete from DB & cache the fetched tiles in memory
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
                                            // Restore project and its tiles back to DB
                                            val tiles = cachedProjectTiles[deletedProject.id] ?: emptyList()
                                            onUndoProjectDelete(deletedProject, tiles)
                                            pendingDeletions.remove(deletedProject.id)
                                            cachedProjectTiles.remove(deletedProject.id)
                                        }
                                        SnackbarResult.Dismissed -> {
                                            // Finalize: delete physical cover and tile images from storage
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
    }
}

@Composable
private fun DashboardTopBar(isScrolled: Boolean) {

    val bgAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 0.82f else 0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "topBarBg"
    )

    val titleScale by animateFloatAsState(
        targetValue = if (isScrolled) 0.82f else 1f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "titleScale"
    )

    val titleOffsetY by animateFloatAsState(
        targetValue = if (isScrolled) -6f else 0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "titleOffset"
    )

    // Title color animates from onSurface → primary on scroll
    val titleColorAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 1f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "titleColor"
    )
    val titleColor = lerp(
        MaterialTheme.colorScheme.onSurface,
        MaterialTheme.colorScheme.primary,
        titleColorAlpha
    )

    val logoRotation by animateFloatAsState(
        targetValue = if (isScrolled) 360f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "logoRotation"
    )

    val logoScale by animateFloatAsState(
        targetValue = if (isScrolled) 0.86f else 1f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "logoScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                // Bottom border line — modern glassmorphism feel
                drawRect(
                    color = Color.White.copy(alpha = if (isScrolled) 0.08f else 0f),
                    topLeft = Offset(0f, size.height - 1f),
                    size = Size(size.width, 1f)
                )
            }
            .background(
                color = MaterialTheme.colorScheme.background.copy(alpha = bgAlpha)
            )
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 20.dp, bottom = 16.dp)
            .clickable(enabled = false) {}
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Title — color interpolates from onSurface to primary as you scroll
            Text(
                text = "My Collections",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1.5).sp,
                color = titleColor,
                modifier = Modifier.graphicsLayer {
                    scaleX = titleScale
                    scaleY = titleScale
                    translationY = titleOffsetY
                    transformOrigin = TransformOrigin(0f, 0.5f)
                }
            )

            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(52.dp)
                    .graphicsLayer {
                        rotationZ = logoRotation
                        scaleX = logoScale
                        scaleY = logoScale
                    }
            )
        }
    }
}

@Composable
private fun EmptyShelfView(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BentoEmptyAnimation(modifier = Modifier.size(140.dp))

            Spacer(Modifier.height(8.dp))

            Text(
                "Your shelf is empty",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
            Text(
                "Tap + below to create your first collection",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                letterSpacing = 0.sp
            )
        }
    }
}
