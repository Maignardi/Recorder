package com.example.recorder.domain.repository

import com.example.recorder.domain.model.Folder
import com.example.recorder.domain.model.FolderWithCount
import kotlinx.coroutines.flow.Flow

interface FolderRepository {
    fun getAllFolders(): Flow<List<Folder>>
    fun getFoldersWithCount(): Flow<List<FolderWithCount>>
    suspend fun getById(id: Long): Folder?
    suspend fun create(name: String)
    suspend fun rename(folder: Folder, newName: String)
    suspend fun delete(folder: Folder)
}
