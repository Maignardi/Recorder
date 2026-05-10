package com.example.recorder.domain.usecase

import com.example.recorder.domain.model.Recording
import com.example.recorder.domain.repository.RecordingRepository
import java.io.File
import javax.inject.Inject

class DeleteRecordingUseCase @Inject constructor(
    private val repository: RecordingRepository
) {
    suspend operator fun invoke(recording: Recording) {
        repository.delete(recording)
        File(recording.filePath).takeIf { it.exists() }?.delete()
    }
}
