package com.example.recorder.presentation.transcription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recorder.BuildConfig
import com.example.recorder.domain.model.Recording
import com.example.recorder.domain.repository.RecordingRepository
import com.example.recorder.domain.usecase.TranscribeAndSummarizeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class TranscriptionUiState {
    object Idle : TranscriptionUiState()
    data class Loading(val step: String) : TranscriptionUiState()
    data class HasResult(val transcription: String, val summary: String) : TranscriptionUiState()
    data class Error(val message: String) : TranscriptionUiState()
}

@HiltViewModel
class TranscriptionViewModel @Inject constructor(
    private val repository: RecordingRepository,
    private val transcribeAndSummarize: TranscribeAndSummarizeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<TranscriptionUiState>(TranscriptionUiState.Idle)
    val uiState: StateFlow<TranscriptionUiState> = _uiState.asStateFlow()

    private val _recording = MutableStateFlow<Recording?>(null)
    val recording: StateFlow<Recording?> = _recording.asStateFlow()

    fun loadRecording(id: Long) = viewModelScope.launch {
        val rec = repository.getById(id) ?: return@launch
        _recording.value = rec
        if (!rec.transcription.isNullOrBlank() && !rec.summary.isNullOrBlank()) {
            _uiState.value = TranscriptionUiState.HasResult(rec.transcription, rec.summary)
        }
    }

    fun transcribe() = viewModelScope.launch {
        val rec = _recording.value ?: return@launch

        if (BuildConfig.GROQ_API_KEY.isBlank()) {
            _uiState.value = TranscriptionUiState.Error(
                "Groq API key not configured.\n\nAdd to local.properties:\nGROQ_API_KEY=gsk_your_key\n\nGet yours free at console.groq.com"
            )
            return@launch
        }

        _uiState.value = TranscriptionUiState.Loading("Transcribing audio with Whisper...")
        try {
            _uiState.value = TranscriptionUiState.Loading("Transcribing audio with Whisper...")
            val (transcription, summary) = transcribeAndSummarize(rec)
            _uiState.value = TranscriptionUiState.HasResult(transcription, summary)
            _recording.value = rec.copy(transcription = transcription, summary = summary)
        } catch (e: Exception) {
            val msg = when {
                e.message?.contains("401") == true -> "Invalid API key. Check GROQ_API_KEY."
                e.message?.contains("413") == true -> "File too large (limit: 25 MB)."
                e.message?.contains("429") == true -> "Rate limit reached. Please try again shortly."
                else -> e.message ?: "Unknown error"
            }
            _uiState.value = TranscriptionUiState.Error(msg)
        }
    }
}
