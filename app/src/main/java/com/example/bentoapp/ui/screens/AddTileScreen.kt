package com.example.bentoapp.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bentoapp.data.BentoEntity
import com.example.bentoapp.data.TileShape
import com.example.bentoapp.ui.components.BentoTile
import com.example.bentoapp.ui.theme.BentoPalette
import com.example.bentoapp.ui.theme.TileViolet
import com.example.bentoapp.viewmodel.BentoViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AddTileScreen(
    projectId: Int,
    tileId: Int? = null,
    shapeIndex: Int,
    viewModel: BentoViewModel,
    onSave: (BentoEntity, Uri?) -> Unit,
    onBack: () -> Unit
) {
    val ColorSaver = Saver<Color, Int>(
        save = { it.toArgb() },
        restore = { Color(it) }
    )

    val isEditMode = tileId != null
    val shapeListState = rememberLazyListState()
    val gradientListState = rememberLazyListState()
    val solidListState = rememberLazyListState()
    val monoListState = rememberLazyListState()
    val textColorListState = rememberLazyListState()
    val contentColorListState = rememberLazyListState()

    val defaultInitialColor = BentoPalette.gradients.first().first()

    // ── UI STATES ──
    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    var selectedShape by rememberSaveable { mutableStateOf(TileShape.SQUARE) }
    var selectedColor by rememberSaveable(stateSaver = ColorSaver) { mutableStateOf(defaultInitialColor) }
    var alignment by rememberSaveable { mutableIntStateOf(0) }
    var isBold by rememberSaveable { mutableStateOf(false) }
    var isItalic by rememberSaveable { mutableStateOf(false) }
    var isUnderline by rememberSaveable { mutableStateOf(false) }
    var selectedTextColor by rememberSaveable(stateSaver = ColorSaver) { mutableStateOf(Color.White) }
    var textSizeOffset by rememberSaveable { mutableIntStateOf(0) }
    var isReversed by rememberSaveable { mutableStateOf(false) }
    var selectedContentTextColor by rememberSaveable(stateSaver = ColorSaver) { mutableStateOf(Color.White) }
    var isContentBold by rememberSaveable { mutableStateOf(false) }
    var isContentItalic by rememberSaveable { mutableStateOf(false) }
    var isContentUnderline by rememberSaveable { mutableStateOf(false) }
    var contentSizeOffset by rememberSaveable { mutableIntStateOf(0) }
    var selectedImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var existingImagePath by rememberSaveable { mutableStateOf<String?>(null) }

    var imageUrlInput by rememberSaveable { mutableStateOf("") }
    var isFetchingUrl by rememberSaveable { mutableStateOf(false) }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var urlFetchError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(urlFetchError) {
        if (urlFetchError != null) {
            kotlinx.coroutines.delay(3000)
            urlFetchError = null
        }
    }


    // --- ORIGINAL STATE TRACKER (For Comparison) ---
    var originalTitle by rememberSaveable { mutableStateOf("") }
    var originalContent by rememberSaveable { mutableStateOf("") }
    var originalShape by rememberSaveable { mutableStateOf(TileShape.SQUARE) }
    var originalColor by rememberSaveable(stateSaver = ColorSaver) { mutableStateOf(defaultInitialColor) }
    var originalAlignment by rememberSaveable { mutableIntStateOf(0) }
    var originalIsBold by rememberSaveable { mutableStateOf(false) }
    var originalIsItalic by rememberSaveable { mutableStateOf(false) }
    var originalIsUnderline by rememberSaveable { mutableStateOf(false) }
    var originalTextColor by rememberSaveable(stateSaver = ColorSaver) { mutableStateOf(Color.White) }
    var originalTextSizeOffset by rememberSaveable { mutableIntStateOf(0) }
    var originalIsReversed by rememberSaveable { mutableStateOf(false) }
    var originalContentTextColor by rememberSaveable(stateSaver = ColorSaver) { mutableStateOf(Color.White) }
    var originalIsContentBold by rememberSaveable { mutableStateOf(false) }
    var originalIsContentItalic by rememberSaveable { mutableStateOf(false) }
    var originalIsContentUnderline by rememberSaveable { mutableStateOf(false) }
    var originalContentSizeOffset by rememberSaveable { mutableIntStateOf(0) }
    var originalImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var originalImageUrlInput by rememberSaveable { mutableStateOf("") }


    var isInitialized by rememberSaveable { mutableStateOf(false) }
    var isCurrentSelectionGradient by rememberSaveable { mutableStateOf(true) }

    if (isEditMode) {
        val existingTile by viewModel.getTileById(tileId!!).collectAsState(initial = null)

        LaunchedEffect(existingTile) {
            if (!isInitialized && existingTile != null) {
                existingTile?.let { tile ->
                    // Set Current UI
                    title = tile.title
                    content = tile.content
                    selectedShape = tile.shape
                    selectedColor = Color(tile.backgroundColor ?: TileViolet.toArgb())
                    alignment = tile.textAlignment
                    isBold = tile.isBold
                    isItalic = tile.isItalic
                    isUnderline = tile.isUnderline
                    selectedTextColor = Color(tile.textColor)
                    textSizeOffset = tile.textSizeOffset
                    isReversed = tile.isReversed
                    selectedContentTextColor = Color(tile.contentTextColor)
                    isContentBold = tile.isContentBold
                    isContentItalic = tile.isContentItalic
                    isContentUnderline = tile.isContentUnderline
                    contentSizeOffset = tile.contentSizeOffset
                    existingImagePath = tile.imageUri
                    imageUrlInput = tile.originalImageUrl ?: ""
                    val imageUri = if (!tile.imageUri.isNullOrEmpty()) Uri.fromFile(File(tile.imageUri)) else null
                    selectedImageUri = imageUri

                    // Set Originals
                    originalTitle = tile.title
                    originalContent = tile.content
                    originalShape = tile.shape
                    originalColor = Color(tile.backgroundColor ?: TileViolet.toArgb())
                    originalAlignment = tile.textAlignment
                    originalIsBold = tile.isBold
                    originalIsItalic = tile.isItalic
                    originalIsUnderline = tile.isUnderline
                    originalTextColor = Color(tile.textColor)
                    originalTextSizeOffset = tile.textSizeOffset
                    originalIsReversed = tile.isReversed
                    originalContentTextColor = Color(tile.contentTextColor)
                    originalIsContentBold = tile.isContentBold
                    originalIsContentItalic = tile.isContentItalic
                    originalIsContentUnderline = tile.isContentUnderline
                    originalContentSizeOffset = tile.contentSizeOffset
                    originalImageUri = imageUri
                    originalImageUrlInput = tile.originalImageUrl ?: ""

                    isCurrentSelectionGradient = BentoPalette.isGradient(tile.backgroundColor ?: 0)

                    kotlinx.coroutines.delay(100)

                    // 1. Scroll Shape (Existing logic)
                    shapeListState.centerItem(tile.shapeIndex)

                    // 2. Scroll main Color Category Rows
                    // Find index in each list
                    val gradIndex = BentoPalette.gradients.indexOfFirst { it.first().toArgb() == tile.backgroundColor }
                    val solidIndex = BentoPalette.vibrantSolids.indexOfFirst { it.toArgb() == tile.backgroundColor }
                    val monoIndex = BentoPalette.monochrome.indexOfFirst { it.toArgb() == tile.backgroundColor }

                    if (gradIndex != -1) gradientListState.centerItem(gradIndex)
                    if (solidIndex != -1) solidListState.centerItem(solidIndex)
                    if (monoIndex != -1) monoListState.centerItem(monoIndex)

                    // 3. Scroll Text Color Row
                    val textColors = listOf(Color.White, Color.Black) + BentoPalette.vibrantSolids +
                            BentoPalette.monochrome.filter { it != Color.White && it != Color.Black }
                    val textColorIndex = textColors.indexOfFirst { it.toArgb() == tile.textColor }
                    if (textColorIndex != -1) textColorListState.centerItem(textColorIndex)

                    val contentColorIdx = textColors.indexOfFirst { it.toArgb() == tile.contentTextColor }
                    if (contentColorIdx != -1) contentColorListState.centerItem(contentColorIdx)

                    isInitialized = true
                }
            }
        }
    }

    val hasChanges = remember(
        // ── ALL STATE KEYS ──
        title, content, selectedShape, selectedColor, alignment,
        selectedTextColor, isBold, isItalic, isUnderline, textSizeOffset,
        isReversed, selectedImageUri,
        selectedContentTextColor, isContentBold, isContentItalic,
        isContentUnderline, contentSizeOffset,

        // ── ALL ORIGINAL KEYS ──
        originalTitle, originalContent, originalShape, originalColor, originalAlignment,
        originalTextColor, originalIsBold, originalIsItalic, originalIsUnderline, originalTextSizeOffset,
        originalIsReversed, originalImageUri,
        originalContentTextColor, originalIsContentBold, originalIsContentItalic,
        originalIsContentUnderline, originalContentSizeOffset
    ) {
        title != originalTitle ||
                content != originalContent ||
                selectedShape != originalShape ||
                selectedColor != originalColor ||
                alignment != originalAlignment ||
                selectedTextColor != originalTextColor ||
                isBold != originalIsBold ||
                isItalic != originalIsItalic ||
                isUnderline != originalIsUnderline ||
                textSizeOffset != originalTextSizeOffset ||
                isReversed != originalIsReversed ||
                selectedImageUri?.toString() != originalImageUri?.toString() ||
                selectedContentTextColor != originalContentTextColor ||
                isContentBold != originalIsContentBold ||
                isContentItalic != originalIsContentItalic ||
                isContentUnderline != originalIsContentUnderline ||
                contentSizeOffset != originalContentSizeOffset
    }

    // New logic: Always allow in Add Mode. In Edit Mode, require changes.
    val canSave = if (!isEditMode) true else hasChanges

    LaunchedEffect(selectedShape) {
        // 1. Clamp Title Size
        val maxTitleOffset = when {
            selectedShape == TileShape.SMALL_V -> 0
            selectedShape == TileShape.SMALL_H -> 1
            else -> 2
        }
        if (textSizeOffset > maxTitleOffset) {
            textSizeOffset = maxTitleOffset
        }

        // 2. Clamp Content Size
        val maxContentOffset = if (selectedShape.colSpan == 1) 0 else 1
        if (contentSizeOffset > maxContentOffset) {
            contentSizeOffset = maxContentOffset
        }
    }

    var showDiscardSheet by remember { mutableStateOf(false) }
    BackHandler(enabled = true) {
        if (!isFetchingUrl && !isSaving) {
            if (hasChanges) {
                showDiscardSheet = true
            } else {
                onBack()
            }
        }
    }

    // --- HAPTIC ENGINE ---
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
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
    // ── DYNAMIC PREVIEW MATH ──────────────────────────────────────────
    val scrollState = rememberScrollState()

    // We calculate progress directly.
    // Increase 350f to 500f if you want the movement to last longer during the scroll.
    val scrollProgress = (scrollState.value / 750f).coerceIn(0f, 1f)

    // Calculate dimensions directly from scrollProgress (No animateDpAsState)
    val baseSquareSize = lerp(150f, 130f, scrollProgress)
    val currentWidth = (baseSquareSize * (selectedShape.colSpan / 2f)).dp
    val currentHeight = (baseSquareSize * (selectedShape.heightDp / 160f)).dp

    // Calculate position directly (No animateFloatAsState)
    val horizontalBias = lerp(0f, 1f, scrollProgress)
    val verticalBias = lerp(-0.4f, -1f, scrollProgress)


    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedImageUri = it }
    }

    val finalImageUrlToSave = if (selectedImageUri == null || selectedImageUri?.toString()?.startsWith("content://") == true) "" else imageUrlInput

    val previewTile = remember(
        title, content, selectedImageUri, selectedShape, selectedColor,
        alignment, selectedTextColor, isBold, isItalic, isUnderline,
        textSizeOffset, isReversed, selectedContentTextColor,
        isContentBold, isContentItalic, isContentUnderline, contentSizeOffset, imageUrlInput
    ) {
        BentoEntity(
            projectId = projectId,
            title = title,
            content = content,
            imageUri = selectedImageUri?.toString(),
            shapeIndex = selectedShape.index,
            backgroundColor = selectedColor.toArgb(),
            textAlignment = alignment,
            textColor = selectedTextColor.toArgb(),
            isBold = isBold,
            isItalic = isItalic,
            isUnderline = isUnderline,
            textSizeOffset = textSizeOffset,
            isReversed = isReversed,
            contentTextColor = selectedContentTextColor.toArgb(),
            isContentBold = isContentBold,
            isContentItalic = isContentItalic,
            isContentUnderline = isContentUnderline,
            contentSizeOffset = contentSizeOffset,
            originalImageUrl = finalImageUrlToSave
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    onClick = {
                        if (!isFetchingUrl && !isSaving) {
                            triggerHaptic("TICK")
                            if (hasChanges) showDiscardSheet = true else onBack()
                        }
                    },
                    enabled = !isFetchingUrl && !isSaving,
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = if (isFetchingUrl || isSaving) 0.dp else 4.dp,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isFetchingUrl || isSaving)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            else
                                MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = if (isEditMode) "Edit Tile" else "New Tile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (canSave)
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF7C3AED), Color(0xFF9333EA), Color(0xFFA855F7))
                                )
                            else
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                    )
                                )
                        )
                        .clickable(enabled = canSave && !isFetchingUrl && !isSaving) {
                            triggerHaptic("CONFIRM")
                            isSaving = true
                            onSave(
                                BentoEntity(
                                    id = tileId ?: 0,
                                    projectId = projectId,
                                    title = title,
                                    content = content,
                                    imageUri = existingImagePath,
                                    shapeIndex = selectedShape.index,
                                    backgroundColor = selectedColor.toArgb(),
                                    textAlignment = alignment,
                                    textColor = selectedTextColor.toArgb(),
                                    isBold = isBold,
                                    isItalic = isItalic,
                                    isUnderline = isUnderline,
                                    textSizeOffset = textSizeOffset,
                                    isReversed = isReversed,
                                    contentTextColor = selectedContentTextColor.toArgb(),
                                    isContentBold = isContentBold,
                                    isContentItalic = isContentItalic,
                                    isContentUnderline = isContentUnderline,
                                    contentSizeOffset = contentSizeOffset,
                                    originalImageUrl = finalImageUrlToSave
                                    ),
                                    selectedImageUri
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            // Check icon for edit, Add icon for new - matching your dialog
                            imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null,
                            tint = if (canSave) Color.White
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isEditMode) "Save Changes" else "Add to Collection",
                            color = if (canSave) Color.White
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge,
                            letterSpacing = 0.sp
                        )
                    }
                }
            }
        }
    ) { padding ->

        // ── Root Box — lets floating preview overlay the scroll content ───
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Scrollable form ───────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.fillMaxWidth().height(260.dp))

                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    // ── SHAPE SELECTOR ────────────────────────────────────────
                    SectionLabel(
                        text = "Shape"
                    )
                    Spacer(Modifier.height(12.dp))
                    ShapeSelector(
                        selectedShape = selectedShape,
                        state = shapeListState,
                        shapeIndex = shapeIndex,
                        onShapeSelected = { selectedShape = it },
                        triggerHaptic = triggerHaptic
                    )

                    Spacer(Modifier.height(28.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // ── CONTENT ───────────────────────────────────────────
                        SectionLabel("Tile Details")

                        OutlinedTextField(
                            value = title,
                            onValueChange = { input ->
                                title = input.split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                            },
                            label = { Text("Tile Title") },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            maxLines = 2
                        )

                        // ── TEXT CONTROLS ──
                        TextControls(
                            alignment = alignment,
                            isBold = isBold,
                            isItalic = isItalic,
                            isUnderline = isUnderline,
                            selectedTextColor = selectedTextColor,
                            textColorState = textColorListState,
                            textSizeOffset = textSizeOffset,
                            selectedShape = selectedShape,
                            isReversed = isReversed,
                            onReverseToggle = { isReversed = !isReversed },
                            onAlignmentChange = { alignment = it },
                            onBoldToggle = { isBold = !isBold },
                            onItalicToggle = { isItalic = !isItalic },
                            onUnderlineToggle = { isUnderline = !isUnderline },
                            onTextColorChange = { selectedTextColor = it },
                            onSizeChange = { textSizeOffset = it },
                            triggerHaptic = triggerHaptic
                        )

                        OutlinedTextField(
                            value = content,
                            onValueChange = { input ->
                                content = input.replaceFirstChar { it.uppercase() }
                            },
                            label = { Text("Content") },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 2,
                            shape = RoundedCornerShape(16.dp),
                            placeholder = { Text("Add text over your tile...") }
                        )

                        //Spacer(Modifier.height(12.dp))
                        ContentStyleRow(
                            isBold = isContentBold,
                            isItalic = isContentItalic,
                            isUnderline = isContentUnderline,
                            sizeOffset = contentSizeOffset,
                            selectedShape = selectedShape,
                            selectedTextColor = selectedContentTextColor,
                            textColorState = contentColorListState,
                            onBoldToggle = { isContentBold = !isContentBold },
                            onItalicToggle = { isContentItalic = !isContentItalic },
                            onUnderlineToggle = { isContentUnderline = !isContentUnderline },
                            onSizeChange = { contentSizeOffset = it },
                            onTextColorChange = { selectedContentTextColor = it },
                            triggerHaptic = triggerHaptic
                        )

                        // ── IMAGE PICKER SECTION ──────────────────────────────────────
                        SectionLabel("Image")

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. The Pick/Change Button
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .clickable {
                                        triggerHaptic("TICK")
                                        launcher.launch("image/*")
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (selectedImageUri == null) "Pick Image" else "Change Image",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // 2. The Remove Button
                            if (selectedImageUri != null) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                                    modifier = Modifier
                                        .height(52.dp)
                                        .clickable {
                                            triggerHaptic("TICK")
                                            selectedImageUri = null
                                            scope.launch {
                                                kotlinx.coroutines.delay(150)
                                                scrollState.animateScrollBy(
                                                    value = 900f,
                                                    animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing)
                                                )
                                            }
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Remove",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }

                        // ── URL FETCH SECTION ─────────────────────────────────────────
                        Spacer(Modifier.height(12.dp))

                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionLabel("Web Image")
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                OutlinedTextField(
                                    value = imageUrlInput,
                                    onValueChange = { imageUrlInput = it },
                                    placeholder = { Text("Enter image URL...") },
                                    enabled = !isFetchingUrl,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true,
                                    trailingIcon = {
                                        if (imageUrlInput.isNotEmpty()) {
                                            androidx.compose.material3.IconButton(
                                                onClick = {
                                                    triggerHaptic("TICK")
                                                    imageUrlInput = ""
                                                },
                                                enabled = !isFetchingUrl
                                            ) {
                                                Icon(
                                                    imageVector = androidx.compose.material.icons.Icons.Default.Clear,
                                                    contentDescription = "Clear URL"
                                                )
                                            }
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                        onDone = {
                                            focusManager.clearFocus()
                                        }
                                    )
                                )

                                Button(
                                    onClick = {
                                        if (imageUrlInput.isNotBlank()) {
                                            triggerHaptic("TICK")
                                            isFetchingUrl = true
                                            urlFetchError = null
                                            focusManager.clearFocus()

                                            scope.launch {
                                                val result = viewModel.downloadImageFromUrl(context, imageUrlInput)
                                                if (result.isSuccess) {
                                                    selectedImageUri = Uri.parse(result.getOrNull())
                                                } else {
                                                    urlFetchError = result.exceptionOrNull()?.message ?: "Failed to fetch image"
                                                }
                                                isFetchingUrl = false
                                            }
                                        }
                                    },
                                    enabled = !isFetchingUrl && imageUrlInput.isNotBlank(),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.height(56.dp)
                                ) {
                                    if (isFetchingUrl) {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("Fetch")
                                    }
                                }
                            }

                            // Error Tooltip Overlay
                            androidx.compose.animation.AnimatedVisibility(
                                visible = urlFetchError != null,
                                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { -it / 2 }),
                                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { -it / 2 }),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 64.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shadowElevation = 4.dp
                                ) {
                                    Text(
                                        text = urlFetchError ?: "",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                        }

                        // ── DYNAMIC COLOUR PICKER ─────────────────────────────────────
                        AnimatedVisibility(
                            visible = selectedImageUri == null,
                            enter = fadeIn(
                                animationSpec = tween(600, delayMillis = 300)
                            ) + androidx.compose.animation.expandVertically(
                                animationSpec = tween(700, delayMillis = 300),
                                expandFrom = Alignment.Top // The "Falling" effect
                            ),
                            exit = fadeOut(
                                animationSpec = tween(durationMillis = 400, delayMillis = 300)
                            ) + androidx.compose.animation.shrinkVertically(
                                animationSpec = tween(durationMillis = 400, delayMillis = 300),
                                shrinkTowards = Alignment.Top
                            )
                        ) {
                            Column {
                                Spacer(Modifier.height(24.dp))
                                SectionLabel("Colour")
                                // ColorPicker already contains the 3 rows
                                ColorPicker(
                                    selectedColor = selectedColor,
                                    isGradientSelection = isCurrentSelectionGradient,
                                    gradientState = gradientListState,
                                    solidState = solidListState,
                                    monoState = monoListState,
                                    triggerHaptic = triggerHaptic,
                                    onColorSelect = { color, isGrad ->
                                        selectedColor = color
                                        isCurrentSelectionGradient = isGrad
                                    }
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
            // ── Floating live preview ───────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, start = 20.dp, end = 20.dp)
                    .height(240.dp),
                contentAlignment = BiasAlignment(horizontalBias, verticalBias)
            ) {
                Box(
                    modifier = Modifier
                        .width(currentWidth)  // Direct sync width
                        .height(currentHeight) // Direct sync height
                        .shadow(
                            elevation = lerp(8f, 16f, scrollProgress).dp,
                            shape = RoundedCornerShape(24.dp),
                            spotColor = selectedColor.copy(alpha = 0.4f),
                            clip = false
                        )
                ) {
                    BentoTile(tile = previewTile, shapeIndex = shapeIndex)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp))
                            .clickable(
                                // interactionSource/indication = null removes the gray ripple
                                // making it feel like a "reset gesture" rather than a button
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) {
                                scope.launch {
                                    scrollState.animateScrollTo(
                                        value = 0,
                                        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
                                    )
                                }
                            }
                    )
                }
            }
        }
    }
    // ── DISCARD CHANGES BOTTOM SHEET ──
    if (showDiscardSheet) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .graphicsLayer { translationY = 0f },
            contentAlignment = Alignment.BottomCenter
        ) {
            // Invisible click layer to dismiss (acts as Cancel)
            Box(modifier = Modifier.fillMaxSize().clickable { showDiscardSheet = false })

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
                    // Warning Icon
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = "Discard Changes?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = if (isEditMode)
                            "You have unsaved edits to this tile. Leaving will revert all changes."
                        else
                            "You haven't added this tile to your collection yet. Discard progress?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(Modifier.height(28.dp))

                    // DISCARD BUTTON
                    Button(
                        onClick = {
                            triggerHaptic("CONFIRM")
                            onBack()
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            "Discard and Leave",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // CANCEL BUTTON
                    androidx.compose.material3.TextButton(
                        onClick = {
                            triggerHaptic("TICK")
                            showDiscardSheet = false
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            "Keep Editing",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

// ── Text Controls — alignment  ──────────────
@Composable
private fun TextControls(
    alignment: Int,
    isBold: Boolean,
    isItalic: Boolean,
    isUnderline: Boolean,
    selectedTextColor: Color,
    textColorState: LazyListState,
    textSizeOffset: Int,
    selectedShape: TileShape,
    onAlignmentChange: (Int) -> Unit,
    onBoldToggle: () -> Unit,
    onItalicToggle: () -> Unit,
    onUnderlineToggle: () -> Unit,
    isReversed: Boolean,
    onReverseToggle: () -> Unit,
    onTextColorChange: (Color) -> Unit,
    onSizeChange: (Int) -> Unit,
    triggerHaptic: (String) -> Unit
) {
    val textColors = remember {
        val remainingMono = BentoPalette.monochrome.filter { it != Color.White && it != Color.Black }
        listOf(Color.White, Color.Black) + BentoPalette.vibrantSolids + remainingMono
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "TITLE STYLE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            letterSpacing = 1.sp
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .graphicsLayer(clip = false),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(start = 6.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
        ) {
            // GROUP 1: ALIGNMENT
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextStylingToggles(Icons.Default.FormatAlignLeft, alignment == 0, { onAlignmentChange(0) }, triggerHaptic)
                    TextStylingToggles(Icons.Default.FormatAlignCenter, alignment == 1, { onAlignmentChange(1) }, triggerHaptic)
                    TextStylingToggles(Icons.Default.FormatAlignRight, alignment == 2, { onAlignmentChange(2) }, triggerHaptic)
                }
            }

            // DIVIDER
            item {
                Box(Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))
            }

            // GROUP 2: FORMATTING & REVERSE
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextStylingToggles(Icons.Default.FormatBold, isBold, onBoldToggle, triggerHaptic)
                    TextStylingToggles(Icons.Default.FormatItalic, isItalic, onItalicToggle, triggerHaptic)
                    TextStylingToggles(Icons.Default.FormatUnderlined, isUnderline, onUnderlineToggle, triggerHaptic)
                    TextStylingToggles(Icons.Default.SwapVert, isReversed, onReverseToggle, triggerHaptic)
                }
            }

            // DIVIDER
            item {
                Box(Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))
            }

            // GROUP 3: SIZE PICKER
            item {
                val sizeOptions = when {
                    selectedShape == TileShape.SMALL_V -> listOf(-1, 0)
                    selectedShape == TileShape.SMALL_H -> listOf(-1, 0, 1)
                    else -> listOf(-1, 0, 1, 2)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sizeOptions.forEach { offset ->
                        val visualSize = when(offset) {
                            -1 -> 14.dp
                            0 -> 18.dp
                            1 -> 22.dp
                            else -> 26.dp
                        }
                        TextStylingToggles(
                            icon = Icons.Default.FormatSize,
                            isSelected = textSizeOffset == offset,
                            onClick = { onSizeChange(offset) },
                            iconSize = visualSize,
                            triggerHaptic = triggerHaptic
                        )
                    }
                }
            }
        }

        // TEXT COLOR SELECTOR
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LazyRow(
                state = textColorState,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(textColors) { color ->
                    TextColorCircle(
                        color = color,
                        isSelected = selectedTextColor == color,
                        onClick = { onTextColorChange(color) },
                        triggerHaptic = triggerHaptic
                    )
                }
            }
        }
    }
}

