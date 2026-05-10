package com.example.recorder.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.recorder.MainActivity
import com.example.recorder.R
import com.example.recorder.util.toFormattedDuration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class RecordingService : Service() {

    inner class RecordingBinder : Binder() {
        fun getService(): RecordingService = this@RecordingService
    }

    enum class RecordingState { IDLE, RECORDING, PAUSED, STOPPED }

    private val binder = RecordingBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var mediaRecorder: MediaRecorder? = null
    private var timerJob: Job? = null
    private var amplitudeJob: Job? = null
    private var startTime = 0L
    private var accumulatedTime = 0L

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime.asStateFlow()

    private val _amplitudeList = MutableStateFlow<List<Int>>(emptyList())
    val amplitudeList: StateFlow<List<Int>> = _amplitudeList.asStateFlow()

    var currentFilePath: String = ""
        private set

    companion object {
        const val ACTION_START = "action_start"
        const val ACTION_STOP = "action_stop"
        const val ACTION_PAUSE = "action_pause"
        const val ACTION_RESUME = "action_resume"
        const val NOTIFICATION_ID = 101
        const val CHANNEL_ID = "recording_channel"
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP -> stopRecording()
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
        }
        return START_STICKY
    }

    fun startRecording() {
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification("00:00"),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification("00:00"))
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dir = getExternalFilesDir("Recordings") ?: filesDir
        dir.mkdirs()
        currentFilePath = "${dir.absolutePath}/REC_$timestamp.m4a"

        mediaRecorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(128000)
            setOutputFile(currentFilePath)
            prepare()
            start()
        }

        accumulatedTime = 0L
        startTime = System.currentTimeMillis()
        _recordingState.value = RecordingState.RECORDING
        startTimer()
        startAmplitudeCapture()
    }

    fun stopRecording() {
        timerJob?.cancel()
        amplitudeJob?.cancel()
        runCatching {
            mediaRecorder?.apply { stop(); release() }
        }
        mediaRecorder = null
        _recordingState.value = RecordingState.STOPPED
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching { mediaRecorder?.pause() }
            accumulatedTime += System.currentTimeMillis() - startTime
            timerJob?.cancel()
            amplitudeJob?.cancel()
            _recordingState.value = RecordingState.PAUSED
        }
    }

    fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching { mediaRecorder?.resume() }
            startTime = System.currentTimeMillis()
            _recordingState.value = RecordingState.RECORDING
            startTimer()
            startAmplitudeCapture()
        }
    }

    fun discardRecording() {
        stopRecording()
        File(currentFilePath).takeIf { it.exists() }?.delete()
    }

    private fun startTimer() {
        timerJob = serviceScope.launch {
            while (isActive) {
                val elapsed = accumulatedTime + (System.currentTimeMillis() - startTime)
                _elapsedTime.value = elapsed
                updateNotification(elapsed)
                delay(500)
            }
        }
    }

    private fun startAmplitudeCapture() {
        amplitudeJob = serviceScope.launch {
            while (isActive) {
                val amp = runCatching { mediaRecorder?.maxAmplitude ?: 0 }.getOrDefault(0)
                val updated = (_amplitudeList.value + amp).takeLast(120)
                _amplitudeList.value = updated
                delay(80)
            }
        }
    }

    private fun updateNotification(elapsedMs: Long) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(elapsedMs.toFormattedDuration()))
    }

    private fun buildNotification(timeText: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, RecordingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_recording)
            .setContentTitle(getString(R.string.notification_recording_title))
            .setContentText(timeText)
            .setContentIntent(openIntent)
            .addAction(R.drawable.ic_stop, getString(R.string.action_stop), stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        mediaRecorder?.release()
        mediaRecorder = null
    }
}
