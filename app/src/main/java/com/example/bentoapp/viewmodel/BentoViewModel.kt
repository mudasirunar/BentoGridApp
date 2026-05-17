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
import kotlinx.coroutines.flow.map
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

    val allProjectCounts: StateFlow<Map<Int, com.example.bentoapp.data.ProjectCounts>> = dao.getAllProjectCounts()
        .map { list -> list.associateBy { it.projectId } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    fun getTilesForProject(projectId: Int): StateFlow<List<BentoEntity>?> =
        dao.getTilesForProject(projectId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
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
            // SCENARIO B: User picked a NEW image from gallery or downloaded from URL
            else if (pickerUri.toString().startsWith("content://") || (pickerUri.scheme == "file" && pickerUri.path != finalImagePath)) {
                // Delete the old file first to avoid "ghost" files
                finalImagePath?.let { path ->
                    val file = File(path)
                    if (file.exists()) file.delete()
                }
                // Save the new one
                finalImagePath = saveImageToInternalStorage(context, pickerUri, "tile_img")
            }

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

    fun deleteProjectDbOnly(project: ProjectEntity, onTilesFetched: (List<BentoEntity>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val tiles = dao.getTilesSync(project.id)
            withContext(Dispatchers.Main) {
                onTilesFetched(tiles)
            }
            dao.deleteTilesByProject(project.id)
            dao.deleteProject(project)
        }
    }

    fun restoreProject(project: ProjectEntity, tiles: List<BentoEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertProject(project)
            tiles.forEach { dao.upsertTile(it) }
        }
    }

    fun deleteProjectImagesOnly(project: ProjectEntity, tiles: List<BentoEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            // Delete project cover image from storage
            if (project.imageUrl.isNotEmpty()) {
                val file = File(project.imageUrl)
                if (file.exists()) file.delete()
            }
            // Delete associated tile images from storage
            tiles.forEach { tile ->
                tile.imageUri?.let { path ->
                    val file = File(path)
                    if (file.exists()) file.delete()
                }
            }
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

    fun deleteTileDbOnly(tile: BentoEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteTile(tile)
        }
    }

    fun insertTileDirect(tile: BentoEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.upsertTile(tile)
        }
    }

    fun deleteTileImageOnly(tile: BentoEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            tile.imageUri?.let { path ->
                val file = File(path)
                if (file.exists()) file.delete()
            }
        }
    }

    // ─── OPTIMIZED IMAGE HANDLING ────────────────────────────────────
    // Added 'prefix' to distinguish between covers and tile photos
    fun saveImageToInternalStorage(context: Context, uri: Uri, prefix: String): String {
        return try {
            val inputStream = if (uri.scheme == "file") {
                java.io.FileInputStream(File(uri.path ?: ""))
            } else {
                context.contentResolver.openInputStream(uri)
            }
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

    suspend fun downloadImageFromUrl(context: Context, urlStr: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val url = java.net.URL(urlStr)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                try {
                    connection.connect()
                } catch (e: java.net.UnknownHostException) {
                    return@withContext Result.failure(Exception("No internet connection or invalid URL."))
                } catch (e: java.net.SocketTimeoutException) {
                    return@withContext Result.failure(Exception("Connection timed out. Please try again."))
                }

                val responseCode = connection.responseCode
                if (responseCode != java.net.HttpURLConnection.HTTP_OK) {
                    val errorMsg = when (responseCode) {
                        java.net.HttpURLConnection.HTTP_NOT_FOUND -> "Image not found at this URL (404)."
                        java.net.HttpURLConnection.HTTP_FORBIDDEN -> "Access to this image is forbidden (403)."
                        java.net.HttpURLConnection.HTTP_UNAUTHORIZED -> "Unauthorized to access this image (401)."
                        in 500..599 -> "Server error on the image host ($responseCode)."
                        else -> "Failed to fetch image. HTTP Error: $responseCode."
                    }
                    return@withContext Result.failure(Exception(errorMsg))
                }

                val contentType = connection.contentType
                if (contentType == null || !contentType.startsWith("image/")) {
                    return@withContext Result.failure(Exception("URL does not point to a valid image."))
                }

                val file = File(context.cacheDir, "temp_download_${System.currentTimeMillis()}.tmp")
                try {
                    connection.inputStream.use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: java.net.SocketTimeoutException) {
                    file.delete()
                    return@withContext Result.failure(Exception("Download timed out. Image might be too large or connection is slow."))
                } catch (e: Exception) {
                    file.delete()
                    return@withContext Result.failure(Exception("Error while downloading the image data."))
                }

                // Validate if it's an image
                val options = android.graphics.BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                android.graphics.BitmapFactory.decodeFile(file.absolutePath, options)
                if (options.outWidth == -1 || options.outHeight == -1) {
                    file.delete()
                    return@withContext Result.failure(Exception("Invalid or unsupported image format."))
                }

                Result.success(Uri.fromFile(file).toString())
            } catch (e: java.net.MalformedURLException) {
                Result.failure(Exception("Invalid URL format. Please enter a valid HTTP/HTTPS link."))
            } catch (e: Exception) {
                Result.failure(Exception("An unexpected error occurred: ${e.localizedMessage}"))
            }
        }
    }

}