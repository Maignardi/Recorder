package com.example.recorder.presentation.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recorder.domain.model.Folder
import com.example.recorder.domain.model.FolderWithCount
import com.example.recorder.domain.usecase.CreateFolderUseCase
import com.example.recorder.domain.usecase.DeleteFolderUseCase
import com.example.recorder.domain.usecase.GetFoldersWithCountUseCase
import com.example.recorder.domain.usecase.RenameFolderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FolderListViewModel @Inject constructor(
    getFoldersWithCountUseCase: GetFoldersWithCountUseCase,
    private val createFolderUseCase: CreateFolderUseCase,
    private val deleteFolderUseCase: DeleteFolderUseCase,
    private val renameFolderUseCase: RenameFolderUseCase
) : ViewModel() {

    val foldersWithCount: StateFlow<List<FolderWithCount>> = getFoldersWithCountUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun create(name: String) = viewModelScope.launch { createFolderUseCase(name) }

    fun delete(folder: Folder) = viewModelScope.launch { deleteFolderUseCase(folder) }

    fun rename(folder: Folder, newName: String) = viewModelScope.launch { renameFolderUseCase(folder, newName) }
}
