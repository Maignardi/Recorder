package com.example.recorder.domain.model

data class Recording(
    val id: Long = 0,
    val name: String,
    val filePath: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val createdAt: Long,
    val isFavorite: Boolean = false,
    val isInTrash: Boolean = false,
    val transcription: String? = null,
    val summary: String? = null,
    val folderId: Long? = null
)