@Composable
private fun TextColorCircle(
    color: Color,
    isSelected: Boolean,
    triggerHaptic: (String) -> Unit,
    onClick: () -> Unit
) {
    // Exact sizing and spring specs from your SolidCircle reference
    val size by animateDpAsState(
        targetValue = if (isSelected) 40.dp else 32.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "size"
    )

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                // Using onBackground ensures a white ring on dark colors and vice versa
                color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                shape = CircleShape
            )
            .clickable {
                triggerHaptic("TICK")
                onClick() }
    )
}


@Composable
private fun TextStylingToggles(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    triggerHaptic: (String) -> Unit,
    iconSize: Dp = 18.dp
) {
    val bgAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.15f else 0f,
        animationSpec = tween(180),
        label = "buttonBg"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "buttonScale"
    )

    Box(
        modifier = Modifier
            .size(42.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = bgAlpha))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable {
                triggerHaptic("TICK")
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun ContentStyleRow(
    isBold: Boolean,
    isItalic: Boolean,
    isUnderline: Boolean,
    sizeOffset: Int,
    selectedShape: TileShape,
    selectedTextColor: Color,
    textColorState: LazyListState,
    onBoldToggle: () -> Unit,
    onItalicToggle: () -> Unit,
    onUnderlineToggle: () -> Unit,
    onSizeChange: (Int) -> Unit,
    onTextColorChange: (Color) -> Unit,
    triggerHaptic: (String) -> Unit
) {
    val textColors = remember {
        val remainingMono = BentoPalette.monochrome.filter { it != Color.White && it != Color.Black }
        listOf(Color.White, Color.Black) + BentoPalette.vibrantSolids + remainingMono
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "CONTENT STYLE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )

        // 1. STYLE TOGGLES
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .graphicsLayer(clip = false),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(start = 6.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextStylingToggles(Icons.Default.FormatBold, isBold, onBoldToggle, triggerHaptic)
                    TextStylingToggles(Icons.Default.FormatItalic, isItalic, onItalicToggle, triggerHaptic)
                    TextStylingToggles(Icons.Default.FormatUnderlined, isUnderline, onUnderlineToggle, triggerHaptic)
                }
            }
            item { Box(Modifier.width(1.dp).height(20.dp).background(MaterialTheme.colorScheme.outlineVariant)) }
            item {
                val sizeOptions = if (selectedShape.colSpan == 1) listOf(-1, 0) else listOf(-1, 0, 1)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sizeOptions.forEach { offset ->
                        TextStylingToggles(
                            icon = Icons.Default.FormatSize,
                            isSelected = sizeOffset == offset,
                            onClick = { onSizeChange(offset) },
                            iconSize = when(offset) {
                                -1 -> 14.dp
                                0 -> 18.dp
                                else -> 22.dp
                            },
                            triggerHaptic = triggerHaptic
                        )
                    }
                }
            }
        }

        // 2. COLOR SELECTOR
        LazyRow(
            state = textColorState,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(textColors) { color ->
                TextColorCircle(
                    color = color,
                    isSelected = selectedTextColor == color,
                    onClick = { onTextColorChange(color) },
                    triggerHaptic = triggerHaptic
                )
            }
        }
    }
}

