package com.example.bentoapp.ui.components

// ─────────────────────────────────────────────────────────────────────────────
// build.gradle dependency — pick the one matching your Coil version:
//
//   Coil 2.x  →  implementation("io.github.panpf.zoomimage:zoomimage-compose-coil2:1.4.0")
//   Coil 3.x  →  implementation("io.github.panpf.zoomimage:zoomimage-compose-coil3:1.4.0")
// ─────────────────────────────────────────────────────────────────────────────

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.github.panpf.zoomimage.CoilZoomAsyncImage
import com.github.panpf.zoomimage.rememberCoilZoomState
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

// ─────────────────────────────────────────────────────────────────────────────
// BentoLightbox — unchanged
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BentoLightbox(
    titles: List<String>,
    imagePaths: List<String>,
    initialIndex: Int,
    initialStatusBarVisible: Boolean,
    onUiVisibilityChange: (Boolean) -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape   = configuration.orientation ==
            android.content.res.Configuration.ORIENTATION_LANDSCAPE


    var controlsVisible by rememberSaveable { mutableStateOf(initialStatusBarVisible) }

    LaunchedEffect(controlsVisible) {
        onUiVisibilityChange(controlsVisible)
    }


    // isZoomed only locks pager scroll. Does NOT affect UI visibility.
    var isZoomed by remember { mutableStateOf(false) }

    val view   = LocalView.current
    val window = (view.context as? android.app.Activity)?.window

    var isVisible       by rememberSaveable { mutableStateOf(false) }
    val pagerState      = rememberPagerState(initialPage = initialIndex) { imagePaths.size }
    var globalDragAlpha by remember { mutableFloatStateOf(1f) }
    val currentTitle    = titles.getOrNull(pagerState.currentPage) ?: ""

    LaunchedEffect(initialIndex) {
        if (pagerState.currentPage != initialIndex && initialIndex < imagePaths.size) {
            pagerState.scrollToPage(initialIndex)
        }
    }

    val scope = rememberCoroutineScope()
    val deleteTranslateX = remember { Animatable(0f) }
    val deleteAlpha      = remember { Animatable(1f) }

    val handleDelete: () -> Unit = {
        val currentPage = pagerState.currentPage
        // slide left if next image exists, slide right if this was last
        val direction = if (currentPage < imagePaths.size - 1) -1f else 1f
        scope.launch {
            coroutineScope {
                launch { deleteTranslateX.animateTo(direction * 500f, tween(220, easing = FastOutSlowInEasing)) }
                launch { deleteAlpha.animateTo(0f, tween(180)) }
            }
            onDelete(currentPage)
            // reset instantly — next image is already in place
            deleteTranslateX.snapTo(0f)
            deleteAlpha.snapTo(1f)
        }
    }

    LaunchedEffect(Unit) { isVisible = true }

    DisposableEffect(controlsVisible, isLandscape) {
        window?.let { win ->
            val controller = WindowInsetsControllerCompat(win, view)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (controlsVisible) {
                controller.show(WindowInsetsCompat.Type.statusBars())
            } else {
                controller.hide(WindowInsetsCompat.Type.statusBars())
            }
        }
        onDispose { }
    }

    fun safeDismiss() {
        isVisible = false
        onDismiss()
    }

    BackHandler(enabled = isVisible) { safeDismiss() }

    AnimatedVisibility(
        visible = isVisible,
        enter   = fadeIn(tween(250)),
        exit    = fadeOut(tween(250))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {}
                .background(Color.Black.copy(alpha = 0.90f * globalDragAlpha))
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(
                        top    = if (isLandscape) 0.dp else 80.dp,
                        bottom = if (isLandscape) 0.dp else 130.dp
                    )
                    .graphicsLayer {
                        translationX = deleteTranslateX.value
                        alpha        = deleteAlpha.value
                    },
            ) { pageIndex ->
                ZoomableImage(
                    imagePath      = imagePaths[pageIndex],
                    onDismiss      = { safeDismiss() },
                    onZoomChanged  = { isZoomed = it },
                    onDragProgress = { progress -> globalDragAlpha = progress },
                    onImageTap     = { controlsVisible = !controlsVisible }
                )
            }

            // Close button
            AnimatedVisibility(
                visible  = controlsVisible,
                enter    = fadeIn() + slideInVertically { -it },
                exit     = fadeOut() + slideOutVertically { -it },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Box(modifier = Modifier.statusBarsPadding().padding(16.dp)) {
                    LightboxTopControls(
                        onEdit    = { onEdit(pagerState.currentPage) },
                        onDelete  = { handleDelete() },
                        onDismiss = { safeDismiss() }
                    )
                }
            }

            if (!isLandscape) {
                AnimatedVisibility(
                    visible  = controlsVisible,
                    enter    = fadeIn() + slideInVertically { it },
                    exit     = fadeOut() + slideOutVertically { it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
                            .graphicsLayer { alpha = globalDragAlpha },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val displayTitle = if (currentTitle.isNotBlank()) currentTitle else " "
                        CounterChip(current = pagerState.currentPage + 1, total = imagePaths.size)
                        TitleLabel(title = displayTitle, isLandscape = false)
                    }
                }
            } else {
                AnimatedVisibility(
                    visible  = controlsVisible,
                    enter    = fadeIn() + slideInHorizontally { it },
                    exit     = fadeOut() + slideOutHorizontally { it },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(150.dp)
                            .navigationBarsPadding()
                            .padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
                            .graphicsLayer { this.alpha = globalDragAlpha },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        if (currentTitle.isNotBlank()) {
                            TitleLabel(title = currentTitle, isLandscape = true)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        CounterChip(current = pagerState.currentPage + 1, total = imagePaths.size)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UI components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LightboxTopControls(onEdit: () -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    Surface(
        color = Color.White.copy(alpha = 0.12f),
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.height(44.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            // Segment 1: Edit
            Surface(onClick = onEdit, color = Color.White.copy(alpha = 0.15f), shape = CircleShape, modifier = Modifier.fillMaxHeight(0.85f)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
                    Text("Edit", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Spacer(Modifier.width(4.dp))
            // Segment 2: Delete (Icon)
            Surface(onClick = onDelete, color = Color.White.copy(alpha = 0.15f), shape = CircleShape, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, "Delete", tint = Color.White, modifier = Modifier.padding(9.dp))
            }
            Spacer(Modifier.width(4.dp))
            // Segment 3: Close
            Surface(onClick = onDismiss, color = Color.White.copy(alpha = 0.15f), shape = CircleShape, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Close, "Close", tint = Color.White, modifier = Modifier.padding(8.dp))
            }
        }
    }
}

@Composable
private fun CounterChip(current: Int, total: Int) {
    if (total > 1) {
        Surface(color = Color.White.copy(alpha = 0.18f), shape = CircleShape) {
            Text(
                text       = "$current / $total",
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
                modifier   = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun TitleLabel(title: String, isLandscape: Boolean, modifier: Modifier = Modifier) {
    if (!isLandscape || title.isNotBlank()) {
        Text(
            text          = title,
            style         = MaterialTheme.typography.titleMedium,
            fontWeight    = FontWeight.ExtraBold,
            color         = Color.White,
            textAlign     = androidx.compose.ui.text.style.TextAlign.Center,
            modifier      = modifier.fillMaxWidth(),
            letterSpacing = (-0.5).sp,
            maxLines      = 3
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ZoomableImage
//
//  Tap / double-tap logic
//  ───────────────────────
//  Problem 1 — Single tap didn't work while zoomed in:
//    Old code had `if (userTransform.scaleX > 1.01f) return@awaitEachGesture`
//    which exited the ENTIRE gesture handler, so tap detection never ran.
//    Fix: remove the blanket early return. Instead only gate the DISMISS
//    logic behind the zoom check. Tap detection always runs.
//
//  Problem 2 — Double-tap was toggling the UI:
//    On tap-1 up  → singleTapJob timer started (300 ms).
//    On tap-2 down → singleTapJob correctly cancelled.
//    On tap-2 up  → hasMoved=false, so a NEW singleTapJob was started for
//                   tap-2, which then fired onImageTap after 300 ms.
//    Fix: at finger-down, snapshot `wasDoubleTap = singleTapJob?.isActive == true`
//    BEFORE cancelling. If true this gesture is the second tap of a double-tap
//    → skip starting singleTapJob on its finger-up.
//
// ─────────────────────────────────────────────────────────────────────────────

private const val DOUBLE_TAP_WINDOW_MS = 300L
private const val TAP_SLOP_PX         = 30f

@Composable
private fun ZoomableImage(
    imagePath      : String,
    onDismiss      : () -> Unit,
    onZoomChanged  : (Boolean) -> Unit,
    onDragProgress : (Float) -> Unit,
    onImageTap     : () -> Unit
) {
    val scope     = rememberCoroutineScope()
    val touchSlop = LocalViewConfiguration.current.touchSlop

    val zoomState = rememberCoilZoomState()

    // userTransform only — ContentScale base scale excluded (fixes portrait/landscape breakage)
    val isUserZoomed = zoomState.zoomable.userTransform.scaleX > 1.01f
    LaunchedEffect(isUserZoomed) { onZoomChanged(isUserZoomed) }

    val dismissY   = remember { Animatable(0f) }
    var physicsJob : Job? = remember { null }
    var singleTapJob: Job? = remember { null }

    // ── Vibrator ──────────────────────────────────────────────────────────────
    val context  = androidx.compose.ui.platform.LocalContext.current
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE)
                    as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    val triggerClickHaptic: () -> Unit = {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
            else -> @Suppress("DEPRECATION") vibrator.vibrate(20)
        }
    }

    val triggerSpringHaptic: () -> Unit = {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                vibrator.vibrate(
                    VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 1.0f)
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.7f, 20)
                        .compose()
                )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 10, 30, 8), -1))
            else -> @Suppress("DEPRECATION") vibrator.vibrate(longArrayOf(0, 15, 20, 10), -1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = dismissY.value
                val drag     = (dismissY.value / 2000f).coerceIn(0f, 1f)
                alpha        = 1f - drag
                val s        = 1f - drag * 0.2f
                scaleX       = s
                scaleY       = s
            }
            .pointerInput(Unit) {
                val velocityTracker = VelocityTracker()

                coroutineScope {
                    awaitEachGesture {

                        // ── FINGER DOWN ───────────────────────────────────
                        val downEvent  = awaitPointerEvent(PointerEventPass.Initial)
                        val downChange = downEvent.changes.firstOrNull { it.pressed }
                            ?: return@awaitEachGesture
                        val startPos   = downChange.position

                        // FIX 2: snapshot BEFORE cancelling.
                        // If a timer was actively counting down it means this
                        // finger-down is tap-2 of a double-tap. We must NOT
                        // start a new singleTapJob on this gesture's finger-up.
                        val isSecondTapOfDoubleTap = singleTapJob?.isActive == true
                        singleTapJob?.cancel()
                        singleTapJob = null

                        velocityTracker.resetTracking()
                        velocityTracker.addPosition(downChange.uptimeMillis, downChange.position)

                        physicsJob?.cancel()
                        physicsJob = null

                        val currentlyZoomed = zoomState.zoomable.userTransform.scaleX > 1.01f

                        var isDismiss         = false
                        var directionResolved = false
                        var hasMoved          = false
                        var localDismissY     = 0f

                        // ── EVENT LOOP ────────────────────────────────────
                        while (true) {
                            val event  = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == downChange.id }

                            // ── FINGER UP ─────────────────────────────────
                            if (change == null || !change.pressed) {

                                when {
                                    // Dismiss committed or cancelled — run physics
                                    isDismiss -> {
                                        val velocity = velocityTracker.calculateVelocity()
                                        physicsJob = scope.launch {
                                            if (localDismissY > 600f || velocity.y > 1800f) {
                                                triggerClickHaptic()
                                                dismissY.animateTo(3000f, tween(350))
                                                onDismiss()
                                            } else {
                                                triggerSpringHaptic()
                                                onDragProgress(1f)
                                                dismissY.animateTo(
                                                    0f,
                                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                                )
                                            }
                                        }
                                    }

                                    // Finger lifted without movement AND this is
                                    // NOT the second tap of a double-tap → tap.
                                    // FIX 1: no early return for zoom — tap detection
                                    // always runs regardless of zoom state.
                                    // FIX 2: skip if isSecondTapOfDoubleTap so tap-2
                                    // of a double-tap never triggers the UI toggle.
                                    !hasMoved && !isSecondTapOfDoubleTap -> {
                                        singleTapJob = scope.launch {
                                            delay(DOUBLE_TAP_WINDOW_MS)
                                            onImageTap()
                                        }
                                    }
                                }
                                break
                            }

                            val totalDelta = change.position - startPos
                            if (totalDelta.getDistance() > TAP_SLOP_PX) hasMoved = true

                            // Only run dismiss detection when NOT zoomed in.
                            // When zoomed, ZoomImage owns the pan gesture — don't intercept.
                            if (!currentlyZoomed) {
                                if (!directionResolved && totalDelta.getDistance() > touchSlop) {
                                    directionResolved = true
                                    isDismiss = abs(totalDelta.y) > abs(totalDelta.x) && totalDelta.y > 0f
                                    if (!isDismiss) break // horizontal/upward → pager or ZoomImage
                                }

                                if (isDismiss) {
                                    event.changes.forEach { it.consume() }
                                    localDismissY = (totalDelta.y * 0.7f).coerceAtLeast(0f)
                                    scope.launch { dismissY.snapTo(localDismissY) }
                                    onDragProgress((1f - localDismissY / 1500f).coerceIn(0f, 1f))
                                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                                }
                            }
                        }
                    }
                }
            }
    ) {
        CoilZoomAsyncImage(
            model              = File(imagePath),
            contentDescription = null,
            contentScale       = ContentScale.Fit,
            modifier           = Modifier.fillMaxSize(),
            zoomState          = zoomState,
            scrollBar          = null
            // onTap intentionally omitted — we handle it above with double-tap
            // disambiguation so the UI never flickers on zoom gestures
        )
    }
}