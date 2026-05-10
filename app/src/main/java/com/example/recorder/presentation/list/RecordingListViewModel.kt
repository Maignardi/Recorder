package com.example.recorder.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recorder.domain.model.Recording
import com.example.recorder.domain.model.RecordingFolder
import com.example.recorder.domain.model.Folder
import com.example.recorder.domain.usecase.DeleteRecordingUseCase
import com.example.recorder.domain.usecase.EmptyTrashUseCase
import com.example.recorder.domain.usecase.GetFoldersUseCase
import com.example.recorder.domain.usecase.GetRecordingsByFolderUseCase
import com.example.recorder.domain.usecase.MoveToFolderUseCase
import com.example.recorder.domain.usecase.MoveToTrashUseCase
import com.example.recorder.domain.usecase.RestoreRecordingUseCase
import com.example.recorder.domain.usecase.ToggleFavoriteUseCase
import com.example.recorder.domain.usecase.UpdateRecordingUseCase
import com.example.recorder.domain.usecase.TrimRecordingUseCase
import com.example.recorder.domain.usecase.ToggleFavoriteBatchUseCase
import com.example.recorder.domain.usecase.MoveToTrashBatchUseCase
import com.example.recorder.domain.usecase.MoveToFolderBatchUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecordingListViewModel @Inject constructor(
    private val getRecordingsByFolder: GetRecordingsByFolderUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val moveToTrash: MoveToTrashUseCase,
    private val restore: RestoreRecordingUseCase,
    private val delete: DeleteRecordingUseCase,
    private val emptyTrash: EmptyTrashUseCase,
    private val updateRecording: UpdateRecordingUseCase,
    private val trimRecording: TrimRecordingUseCase,
    getFoldersUseCase: GetFoldersUseCase,
    private val moveToFolderUseCase: MoveToFolderUseCase,
    private val toggleFavoriteBatch: ToggleFavoriteBatchUseCase,
    private val moveToTrashBatch: MoveToTrashBatchUseCase,
    private val moveToFolderBatch: MoveToFolderBatchUseCase
) : ViewModel() {

    val folders: StateFlow<List<Folder>> = getFoldersUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _currentFolder = MutableStateFlow(RecordingFolder.ALL)
    val currentFolder: StateFlow<RecordingFolder> = _currentFolder.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val recordings: StateFlow<List<Recording>> = _currentFolder
        .flatMapLatest { folder ->
            android.util.Log.d("RecordingListViewModel", "Switching to folder: $folder")
            getRecordingsByFolder(folder)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun setFolder(folder: RecordingFolder) {
        _currentFolder.value = folder
    }

    fun toggleFavorite(recording: Recording) = viewModelScope.launch {
        toggleFavorite.invoke(recording)
    }

    fun renameRecording(recording: Recording, newName: String) = viewModelScope.launch {
        updateRecording(recording.copy(name = newName))
    }

    fun trimRecording(recording: Recording, startMs: Long, endMs: Long) = viewModelScope.launch {
        trimRecording.invoke(recording, startMs, endMs)
    }

    fun moveToTrash(recording: Recording) = viewModelScope.launch {
        moveToTrash.invoke(recording)
    }

    fun restoreFromTrash(recording: Recording) = viewModelScope.launch {
        restore.invoke(recording)
    }

    fun deletePermanently(recording: Recording) = viewModelScope.launch {
        delete.invoke(recording)
    }

    fun emptyTrash() = viewModelScope.launch {
        emptyTrash.invoke()
    }

    fun moveToFolder(recordingId: Long, folderId: Long?) = viewModelScope.launch {
        moveToFolderUseCase(recordingId, folderId)
    }

    fun toggleSelection(id: Long) {
        val current = _selectedIds.value
        _selectedIds.value = if (current.contains(id)) current - id else current + id
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun toggleFavoriteBatch(isFavorite: Boolean) = viewModelScope.launch {
        toggleFavoriteBatch.invoke(_selectedIds.value.toList(), isFavorite)
        clearSelection()
    }

    fun moveToTrashBatch() = viewModelScope.launch {
        moveToTrashBatch.invoke(_selectedIds.value.toList())
        clearSelection()
    }

    fun moveToFolderBatch(folderId: Long?) = viewModelScope.launch {
        moveToFolderBatch.invoke(_selectedIds.value.toList(), folderId)
        clearSelection()
    }
}
