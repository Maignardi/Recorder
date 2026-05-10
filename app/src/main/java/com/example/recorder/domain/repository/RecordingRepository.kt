package com.example.recorder.domain.repository

import com.example.recorder.domain.model.Recording
import kotlinx.coroutines.flow.Flow

interface RecordingRepository {
    fun getAllRecordings(): Flow<List<Recording>>
    fun getFavorites(): Flow<List<Recording>>
    fun getTrash(): Flow<List<Recording>>
    suspend fun getById(id: Long): Recording?
    suspend fun insert(recording: Recording): Long
    suspend fun update(recording: Recording)
    suspend fun delete(recording: Recording)
    suspend fun deleteAllInTrash()
    suspend fun saveTranscription(id: Long, transcription: String, summary: String)
    fun getByFolder(folderId: Long): Flow<List<Recording>>
    suspend fun moveToFolder(recordingId: Long, folderId: Long?)
    suspend fun updateFavoriteBatch(ids: List<Long>, isFavorite: Boolean)
    suspend fun moveToTrashBatch(ids: List<Long>)
    suspend fun moveToFolderBatch(ids: List<Long>, folderId: Long?)
}
