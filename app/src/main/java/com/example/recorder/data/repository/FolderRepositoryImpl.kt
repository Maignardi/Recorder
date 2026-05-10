package com.example.recorder.data.repository

import com.example.recorder.data.local.dao.FolderDao
import com.example.recorder.data.mapper.toDomain
import com.example.recorder.data.mapper.toEntity
import com.example.recorder.domain.model.Folder
import com.example.recorder.domain.model.FolderWithCount
import com.example.recorder.domain.repository.FolderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FolderRepositoryImpl @Inject constructor(
    private val dao: FolderDao
) : FolderRepository {

    override fun getAllFolders(): Flow<List<Folder>> =
        dao.getAllFolders().map { list -> list.map { it.toDomain() } }

    override fun getFoldersWithCount(): Flow<List<FolderWithCount>> =
        dao.getFoldersWithCount().map { list ->
            list.map { FolderWithCount(Folder(it.id, it.name, it.createdAt), it.recordingCount) }
        }

    override suspend fun getById(id: Long): Folder? = dao.getById(id)?.toDomain()

    override suspend fun create(name: String) =
        dao.insert(Folder(name = name, createdAt = System.currentTimeMillis()).toEntity()).let { Unit }

    override suspend fun rename(folder: Folder, newName: String) =
        dao.update(folder.copy(name = newName).toEntity())

    override suspend fun delete(folder: Folder) =
        dao.delete(folder.toEntity())
}
