package com.example.recorder.domain.usecase

import com.example.recorder.domain.model.Recording
import com.example.recorder.domain.repository.RecordingRepository
import com.example.recorder.util.AudioTrimmer
import java.io.File
import javax.inject.Inject

class TrimRecordingUseCase @Inject constructor(
    private val repository: RecordingRepository
) {
    suspend operator fun invoke(recording: Recording, startMs: Long, endMs: Long): Boolean {
        val originalFile = File(recording.filePath)
        if (!originalFile.exists()) return false

        val directory = originalFile.parentFile ?: return false
        val timestamp = System.currentTimeMillis()
        val extension = recording.filePath.substringAfterLast(".", "m4a")
        val newFileName = "TRIMMED_${recording.name}_$timestamp.$extension"
        val newFile = File(directory, newFileName)

        val success = AudioTrimmer.trim(recording.filePath, newFile.absolutePath, startMs, endMs)
        
        if (success) {
            val newRecording = recording.copy(
                id = 0, // New entry
                name = "Trimmed — ${recording.name}",
                filePath = newFile.absolutePath,
                durationMs = endMs - startMs,
                sizeBytes = newFile.length(),
                createdAt = timestamp
            )
            repository.insert(newRecording)
        }
        
        return success
    }
}
