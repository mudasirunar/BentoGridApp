package com.example.bentoapp.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, imageUri: String, isBackground: Boolean, shapeIndex: Int) -> Unit,
    triggerHaptic: (String) -> Unit,
    existingName: String = "",
    existingImageUri: String = "",
    existingIsBackground: Boolean = false,
    existingShapeIndex: Int = 1,
    isEditMode: Boolean = false
) {
    var projectName by rememberSaveable { mutableStateOf(existingName) }
    var selectedImageUri by rememberSaveable { mutableStateOf(existingImageUri) }
    var setAsBackground by rememberSaveable { mutableStateOf(existingIsBackground) }
    var selectedShapeIndex by rememberSaveable { mutableStateOf(existingShapeIndex) }
    val isNameValid = projectName.isNotBlank()
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val scrollState = rememberScrollState()



    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri.toString()
        }
    }

    // --- Entry scale + fade animation ---
    var visible by remember { mutableStateOf(false) }
    val dialogScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "dialogScale"
    )
    val dialogAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "dialogAlpha"
    )
    LaunchedEffect(Unit) { visible = true }

    // --- Button press scale ---
    var buttonPressed by remember { mutableStateOf(false) }
    val buttonScale by animateFloatAsState(
        targetValue = if (buttonPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "buttonScale"
    )
    LaunchedEffect(buttonPressed) {
        if (buttonPressed) {
            kotlinx.coroutines.delay(200)
            buttonPressed = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .background(Color.Transparent)
                .padding(horizontal = 20.dp)
                .graphicsLayer {
                    scaleX = dialogScale
                    scaleY = dialogScale
                    alpha = dialogAlpha
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = screenHeight - 40.dp)
                    .clip(RoundedCornerShape(36.dp))
                    .border(
                        width = 0.5.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                            )
                        ),
                        shape = RoundedCornerShape(36.dp)
                    ),
                shape = RoundedCornerShape(36.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // --- Header ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                // Dynamic title
                                text = if (isEditMode) "Edit Collection" else "New Collection",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                // Dynamic subtitle
                                text = if (isEditMode) "Update project details" else "Set up your project",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                letterSpacing = 0.sp
                            )
                        }

                        // Close button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                                )
                                .clickable {
                                    triggerHaptic("TICK")
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✕",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // --- Cover Picker ---
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(148.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                if (selectedImageUri.isEmpty())
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                        )
                                    )
                                else
                                    Brush.linearGradient(
                                        listOf(Color.Transparent, Color.Transparent)
                                    )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                    )
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .clickable {
                                triggerHaptic("TICK")
                                pickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri.isEmpty()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Image,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Text(
                                    "Tap to add cover photo",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    letterSpacing = 0.sp
                                )
                            }
                        } else {
                            // Smart image loading: internal file path vs content URI
                            AsyncImage(
                                model = if (!selectedImageUri.startsWith("content://"))
                                    File(selectedImageUri)
                                else
                                    selectedImageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Change photo overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // Change Photo Button
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.Black.copy(alpha = 0.5f),
                                        modifier = Modifier.clickable {
                                            triggerHaptic("TICK")
                                            pickerLauncher.launch(
                                                PickVisualMediaRequest(
                                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                                )
                                            )
                                        }
                                    ) {
                                        Text(
                                            text = "Change Photo",
                                            modifier = Modifier.padding(
                                                horizontal = 14.dp,
                                                vertical = 8.dp
                                            ),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Remove Photo Button
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                        modifier = Modifier.clickable {
                                            triggerHaptic("TICK")
                                            selectedImageUri = ""
                                            setAsBackground = false
                                        }
                                    ) {
                                        Text(
                                            text = "Remove",
                                            modifier = Modifier.padding(
                                                horizontal = 14.dp,
                                                vertical = 8.dp
                                            ),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // --- Name Field ---
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            value = projectName,
                            onValueChange = { projectName = it },
                            placeholder = {
                                Text(
                                    "Collection name…",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    SectionLabelSmall("Tile Style")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val shapes = listOf("Edged", "Rounded", "Circular")
                        shapes.forEachIndexed { index, label ->
                            StyleOptionCard(
                                label = label,
                                isSelected = selectedShapeIndex == index,
                                onClick = {
                                    selectedShapeIndex = index
                                    triggerHaptic("TICK")
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Background Toggle
                    if (selectedImageUri.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    triggerHaptic("TICK")
                                    setAsBackground = !setAsBackground
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Checkbox(
                                checked = setAsBackground,
                                onCheckedChange = {
                                    triggerHaptic("TICK")
                                    setAsBackground = it
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF7C3AED),
                                    uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            )
                            Text(
                                text = "Set as collection background",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // --- Dynamic Create / Save Button ---
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .scale(buttonScale)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (isNameValid)
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF7C3AED),
                                            Color(0xFF9333EA),
                                            Color(0xFFA855F7)
                                        )
                                    )
                                else
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                        )
                                    )
                            )
                            .clickable(enabled = isNameValid) {
                                triggerHaptic("CONFIRM")
                                buttonPressed = true
                                onConfirm(projectName, selectedImageUri, setAsBackground, selectedShapeIndex)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                // Check icon for edit, Add icon for create
                                imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = null,
                                tint = if (isNameValid) Color.White
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                // Dynamic button label
                                text = if (isEditMode) "Save Changes" else "Create Collection",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                letterSpacing = 0.sp,
                                color = if (isNameValid) Color.White
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabelSmall(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 8.dp, top = 4.dp)
    )
}

@Composable
private fun StyleOptionCard(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        animationSpec = tween(300),
        label = "bg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
        else Color.Transparent,
        animationSpec = tween(300),
        label = "border"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = modifier.height(80.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Visual Silhouette of the Tile
            Box(
                modifier = Modifier
                    .size(width = 32.dp, height = 24.dp)
                    // ── THE VISUAL CHOICE ──
                    .clip(
                        when(label) {
                            "Edged" -> RoundedCornerShape(2.dp)
                            "Rounded" -> RoundedCornerShape(8.dp)
                            else -> androidx.compose.foundation.shape.CircleShape // Circular/Capsule
                        }
                    )
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}