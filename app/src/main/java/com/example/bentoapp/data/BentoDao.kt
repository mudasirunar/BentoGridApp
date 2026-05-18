package com.example.bentoapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class ProjectCounts(
    val projectId: Int,
    val tiles: Int,
    val images: Int
)

@Dao
interface BentoDao {
    // --- Project Methods ---
    @Query("SELECT * FROM projects ORDER BY id DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT projectId, COUNT(id) as tiles, SUM(CASE WHEN imageUri IS NOT NULL AND imageUri != '' THEN 1 ELSE 0 END) as images FROM bento_tiles GROUP BY projectId")
    fun getAllProjectCounts(): Flow<List<ProjectCounts>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteTile(tile: BentoEntity)

    // --- Tile Methods ---
    // This is crucial: it only fetches tiles belonging to a specific project
    @Query("SELECT * FROM bento_tiles WHERE projectId = :projectId ORDER BY id DESC")
    fun getTilesForProject(projectId: Int): Flow<List<BentoEntity>>

    @Query("SELECT * FROM bento_tiles WHERE id = :tileId")
    fun getTileById(tileId: Int): Flow<BentoEntity?>

    @Upsert
    suspend fun upsertTile(tile: BentoEntity)

    @Query("SELECT * FROM bento_tiles WHERE projectId = :projectId")
    suspend fun getTilesSync(projectId: Int): List<BentoEntity> // Non-flow for one-time cleanup

    @Query("SELECT * FROM bento_tiles WHERE imageUri IS NOT NULL AND imageUri != '' ORDER BY id DESC")
    fun getAllGalleryImages(): Flow<List<BentoEntity>>
    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("DELETE FROM bento_tiles WHERE projectId = :projectId")
    suspend fun deleteTilesByProject(projectId: Int)
}