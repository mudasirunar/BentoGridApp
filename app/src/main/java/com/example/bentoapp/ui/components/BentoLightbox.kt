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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import java.io.FileOutputStream
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
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

@Composable
fun BentoLightbox(
    titles: List<String>,
    imagePaths: List<String>,
    initialIndex: Int,
    initialStatusBarVisible: Boolean,
    onUiVisibilityChange: (Boolean) -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onDismiss: () -> Unit,
    isGalleryMode: Boolean = false
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape   = configuration.orientation ==
            android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val context = androidx.compose.ui.platform.LocalContext.current

    var controlsVisible by rememberSaveable { mutableStateOf(initialStatusBarVisible) }

    LaunchedEffect(controlsVisible) {
        onUiVisibilityChange(controlsVisible)
    }

    var isZoomed by remember { mutableStateOf(false) }

    val view   = LocalView.current
    val window = remember(view) {
        var foundWindow: android.view.Window? = null

        var currentParent = view.parent
        while (currentParent != null) {
            if (currentParent is androidx.compose.ui.window.DialogWindowProvider) {
                foundWindow = currentParent.window
                break
            }
            currentParent = currentParent.parent
        }

        if (foundWindow == null) {
            var context = view.context
            while (context is android.content.ContextWrapper) {
                if (context is android.app.Activity) {
                    foundWindow = context.window
                    break
                }
                context = context.baseContext
            }
        }
        foundWindow
    }

    var isVisible by rememberSaveable { mutableStateOf(false) }
    val pagerState = rememberPagerState(initialPage = initialIndex) { imagePaths.size }
    var globalDragAlpha by remember { mutableFloatStateOf(1f) }
    val currentTitle = titles.getOrNull(pagerState.currentPage) ?: ""

    val scope = rememberCoroutineScope()
    val deleteTranslateX = remember { Animatable(0f) }
    val deleteAlpha      = remember { Animatable(1f) }

    val handleDelete: () -> Unit = {
        val currentPage = pagerState.currentPage
        val direction = if (currentPage < imagePaths.size - 1) -1f else 1f
        scope.launch {
            coroutineScope {
                launch { deleteTranslateX.animateTo(direction * 500f, tween(220, easing = FastOutSlowInEasing)) }
                launch { deleteAlpha.animateTo(0f, tween(180)) }
            }
            onDelete(currentPage)
            deleteTranslateX.snapTo(0f)
            deleteAlpha.snapTo(1f)
        }
    }

    LaunchedEffect(Unit) { isVisible = true }

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    DisposableEffect(controlsVisible, isLandscape, isDarkTheme) {
        window?.let { win ->
            win.statusBarColor = android.graphics.Color.TRANSPARENT
            win.navigationBarColor = android.graphics.Color.TRANSPARENT
            val controller = WindowCompat.getInsetsController(win, view)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.isAppearanceLightStatusBars = !isDarkTheme
            controller.isAppearanceLightNavigationBars = !isDarkTheme

            if (controlsVisible) {
                controller.show(WindowInsetsCompat.Type.statusBars())
            } else {
                controller.hide(WindowInsetsCompat.Type.statusBars())
            }
        }
        onDispose {
            window?.let { win ->
                val controller = WindowCompat.getInsetsController(win, view)
                if (initialStatusBarVisible) {
                    controller.show(WindowInsetsCompat.Type.statusBars())
                } else {
                    controller.hide(WindowInsetsCompat.Type.statusBars())
                }
            }
        }
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
                .background(
                    if (isGalleryMode) MaterialTheme.colorScheme.background.copy(alpha = globalDragAlpha)
                    else Color.Black.copy(alpha = 0.90f * globalDragAlpha)
                )
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

            if (!isGalleryMode) {
                // Menu controls overlay animation
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn() + slideInVertically { -it },
                    exit = fadeOut() + slideOutVertically { -it },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Box(modifier = Modifier.statusBarsPadding().padding(16.dp)) {
                        LightboxTopControls(
                            onEdit = { onEdit(pagerState.currentPage) },
                            onShare = {
                                val currentPath = imagePaths[pagerState.currentPage]
                                shareImage(context, currentPath, scope)
                            },
                            onDelete = { handleDelete() }
                        )
                    }
                }

                if (!isLandscape) {
                    AnimatedVisibility(
                        visible = controlsVisible,
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it },
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
                            CounterChip(
                                current = pagerState.currentPage + 1,
                                total = imagePaths.size
                            )
                            TitleLabel(title = displayTitle, isLandscape = false)
                        }
                    }
                } else {
                    AnimatedVisibility(
                        visible = controlsVisible,
                        enter = fadeIn() + slideInHorizontally { it },
                        exit = fadeOut() + slideOutHorizontally { it },
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
                            CounterChip(
                                current = pagerState.currentPage + 1,
                                total = imagePaths.size
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UI components
// ─────────────────────────────────────────────────────────────────────────────

private fun shareImage(context: Context, imagePath: String, coroutineScope: kotlinx.coroutines.CoroutineScope) {
    coroutineScope.launch(Dispatchers.IO) {
        try {
            val file = java.io.File(imagePath)
            val sharedFile = if (file.exists() && file.length() > 0) {
                file
            } else if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                val loader = coil.ImageLoader(context)
                val request = coil.request.ImageRequest.Builder(context)
                    .data(imagePath)
                    .allowHardware(false)
                    .build()
                val result = loader.execute(request)
                if (result is coil.request.SuccessResult) {
                    val drawable = result.drawable
                    if (drawable is android.graphics.drawable.BitmapDrawable) {
                        val bitmap = drawable.bitmap
                        val cacheFile = java.io.File(context.cacheDir, "shared_image_${System.currentTimeMillis()}.png")
                        java.io.FileOutputStream(cacheFile).use { out ->
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                        }
                        cacheFile
                    } else {
                        null
                    }
                } else {
                    null
                }
            } else {
                null
            }

            if (imagePath.startsWith("content://")) {
                val uri = android.net.Uri.parse(imagePath)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Share Image")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } else if (sharedFile != null && sharedFile.exists()) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    sharedFile
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Share Image")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
private fun LightboxTopControls(
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = { menuExpanded = true },
            color = Color.White.copy(alpha = 0.12f),
            shape = CircleShape,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            offset = androidx.compose.ui.unit.DpOffset(0.dp, 8.dp),
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White.copy(alpha = 0.25f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            modifier = Modifier.widthIn(min = 160.dp)
        ) {
            DropdownMenuItem(
                text = { Text("Edit", fontWeight = FontWeight.Bold, color = Color.White) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF00B2FF)) },
                onClick = {
                    menuExpanded = false
                    onEdit()
                }
            )
            DropdownMenuItem(
                text = { Text("Share", fontWeight = FontWeight.Bold, color = Color.White) },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF10B981)) },
                onClick = {
                    menuExpanded = false
                    onShare()
                }
            )
            DropdownMenuItem(
                text = { Text("Delete", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFDC2626)) },
                onClick = {
                    menuExpanded = false
                    onDelete()
                }
            )
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

    val isUserZoomed = zoomState.zoomable.userTransform.scaleX > 1.01f
    LaunchedEffect(isUserZoomed) { onZoomChanged(isUserZoomed) }

    val dismissY   = remember { Animatable(0f) }
    var physicsJob : Job? = remember { null }
    var singleTapJob: Job? = remember { null }

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
                        val downEvent  = awaitPointerEvent(PointerEventPass.Initial)
                        val downChange = downEvent.changes.firstOrNull { it.pressed }
                            ?: return@awaitEachGesture
                        val startPos   = downChange.position

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

                        while (true) {
                            val event  = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == downChange.id }

                            if (change == null || !change.pressed) {
                                when {
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

                            if (!currentlyZoomed) {
                                if (!directionResolved && totalDelta.getDistance() > touchSlop) {
                                    directionResolved = true
                                    isDismiss = abs(totalDelta.y) > abs(totalDelta.x) && totalDelta.y > 0f
                                    if (!isDismiss) break
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
        )
    }
}