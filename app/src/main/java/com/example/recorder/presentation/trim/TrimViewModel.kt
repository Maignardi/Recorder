package com.example.recorder.presentation.trim

import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recorder.domain.model.Recording
import com.example.recorder.domain.repository.RecordingRepository
import com.example.recorder.domain.usecase.TrimRecordingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class TrimUiState {
    object Idle : TrimUiState()
    object Trimming : TrimUiState()
    object Success : TrimUiState()
    data class Error(val message: String) : TrimUiState()
}

@HiltViewModel
class TrimViewModel @Inject constructor(
    private val repository: RecordingRepository,
    private val trimUseCase: TrimRecordingUseCase
) : ViewModel() {

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _recording = MutableStateFlow<Recording?>(null)
    val recording: StateFlow<Recording?> = _recording.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0)
    val currentPositionMs: StateFlow<Int> = _currentPositionMs.asStateFlow()

    private val _trimStartMs = MutableStateFlow(0L)
    val trimStartMs: StateFlow<Long> = _trimStartMs.asStateFlow()

    private val _trimEndMs = MutableStateFlow(0L)
    val trimEndMs: StateFlow<Long> = _trimEndMs.asStateFlow()

    private val _uiState = MutableStateFlow<TrimUiState>(TrimUiState.Idle)
    val uiState: StateFlow<TrimUiState> = _uiState.asStateFlow()

    fun loadRecording(id: Long) {
        viewModelScope.launch {
            val rec = repository.getById(id) ?: return@launch
            _recording.value = rec
            _trimStartMs.value = 0L
            _trimEndMs.value = rec.durationMs
            initPlayer(rec)
        }
    }

    private fun initPlayer(recording: Recording) {
        releasePlayer()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(recording.filePath)
            prepare()
            setOnCompletionListener {
                _isPlaying.value = false
                progressJob?.cancel()
                _currentPositionMs.value = 0
                seekTo(0)
            }
        }
    }

    fun playOrPause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
            progressJob?.cancel()
        } else {
            player.start()
            _isPlaying.value = true
            startProgressTracking()
        }
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
        _currentPositionMs.value = positionMs
    }

    fun setTrimRange(startMs: Long, endMs: Long) {
        _trimStartMs.value = startMs
        _trimEndMs.value = endMs
    }

    fun executeTrim() {
        val rec = _recording.value ?: return
        viewModelScope.launch {
            _uiState.value = TrimUiState.Trimming
            val success = trimUseCase(rec, _trimStartMs.value, _trimEndMs.value)
            _uiState.value = if (success) TrimUiState.Success
            else TrimUiState.Error("Failed to trim audio")
        }
    }

    private fun startProgressTracking() {
        progressJob = viewModelScope.launch {
            while (isActive) {
                _currentPositionMs.value = mediaPlayer?.currentPosition ?: 0
                delay(100)
            }
        }
    }

    private fun releasePlayer() {
        progressJob?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
        _isPlaying.value = false
        _currentPositionMs.value = 0
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}
