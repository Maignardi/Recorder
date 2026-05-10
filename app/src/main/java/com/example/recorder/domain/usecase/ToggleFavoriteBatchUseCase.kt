package com.example.recorder.domain.usecase

import com.example.recorder.domain.repository.RecordingRepository
import javax.inject.Inject

class ToggleFavoriteBatchUseCase @Inject constructor(
    private val repository: RecordingRepository
) {
    suspend operator fun invoke(ids: List<Long>, isFavorite: Boolean) = 
        repository.updateFavoriteBatch(ids, isFavorite)
}
