package com.example.recorder.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.recorder.data.local.dao.FolderDao
import com.example.recorder.data.local.dao.RecordingDao
import com.example.recorder.data.local.entity.FolderEntity
import com.example.recorder.data.local.entity.RecordingEntity

@Database(
    entities = [RecordingEntity::class, FolderEntity::class],
    version = 3,
    exportSchema = false
)
abstract class RecorderDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
    abstract fun folderDao(): FolderDao
}
