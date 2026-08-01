package com.example.bentoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.bentoapp.data.BentoDatabase
import com.example.bentoapp.ui.screens.CollectionDetailScreen
import com.example.bentoapp.ui.screens.DashboardScreen
import com.example.bentoapp.ui.theme.BentoAppTheme
import com.example.bentoapp.viewmodel.BentoViewModel
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.bentoapp.data.ProjectEntity
import com.example.bentoapp.ui.screens.AddTileScreen
import com.example.bentoapp.utils.PreferenceManager
import com.example.bentoapp.utils.ThemeMode

import androidx.fragment.app.FragmentActivity
import com.example.bentoapp.utils.BiometricPromptManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = BentoDatabase.getDatabase(applicationContext)
        val preferenceManager = PreferenceManager(applicationContext)
        val viewModel = BentoViewModel(db.bentoDao(), preferenceManager)
        val biometricPromptManager = BiometricPromptManager(this)

        // Theme Loading State
        var themeModeState by mutableStateOf<ThemeMode?>(null)
        lifecycleScope.launch {
            preferenceManager.themeMode.collect { mode ->
                themeModeState = mode
            }
        }

        // Biometric Lock Preference State
        var isBiometricLockEnabled by mutableStateOf(false)
        lifecycleScope.launch {
            preferenceManager.isBiometricLockEnabled.collect { enabled ->
                isBiometricLockEnabled = enabled
            }
        }

        val splashShownAt = System.currentTimeMillis()
        val minSplashDuration = 1050L // ring finishes at 1000ms + 50ms buffer

        splashScreen.setKeepOnScreenCondition {
            val elapsed = System.currentTimeMillis() - splashShownAt
            val animationDone = elapsed >= minSplashDuration
            val themeDone = themeModeState != null
            !(animationDone && themeDone)
        }

        setContent {
            val themeMode = themeModeState ?: return@setContent // Skip first frames until theme is ready
            
            val projects by viewModel.allProjects.collectAsState()
            val projectCounts by viewModel.allProjectCounts.collectAsState()

            BentoAppTheme(themeMode = themeMode) {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "main_dashboard",
                    // Snappy transitions for the whole app
                    enterTransition = {
                        fadeIn(tween(300)) + scaleIn(initialScale = 0.92f, animationSpec = tween(300))
                    },
                    exitTransition = {
                        fadeOut(tween(300)) + scaleOut(targetScale = 0.92f, animationSpec = tween(300))
                    },
                    popEnterTransition = {
                        fadeIn(tween(300)) + scaleIn(initialScale = 1.08f, animationSpec = tween(300))
                    },
                    popExitTransition = {
                        fadeOut(tween(300)) + scaleOut(targetScale = 1.08f, animationSpec = tween(300))
                    }
                ) {

                    // Level 1: Dashboard
                    composable("main_dashboard") {
                        val isBiometricLockActive = isBiometricLockEnabled || projects.any { it.isLocked }
                        var showManageLocksDialog by remember { mutableStateOf(false) }

                        DashboardScreen(
                            viewModel = viewModel,
                            projects = projects,
                            projectCounts = projectCounts,
                            preferenceManager = preferenceManager,
                            currentThemeMode = themeMode,
                            isBiometricLockEnabled = isBiometricLockActive,
                            onOpenManageLocksDialog = {
                                biometricPromptManager.promptBiometricAuth(
                                    title = "Manage Collection Locks",
                                    subtitle = "Confirm identity to access lock manager",
                                    onSuccess = {
                                        showManageLocksDialog = true
                                    }
                                )
                            },
                            onUnlockAllCollections = {
                                biometricPromptManager.promptBiometricAuth(
                                    title = "Unlock All Collections",
                                    subtitle = "Confirm identity to remove all locks",
                                    onSuccess = {
                                        lifecycleScope.launch {
                                            preferenceManager.setBiometricLockEnabled(false)
                                            projects.forEach { project ->
                                                if (project.isLocked) {
                                                    viewModel.toggleProjectLock(project)
                                                }
                                            }
                                        }
                                    }
                                )
                            },
                            showManageLocksDialog = showManageLocksDialog,
                            onDismissManageLocksDialog = { showManageLocksDialog = false },
                            onConfirmBatchLocks = { targetLockedIds ->
                                lifecycleScope.launch {
                                    projects.forEach { p ->
                                        val shouldBeLocked = p.id in targetLockedIds
                                        if (p.isLocked != shouldBeLocked) {
                                            viewModel.toggleProjectLock(p)
                                        }
                                    }
                                    preferenceManager.setBiometricLockEnabled(targetLockedIds.isNotEmpty())
                                }
                            },
                            onProjectClick = { project ->
                                if (project.isLocked) {
                                    biometricPromptManager.promptBiometricAuth(
                                        title = "Unlock ${project.name}",
                                        subtitle = "Authenticate fingerprint to open collection",
                                        onSuccess = {
                                            navController.navigate("collection_detail/${project.id}/${project.name}")
                                        }
                                    )
                                } else {
                                    navController.navigate("collection_detail/${project.id}/${project.name}")
                                }
                            },
                            onToggleProjectLock = { project ->
                                biometricPromptManager.promptBiometricAuth(
                                    title = if (project.isLocked) "Unlock ${project.name}" else "Lock ${project.name}",
                                    subtitle = "Confirm identity to change lock status",
                                    onSuccess = {
                                        viewModel.toggleProjectLock(project)
                                    }
                                )
                            },
                            onRequireBiometricAuth = { title, subtitle, onSuccess ->
                                biometricPromptManager.promptBiometricAuth(
                                    title = title,
                                    subtitle = subtitle,
                                    onSuccess = onSuccess
                                )
                            },
                            onProjectCreated = { name, imageUri, isBackground, shapeIndex, isLocked ->
                                viewModel.addProject(applicationContext, name, imageUri, isBackground, shapeIndex, isLocked)
                            },
                            onProjectDeletedImmediate = { project, onFetched ->
                                viewModel.deleteProjectDbOnly(project, onFetched)
                            },
                            onUndoProjectDelete = { project, tiles ->
                                viewModel.restoreProject(project, tiles)
                            },
                            onProjectDeleteConfirm = { project, tiles ->
                                viewModel.deleteProjectImagesOnly(project, tiles)
                            },
                            onProjectUpdated = { project, newUri, isBackground, shapeIndex, isLocked ->
                                viewModel.updateProject(applicationContext, project, newUri, isBackground, shapeIndex, isLocked)
                            }
                        )
                    }

                    // Level 2: Collection Detail
                    composable(
                        route = "collection_detail/{projectId}/{projectName}",
                        arguments = listOf(
                            navArgument("projectId") { type = NavType.IntType },
                            navArgument("projectName") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val id = backStackEntry.arguments?.getInt("projectId") ?: 0
                        val name = backStackEntry.arguments?.getString("projectName") ?: "Collection"
                        val currentProject = projects.find { it.id == id } ?: ProjectEntity(name = name)

                        val tilesFlow = remember(id) { viewModel.getTilesForProject(id) }
                        val tiles by tilesFlow.collectAsState()

                        CollectionDetailScreen(
                            project = currentProject,
                            projectName = name,
                            tiles = tiles,
                            onBackClick = { navController.popBackStack() },
                            onAddTileClick = { projectId ->
                                navController.navigate("add_tile/$projectId/${currentProject.shapeIndex}")
                            },
                            onEditTileClick = { projectId, tileId ->
                                navController.navigate("add_tile/$projectId/${currentProject.shapeIndex}?tileId=$tileId")
                            },
                            onDeleteTileImmediate = { tile ->
                                viewModel.deleteTileDbOnly(tile)
                            },
                            onUndoDeleteTile = { tile ->
                                viewModel.insertTileDirect(tile)
                            },
                            onDeleteTileConfirm = { tile ->
                                viewModel.deleteTileImageOnly(tile)
                            },
                            navController = navController,
                        )
                    }

                    // Level 3: Add/Edit Screen
                    composable(
                        route = "add_tile/{projectId}/{shapeIndex}?tileId={tileId}",
                        arguments = listOf(
                            navArgument("projectId") { type = NavType.IntType },
                            navArgument("shapeIndex") { type = NavType.IntType },
                            navArgument("tileId") { type = NavType.IntType; defaultValue = -1 }
                        )
                    ) { backStackEntry ->
                        val pId = backStackEntry.arguments?.getInt("projectId") ?: 0
                        val tId = backStackEntry.arguments?.getInt("tileId") ?: -1
                        val shapeIndex = backStackEntry.arguments?.getInt("shapeIndex") ?: 1

                        AddTileScreen(
                            projectId = pId,
                            tileId = if (tId != -1) tId else null,
                            shapeIndex = shapeIndex,
                            viewModel = viewModel,
                            onSave = { tile, uri ->
                                viewModel.saveTile(applicationContext, tile, uri)
                                val actionSignal = if (tId != -1) "updated" else "created"
                                navController.previousBackStackEntry?.savedStateHandle?.set("tile_action", actionSignal)
                                navController.popBackStack()
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}