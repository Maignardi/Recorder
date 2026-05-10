package com.example.recorder.domain.usecase

import com.example.recorder.domain.repository.FolderRepository
import javax.inject.Inject

class GetFoldersUseCase @Inject constructor(private val repository: FolderRepository) {
    operator fun invoke() = repository.getAllFolders()
}
