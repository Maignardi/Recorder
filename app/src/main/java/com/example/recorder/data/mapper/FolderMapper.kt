package com.example.recorder.data.mapper

import com.example.recorder.data.local.entity.FolderEntity
import com.example.recorder.domain.model.Folder

fun FolderEntity.toDomain() = Folder(id = id, name = name, createdAt = createdAt)

fun Folder.toEntity() = FolderEntity(id = id, name = name, createdAt = createdAt)
