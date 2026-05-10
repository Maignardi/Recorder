package com.example.recorder.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.recorder.data.local.dao.FolderDao
import com.example.recorder.data.local.dao.RecordingDao
import com.example.recorder.data.local.database.RecorderDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE recordings ADD COLUMN transcription TEXT")
            db.execSQL("ALTER TABLE recordings ADD COLUMN summary TEXT")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS folders (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, createdAt INTEGER NOT NULL)"
            )
            db.execSQL("ALTER TABLE recordings ADD COLUMN folderId INTEGER")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RecorderDatabase =
        Room.databaseBuilder(context, RecorderDatabase::class.java, "recorder_db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideRecordingDao(db: RecorderDatabase): RecordingDao = db.recordingDao()

    @Provides
    fun provideFolderDao(db: RecorderDatabase): FolderDao = db.folderDao()
}
