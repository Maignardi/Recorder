package com.example.recorder.domain.usecase

import com.example.recorder.domain.repository.RecordingRepository
import javax.inject.Inject

class DeleteRecordingBatchUseCase @Inject constructor(
    private val repository: RecordingRepository
) {
    suspend operator fun invoke(ids: List<Long>) {
        ids.forEach { id ->
            repository.getById(id)?.let { recording ->
                repository.delete(recording)
            }
        }
    }
}
