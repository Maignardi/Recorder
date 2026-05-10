package com.example.recorder.presentation.player

import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recorder.domain.model.Recording
import com.example.recorder.domain.repository.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: RecordingRepository
) : ViewModel() {

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _recording = MutableStateFlow<Recording?>(null)
    val recording: StateFlow<Recording?> = _recording.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0)
    val currentPositionMs: StateFlow<Int> = _currentPositionMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    fun loadRecording(id: Long) {
        viewModelScope.launch {
            _recording.value = repository.getById(id)
            _recording.value?.let { initPlayer(it) }
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
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                player.playbackParams = player.playbackParams.setSpeed(_playbackSpeed.value)
            }
            player.start()
            _isPlaying.value = true
            startProgressTracking()
        }
    }

    fun skipForward() {
        val player = mediaPlayer ?: return
        val newPos = (player.currentPosition + 15000).coerceAtMost(player.duration)
        seekTo(newPos)
    }

    fun skipBackward() {
        val player = mediaPlayer ?: return
        val newPos = (player.currentPosition - 15000).coerceAtLeast(0)
        seekTo(newPos)
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        val player = mediaPlayer ?: return
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && player.isPlaying) {
            player.playbackParams = player.playbackParams.setSpeed(speed)
        }
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
        _currentPositionMs.value = positionMs
    }

    private fun startProgressTracking() {
        progressJob = viewModelScope.launch {
            while (isActive) {
                _currentPositionMs.value = mediaPlayer?.currentPosition ?: 0
                delay(200)
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
