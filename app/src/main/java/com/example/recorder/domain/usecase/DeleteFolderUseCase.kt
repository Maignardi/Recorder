package com.example.recorder.domain.usecase

import com.example.recorder.domain.model.Folder
import com.example.recorder.domain.repository.FolderRepository
import javax.inject.Inject

class DeleteFolderUseCase @Inject constructor(private val repository: FolderRepository) {
    suspend operator fun invoke(folder: Folder) = repository.delete(folder)
}
