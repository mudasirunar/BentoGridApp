package com.example.bentoapp.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bentoapp.data.BentoDao
import com.example.bentoapp.data.BentoEntity
import com.example.bentoapp.data.ProjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class BentoViewModel(private val dao: BentoDao) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    val allProjects: StateFlow<List<ProjectEntity>> = dao.getAllProjects()
        .onEach { _isLoading.value = false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getTilesForProject(projectId: Int): StateFlow<List<BentoEntity>?> =
        dao.getTilesForProject(projectId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null   // null = DB not yet responded
            )

    // Fetch a single tile for editing
    fun getTileById(tileId: Int): Flow<BentoEntity?> = dao.getTileById(tileId)

    fun saveTile(context: Context, tile: BentoEntity, pickerUri: Uri?) {
        viewModelScope.launch(Dispatchers.IO) {
            var finalImagePath = tile.imageUri // This is the current path in DB

            // SCENARIO A: User explicitly removed the image
            if (pickerUri == null) {
                // Delete the old file if it exists to save space
                finalImagePath?.let { path ->
                    val file = File(path)
                    if (file.exists()) file.delete()
                }
                finalImagePath = null
            }
            // SCENARIO B: User picked a NEW image from gallery
            else if (pickerUri.toString().startsWith("content://")) {
                // Delete the old file first to avoid "ghost" files
                finalImagePath?.let { path ->
                    val file = File(path)
                    if (file.exists()) file.delete()
                }
                // Save the new one
                finalImagePath = saveImageToInternalStorage(context, pickerUri, "tile_img")
            }
            // SCENARIO C: User didn't touch the image (pickerUri is the existing file path)
            // finalImagePath stays the same.

            dao.upsertTile(tile.copy(imageUri = finalImagePath))
        }
    }

    // ─── PROJECT CRUD ────────────────────────────────────────────────
    suspend fun addProject(context: Context, name: String, imageUriString: String, isBackground: Boolean, shapeIndex: Int): ProjectEntity {
        return withContext(Dispatchers.IO) {
            var finalPath = ""

            if (imageUriString.isNotEmpty()) {
                val uri = Uri.parse(imageUriString)
                finalPath = saveImageToInternalStorage(context, uri, "project_cover")
            }

            val newProject = ProjectEntity(
                name = name,
                imageUrl = finalPath,
                isBackground = isBackground,
                shapeIndex = shapeIndex
            )

            val id = dao.insertProject(newProject)
            newProject.copy(id = id.toInt())
        }
    }

    fun updateProject(context: Context, project: ProjectEntity, newImageUri: String, isBackground: Boolean, shapeIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            var finalPath = project.imageUrl

            // 1. Check if image has changed
            if (newImageUri != project.imageUrl) {
                if (newImageUri.startsWith("content://")) {
                    val uri = Uri.parse(newImageUri)
                    finalPath = saveImageToInternalStorage(context, uri, "project_cover")

                    // Delete old file
                    if (project.imageUrl.isNotEmpty()) File(project.imageUrl).delete()
                } else if (newImageUri.isEmpty()) {
                    if (project.imageUrl.isNotEmpty()) File(project.imageUrl).delete()
                    finalPath = ""
                }
            }

            // 2. Save updated project with new isBackground state
            dao.updateProject(
                project.copy(
                    imageUrl = finalPath,
                    isBackground = isBackground,
                    shapeIndex = shapeIndex
                )
            )
        }
    }

    fun deleteProjectComplete(context: Context, project: ProjectEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            // Delete project cover
            if (project.imageUrl.isNotEmpty()) File(project.imageUrl).delete()

            // Delete all associated tile images
            val tiles = dao.getTilesSync(project.id)
            tiles.forEach { tile ->
                tile.imageUri?.let { path ->
                    val file = File(path)
                    if (file.exists()) file.delete()
                }
            }
            dao.deleteTilesByProject(project.id)
            dao.deleteProject(project)
        }
    }

    fun deleteTile(context: Context, tile: BentoEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            // Delete the physical image file if it exists
            tile.imageUri?.let { path ->
                val file = File(path)
                if (file.exists()) file.delete()
            }
            dao.deleteTile(tile)
        }
    }

    // ─── OPTIMIZED IMAGE HANDLING ────────────────────────────────────
    // Added 'prefix' to distinguish between covers and tile photos
    fun saveImageToInternalStorage(context: Context, uri: Uri, prefix: String): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)

            // Bento tiles don't need massive 4K images. 800px is sweet spot for quality/speed.
            val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
            val targetWidth = 800
            val targetHeight = (targetWidth / ratio).toInt()

            val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(
                originalBitmap, targetWidth, targetHeight, true
            )

            val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
            val file = File(context.filesDir, fileName)

            FileOutputStream(file).use { output ->
                scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, output)
            }

            originalBitmap.recycle()
            scaledBitmap.recycle()

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

}