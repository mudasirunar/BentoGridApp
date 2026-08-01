package com.example.bentoapp.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bentoapp.data.ProjectEntity
import com.example.bentoapp.data.ProjectCounts
import com.example.bentoapp.ui.components.BentoEmptyAnimation
import com.example.bentoapp.ui.components.ProjectCard

@Composable
fun CollectionsScreen(
    visibleProjects: List<ProjectEntity>,
    projectCounts: Map<Int, ProjectCounts>,
    searchQuery: String,
    isSearchActive: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onProjectClick: (ProjectEntity) -> Unit,
    onProjectDeleteRequest: (ProjectEntity) -> Unit,
    onProjectEditRequest: (ProjectEntity) -> Unit,
    onToggleLockRequest: ((ProjectEntity) -> Unit)? = null,
    triggerHaptic: (String) -> Unit,
    bottomBarHeight: Dp,
    listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    val isScrolled = remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
                    listState.firstVisibleItemScrollOffset > 10
        }
    }

    // Dynamic variable to hold the Top Bar's exact height
    val density = LocalDensity.current
    var topBarHeight by remember { mutableStateOf(110.dp) } // 110dp fallback

    // 1. Back to Box so we get the glassy transparency effect!
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = if (visibleProjects.isEmpty() && !isSearchActive) "empty"
            else if (isSearchActive && visibleProjects.isEmpty()) "no_results"
            else "content",
            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
            label = "collectionsState",
            modifier = Modifier.fillMaxSize()
        ) { state ->
            when (state) {
                "empty" -> EmptyShelfView(
                    modifier = Modifier.fillMaxSize()
                )

                "no_results" -> NoResultsFoundView(
                    searchQuery = searchQuery,
                    modifier = Modifier.fillMaxSize()
                )

                "content" -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = topBarHeight + 8.dp,
                        bottom = bottomBarHeight + 60.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(
                        items = visibleProjects,
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
                                onDeleteRequest = { onProjectDeleteRequest(project) },
                                onEditRequest = { onProjectEditRequest(project) },
                                onToggleLockRequest = if (onToggleLockRequest != null) { { onToggleLockRequest(project) } } else null,
                                onHaptic = { type -> triggerHaptic(type) }
                            )
                        }
                    }
                }
            }
        }

        // 3. TopBar sits on top of the list, measuring its own height dynamically
        CollectionsTopBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onGloballyPositioned { coordinates ->
                    topBarHeight = with(density) { coordinates.size.height.toDp() }
                },
            isScrolled = isScrolled.value,
            searchQuery = searchQuery,
            isSearchActive = isSearchActive,
            onSearchQueryChange = onSearchQueryChange,
            onSearchActiveChange = onSearchActiveChange
        )
    }
}

@Composable
private fun CollectionsTopBar(
    modifier: Modifier = Modifier,
    isScrolled: Boolean,
    searchQuery: String,
    isSearchActive: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

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

    val searchAlpha by animateFloatAsState(
        targetValue = if (isSearchActive) 1f else 0f,
        animationSpec = tween(300),
        label = "searchAlpha"
    )

    val titleAlpha by animateFloatAsState(
        targetValue = if (isSearchActive) 0f else 1f,
        animationSpec = tween(300),
        label = "titleAlpha"
    )

    val titleTranslationX by animateFloatAsState(
        targetValue = if (isSearchActive) -40f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "titleTranslationX"
    )

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        } else {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                drawRect(
                    color = Color.White.copy(alpha = if (isScrolled) 0.08f else 0f),
                    topLeft = Offset(0f, size.height - 1f),
                    size = Size(size.width, 1f)
                )
            }
            .background(
                color = MaterialTheme.colorScheme.background.copy(alpha = bgAlpha)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { /* Consume */ }
            )
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 20.dp, bottom = 16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            if (titleAlpha > 0f) {
                Text(
                    text = "My Collections",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1.5).sp,
                    color = titleColor,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = titleScale
                            scaleY = titleScale
                            translationY = titleOffsetY
                            translationX = titleTranslationX
                            alpha = titleAlpha
                            transformOrigin = TransformOrigin(0f, 0.5f)
                        }
                )
            }

            if (isSearchActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .graphicsLayer { alpha = searchAlpha },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        placeholder = {
                            Text(
                                "Search collections...",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    )

                    Spacer(Modifier.width(8.dp))

                    IconButton(
                        onClick = { onSearchActiveChange(false) },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (!isSearchActive) {
                IconButton(
                    onClick = { onSearchActiveChange(true) },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(44.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            RoundedCornerShape(14.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NoResultsFoundView(searchQuery: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "No matches for \"$searchQuery\"",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Check the spelling or try searching for something else.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                "Tap Add button below to create your first collection",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                letterSpacing = 0.sp
            )
        }
    }
}
