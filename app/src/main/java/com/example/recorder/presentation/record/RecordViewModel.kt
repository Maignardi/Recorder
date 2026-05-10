package com.example.recorder.presentation.record

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.recorder.domain.model.Recording
import com.example.recorder.domain.usecase.SaveRecordingUseCase
import com.example.recorder.service.RecordingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class RecordViewModel @Inject constructor(
    application: Application,
    private val saveRecordingUseCase: SaveRecordingUseCase
) : AndroidViewModel(application) {

    private var recordingService: RecordingService? = null
    private var serviceBound = false

    private val _recordingState = MutableStateFlow(RecordingService.RecordingState.IDLE)
    val recordingState: StateFlow<RecordingService.RecordingState> = _recordingState.asStateFlow()

    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime.asStateFlow()

    private val _amplitudeList = MutableStateFlow<List<Int>>(emptyList())
    val amplitudeList: StateFlow<List<Int>> = _amplitudeList.asStateFlow()

    private val _saveEvent = MutableSharedFlow<Unit>()
    val saveEvent: SharedFlow<Unit> = _saveEvent.asSharedFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as RecordingService.RecordingBinder
            recordingService = binder.getService()
            serviceBound = true
            observeService()
            recordingService?.startRecording()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            recordingService = null
            serviceBound = false
        }
    }

    private fun observeService() {
        val svc = recordingService ?: return
        viewModelScope.launch { svc.recordingState.collect { _recordingState.value = it } }
        viewModelScope.launch { svc.elapsedTime.collect { _elapsedTime.value = it } }
        viewModelScope.launch { svc.amplitudeList.collect { _amplitudeList.value = it } }
    }

    fun bindAndStart() {
        val app = getApplication<Application>()
        val intent = Intent(app, RecordingService::class.java)
        app.startForegroundService(intent)
        app.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun pauseOrResume() {
        val service = recordingService ?: return
        when (service.recordingState.value) {
            RecordingService.RecordingState.RECORDING -> service.pauseRecording()
            RecordingService.RecordingState.PAUSED -> service.resumeRecording()
            else -> Unit
        }
    }

    fun stopRecording(): StopResult? {
        val service = recordingService ?: return null
        val filePath = service.currentFilePath
        val duration = service.elapsedTime.value
        service.stopRecording()
        unbind()
        return StopResult(filePath, duration)
    }

    fun discardRecording() {
        val service = recordingService
        if (service != null) {
            service.discardRecording()
            unbind()
        } else {
            // Service may be starting but not yet connected — send stop intent
            val app = getApplication<Application>()
            app.stopService(Intent(app, RecordingService::class.java))
            unbind()
        }
    }

    fun saveRecording(name: String, filePath: String, durationMs: Long) {
        viewModelScope.launch {
            val file = File(filePath)
            val recording = Recording(
                name = name,
                filePath = filePath,
                durationMs = durationMs,
                sizeBytes = if (file.exists()) file.length() else 0L,
                createdAt = System.currentTimeMillis()
            )
            saveRecordingUseCase(recording)
            _saveEvent.emit(Unit)
        }
    }

    private fun unbind() {
        if (serviceBound) {
            getApplication<Application>().unbindService(serviceConnection)
            serviceBound = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        unbind()
    }

    data class StopResult(val filePath: String, val durationMs: Long)
}
