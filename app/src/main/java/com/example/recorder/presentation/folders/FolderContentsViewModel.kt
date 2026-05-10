package com.example.recorder.presentation.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recorder.domain.model.Folder
import com.example.recorder.domain.model.Recording
import com.example.recorder.domain.repository.RecordingRepository
import com.example.recorder.domain.usecase.GetFoldersUseCase
import com.example.recorder.domain.usecase.MoveToFolderUseCase
import com.example.recorder.domain.usecase.MoveToTrashUseCase
import com.example.recorder.domain.usecase.ToggleFavoriteUseCase
import com.example.recorder.domain.usecase.UpdateRecordingUseCase
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FolderContentsViewModel @Inject constructor(
    private val recordingRepository: RecordingRepository,
    getFoldersUseCase: GetFoldersUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val moveToTrashUseCase: MoveToTrashUseCase,
    private val updateRecordingUseCase: UpdateRecordingUseCase,
    private val moveToFolderUseCase: MoveToFolderUseCase,
    private val toggleFavoriteBatch: ToggleFavoriteBatchUseCase,
    private val moveToTrashBatch: MoveToTrashBatchUseCase,
    private val moveToFolderBatch: MoveToFolderBatchUseCase
) : ViewModel() {

    private val _folderId = MutableStateFlow(-1L)

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    val recordings: StateFlow<List<Recording>> = _folderId
        .flatMapLatest { id ->
            if (id == -1L) kotlinx.coroutines.flow.flowOf(emptyList())
            else recordingRepository.getByFolder(id)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val folders: StateFlow<List<Folder>> = getFoldersUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _folderName = MutableStateFlow("")
    val folderName: StateFlow<String> = _folderName.asStateFlow()

    fun load(folderId: Long, name: String) {
        _folderId.value = folderId
        _folderName.value = name
    }

    fun toggleFavorite(recording: Recording) = viewModelScope.launch { toggleFavoriteUseCase(recording) }

    fun moveToTrash(recording: Recording) = viewModelScope.launch { moveToTrashUseCase(recording) }

    fun rename(recording: Recording, newName: String) = viewModelScope.launch {
        updateRecordingUseCase(recording.copy(name = newName))
    }

    fun setFolder(recordingId: Long, folderId: Long?) = viewModelScope.launch {
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