// ── Shape Selector Banner ─────────────────────────────────────────────────────
@Composable
private fun ShapeSelector(
    selectedShape: TileShape,
    state: LazyListState,
    shapeIndex: Int,
    onShapeSelected: (TileShape) -> Unit,
    triggerHaptic: (String) -> Unit
) {
    LazyRow(
        state = state,
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(TileShape.entries) { shape ->
            ShapeCard(
                shape = shape,
                shapeIndex = shapeIndex,
                isSelected = shape == selectedShape,
                triggerHaptic = triggerHaptic,
                onClick = {
                    onShapeSelected(shape)
                }
            )
        }
    }
}

@Composable
private fun ShapeCard(
    shape: TileShape,
    shapeIndex: Int,
    isSelected: Boolean,
    triggerHaptic: (String) -> Unit,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "shapeScale"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.5.dp else 0.dp,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "borderWidth"
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.45f,
        animationSpec = tween(200),
        label = "labelAlpha"
    )

    val silhouetteRadius = when(shapeIndex) {
        0 -> 2.dp     // Edged
        1 -> 8.dp     // Rounded
        else -> 50.dp // Circular/Capsule
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .width(110.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
    ) {
        Box(
            modifier = Modifier
                .height(110.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .then(
                    if (borderWidth > 0.dp)
                        Modifier.border(borderWidth, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp))
                    else Modifier
                )
                .clickable {
                    triggerHaptic("TICK")
                    onClick() },
            contentAlignment = Alignment.Center
        ) {
            // Silhouette — use previewWidthDp/previewHeightDp from TileShape enum
            Box(
                modifier = Modifier
                    .size(
                        width  = shape.previewWidthDp.dp,
                        height = shape.previewHeightDp.dp
                    )
                    .clip(RoundedCornerShape(silhouetteRadius))
                    .background(
                        if (isSelected)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                    )
            )
        }

        Text(
            text = shape.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = labelAlpha),
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun ColorPicker(
    selectedColor: Color,
    isGradientSelection: Boolean,
    gradientState: LazyListState,
    solidState: LazyListState,
    monoState: LazyListState,
    triggerHaptic: (String) -> Unit,
    onColorSelect: (Color, Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {

        // --- GRADIENTS ---
        ColorCategoryRow(
            label = "Premium Gradients",
            state = gradientState,
            isGradient = true,
            gradientList = BentoPalette.gradients, // Pull from Palette
            selectedColor = selectedColor,
            isCurrentSelectionGradient = isGradientSelection,
            isFamilySelected = isGradientSelection,
            onColorSelect = { color -> onColorSelect(color, true) },
            triggerHaptic = triggerHaptic
        )

        val isVibrantSelected = !isGradientSelection &&
                BentoPalette.vibrantSolids.contains(selectedColor)
        // --- VIBRANT SOLIDS ---
        ColorCategoryRow(
            label = "Vibrant Solids",
            state = solidState,
            colors = BentoPalette.vibrantSolids,
            selectedColor = selectedColor,
            isCurrentSelectionGradient = isGradientSelection,
            isFamilySelected = isVibrantSelected,
            onColorSelect = { color -> onColorSelect(color, false) },
            triggerHaptic = triggerHaptic
        )

        // 3. Check if the current selection is in the Monochrome list
        val isMonoSelected = !isGradientSelection &&
                BentoPalette.monochrome.contains(selectedColor)
        // --- MONOCHROME ---
        ColorCategoryRow(
            label = "Monochrome & Depth",
            state = monoState,
            colors = BentoPalette.monochrome,
            selectedColor = selectedColor,
            isCurrentSelectionGradient = isGradientSelection,
            isFamilySelected = isMonoSelected,
            onColorSelect = { color -> onColorSelect(color, false) },
            triggerHaptic = triggerHaptic
        )
    }
}
@Composable
private fun ColorCategoryRow(
    label: String,
    state: LazyListState,
    colors: List<Color> = emptyList(),
    isGradient: Boolean = false,
    gradientList: List<List<Color>> = emptyList(),
    selectedColor: Color,
    isCurrentSelectionGradient: Boolean,
    isFamilySelected: Boolean,
    onColorSelect: (Color) -> Unit,
    triggerHaptic: (String) -> Unit
) {
    val labelColor by animateColorAsState(
        targetValue = if (isFamilySelected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
        animationSpec = tween(400),
        label = "labelColor"
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isFamilySelected) FontWeight.ExtraBold else FontWeight.Black,
            color = labelColor,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        LazyRow(
            state = state,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            if (isGradient) {
                items(gradientList) { grad ->
                    val isSelected = isCurrentSelectionGradient && (selectedColor == grad.first())
                    GradientCircle(grad, isSelected, triggerHaptic) { onColorSelect(grad.first()) }
                }
            } else {
                items(colors) { color ->
                    val isSelected = !isCurrentSelectionGradient && (selectedColor == color)
                    SolidCircle(color, isSelected, triggerHaptic) { onColorSelect(color) }
                }
            }
        }
    }
}

@Composable
private fun SolidCircle(
    color: Color,
    isSelected: Boolean,
    triggerHaptic: (String) -> Unit,
    onClick: () -> Unit
){
    val size by animateDpAsState(if (isSelected) 50.dp else 42.dp, label = "size")
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                shape = CircleShape
            )
            .clickable {
                triggerHaptic("TICK")
                onClick() }
    )
}

@Composable
private fun GradientCircle(
    colors: List<Color>,
    isSelected: Boolean,
    triggerHaptic: (String) -> Unit,
    onClick: () -> Unit) {
    val size by animateDpAsState(if (isSelected) 54.dp else 46.dp, label = "size")
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(colors))
            .border(
                width = if (isSelected) 3.5.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                shape = CircleShape
            )
            .clickable {
                triggerHaptic("TICK")
                onClick() }
    )
}

// ── Section label ─────────────────────────────────────────────────────────────
@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.2.sp,
        modifier = modifier
    )
}

// Extension to center items in a LazyRow
private suspend fun LazyListState.centerItem(index: Int) {
    this.scrollToItem(index, scrollOffset = -365)
}

// Simple linear interpolation helper
fun lerp(start: Float, stop: Float, fraction: Float): Float =
    (1 - fraction) * start + fraction * stop
