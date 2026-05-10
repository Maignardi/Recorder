package com.example.recorder.data.local.dao

import androidx.room.*
import com.example.recorder.data.local.entity.FolderCount
import com.example.recorder.data.local.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Query("SELECT f.id, f.name, f.createdAt, COUNT(r.id) as recordingCount FROM folders f LEFT JOIN recordings r ON f.id = r.folderId AND r.isInTrash = 0 GROUP BY f.id ORDER BY f.name ASC")
    fun getFoldersWithCount(): Flow<List<FolderCount>>

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: FolderEntity): Long

    @Update
    suspend fun update(folder: FolderEntity)

    @Delete
    suspend fun delete(folder: FolderEntity)
}
