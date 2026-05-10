package com.example.recorder.data.mapper

import com.example.recorder.data.local.entity.RecordingEntity
import com.example.recorder.domain.model.Recording

fun RecordingEntity.toDomain() = Recording(
    id = id,
    name = name,
    filePath = filePath,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
    createdAt = createdAt,
    isFavorite = isFavorite,
    isInTrash = isInTrash,
    transcription = transcription,
    summary = summary,
    folderId = folderId
)

fun Recording.toEntity() = RecordingEntity(
    id = id,
    name = name,
    filePath = filePath,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
    createdAt = createdAt,
    isFavorite = isFavorite,
    isInTrash = isInTrash,
    transcription = transcription,
    summary = summary,
    folderId = folderId
)
