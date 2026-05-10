package com.example.recorder.data.repository

import com.example.recorder.data.local.dao.RecordingDao
import com.example.recorder.data.mapper.toDomain
import com.example.recorder.data.mapper.toEntity
import com.example.recorder.domain.model.Recording
import com.example.recorder.domain.repository.RecordingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject

class RecordingRepositoryImpl @Inject constructor(
    private val dao: RecordingDao
) : RecordingRepository {

    override fun getAllRecordings(): Flow<List<Recording>> =
        dao.getAllRecordings().map { list -> list.map { it.toDomain() } }

    override fun getFavorites(): Flow<List<Recording>> =
        dao.getFavorites().map { list -> list.map { it.toDomain() } }

    override fun getTrash(): Flow<List<Recording>> =
        dao.getTrash().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): Recording? =
        dao.getById(id)?.toDomain()

    override suspend fun insert(recording: Recording): Long =
        dao.insert(recording.toEntity())

    override suspend fun update(recording: Recording) =
        dao.update(recording.toEntity())

    override suspend fun delete(recording: Recording) =
        dao.delete(recording.toEntity())

    override suspend fun saveTranscription(id: Long, transcription: String, summary: String) =
        dao.updateTranscription(id, transcription, summary)

    override fun getByFolder(folderId: Long): Flow<List<Recording>> =
        dao.getByFolder(folderId).map { list -> list.map { it.toDomain() } }

    override suspend fun moveToFolder(recordingId: Long, folderId: Long?) =
        dao.updateFolder(recordingId, folderId)

    override suspend fun updateFavoriteBatch(ids: List<Long>, isFavorite: Boolean) =
        dao.updateFavoriteBatch(ids, isFavorite)

    override suspend fun moveToTrashBatch(ids: List<Long>) =
        dao.moveToTrashBatch(ids)

    override suspend fun moveToFolderBatch(ids: List<Long>, folderId: Long?) =
        dao.updateFolderBatch(ids, folderId)

    override suspend fun deleteAllInTrash() {
        val trashItems = dao.getAllInTrash()
        trashItems.forEach { entity ->
            File(entity.filePath).takeIf { it.exists() }?.delete()
        }
        dao.deleteAllInTrash()
    }
}
