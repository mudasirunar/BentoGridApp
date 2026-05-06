package com.example.bentoapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BentoDao {
    // --- Project Methods ---
    @Query("SELECT * FROM projects ORDER BY id DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

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

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("DELETE FROM bento_tiles WHERE projectId = :projectId")
    suspend fun deleteTilesByProject(projectId: Int)
}