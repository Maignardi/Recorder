package com.example.recorder.data.local.entity

data class FolderCount(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val recordingCount: Int
)
