package com.example.recorder.domain.usecase

import com.example.recorder.domain.repository.FolderRepository
import javax.inject.Inject

class CreateFolderUseCase @Inject constructor(private val repository: FolderRepository) {
    suspend operator fun invoke(name: String) = repository.create(name)
}
