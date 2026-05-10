package com.example.recorder.data.local.dao

import androidx.room.*
import com.example.recorder.data.local.entity.RecordingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {

    @Query("SELECT * FROM recordings WHERE isInTrash = 0 ORDER BY createdAt DESC")
    fun getAllRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE isFavorite = 1 AND isInTrash = 0 ORDER BY createdAt DESC")
    fun getFavorites(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE isInTrash = 1 ORDER BY createdAt DESC")
    fun getTrash(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): RecordingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RecordingEntity): Long

    @Update
    suspend fun update(entity: RecordingEntity)

    @Delete
    suspend fun delete(entity: RecordingEntity)

    @Query("SELECT * FROM recordings WHERE isInTrash = 1")
    suspend fun getAllInTrash(): List<RecordingEntity>

    @Query("DELETE FROM recordings WHERE isInTrash = 1")
    suspend fun deleteAllInTrash()

    @Query("UPDATE recordings SET transcription = :transcription, summary = :summary WHERE id = :id")
    suspend fun updateTranscription(id: Long, transcription: String, summary: String)

    @Query("SELECT * FROM recordings WHERE folderId = :folderId AND isInTrash = 0 ORDER BY createdAt DESC")
    fun getByFolder(folderId: Long): Flow<List<RecordingEntity>>

    @Query("UPDATE recordings SET folderId = :folderId WHERE id = :recordingId")
    suspend fun updateFolder(recordingId: Long, folderId: Long?)

    @Query("UPDATE recordings SET isFavorite = :isFavorite WHERE id IN (:ids)")
    suspend fun updateFavoriteBatch(ids: List<Long>, isFavorite: Boolean)

    @Query("UPDATE recordings SET isInTrash = 1 WHERE id IN (:ids)")
    suspend fun moveToTrashBatch(ids: List<Long>)

    @Query("UPDATE recordings SET folderId = :folderId WHERE id IN (:ids)")
    suspend fun updateFolderBatch(ids: List<Long>, folderId: Long?)
}
