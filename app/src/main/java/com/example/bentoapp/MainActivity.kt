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

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = BentoDatabase.getDatabase(applicationContext)
        val viewModel = BentoViewModel(db.bentoDao())
        val preferenceManager = PreferenceManager(applicationContext)

        // Theme Loading State
        var themeModeState by mutableStateOf<ThemeMode?>(null)
        lifecycleScope.launch {
            preferenceManager.themeMode.collect { mode ->
                themeModeState = mode
            }
        }

        val splashShownAt = System.currentTimeMillis()
        val minSplashDuration = 1050L // ring finishes at 1000ms + 50ms buffer

        splashScreen.setKeepOnScreenCondition {
            val elapsed = System.currentTimeMillis() - splashShownAt
            val animationDone = elapsed >= minSplashDuration
            val loadingDone = !viewModel.isLoading.value
            val themeDone = themeModeState != null
            !(animationDone && loadingDone && themeDone)
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
                        DashboardScreen(
                            projects = projects,
                            projectCounts = projectCounts,
                            preferenceManager = preferenceManager,
                            currentThemeMode = themeMode,
                            onProjectClick = { project ->
                                navController.navigate("collection_detail/${project.id}/${project.name}")
                            },
                            onProjectCreated = { name, imageUri, isBackground, shapeIndex ->
                                viewModel.addProject(applicationContext, name, imageUri, isBackground, shapeIndex)
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
                            onProjectUpdated = { project, newUri, isBackground, shapeIndex ->
                                viewModel.updateProject(applicationContext, project, newUri, isBackground, shapeIndex)
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