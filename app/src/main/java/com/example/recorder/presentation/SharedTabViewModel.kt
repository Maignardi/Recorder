package com.example.recorder.presentation

import androidx.lifecycle.ViewModel
import com.example.recorder.domain.model.RecordingFolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SharedTabViewModel @Inject constructor() : ViewModel() {
    private val _folder = MutableStateFlow(RecordingFolder.ALL)
    val folder = _folder.asStateFlow()

    fun setFolder(folder: RecordingFolder) {
        _folder.value = folder
    }
}
