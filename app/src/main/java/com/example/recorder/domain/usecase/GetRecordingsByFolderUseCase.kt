package com.example.recorder.domain.usecase

import com.example.recorder.domain.model.Recording
import com.example.recorder.domain.model.RecordingFolder
import com.example.recorder.domain.repository.RecordingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecordingsByFolderUseCase @Inject constructor(
    private val repository: RecordingRepository
) {
    operator fun invoke(folder: RecordingFolder): Flow<List<Recording>> = when (folder) {
        RecordingFolder.ALL -> repository.getAllRecordings()
        RecordingFolder.FAVORITES -> repository.getFavorites()
        RecordingFolder.TRASH -> repository.getTrash()
    }
}
