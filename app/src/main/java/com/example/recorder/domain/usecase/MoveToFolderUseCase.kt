package com.example.recorder.domain.usecase

import com.example.recorder.domain.repository.RecordingRepository
import javax.inject.Inject

class MoveToFolderUseCase @Inject constructor(private val repository: RecordingRepository) {
    suspend operator fun invoke(recordingId: Long, folderId: Long?) = repository.moveToFolder(recordingId, folderId)
}
