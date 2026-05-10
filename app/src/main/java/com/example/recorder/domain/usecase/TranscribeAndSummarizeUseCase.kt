package com.example.recorder.domain.usecase

import com.example.recorder.BuildConfig
import com.example.recorder.data.remote.GroqApiService
import com.example.recorder.data.remote.dto.ChatMessage
import com.example.recorder.data.remote.dto.ChatRequest
import com.example.recorder.domain.model.Recording
import com.example.recorder.domain.repository.RecordingRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

class TranscribeAndSummarizeUseCase @Inject constructor(
    private val groqApiService: GroqApiService,
    private val repository: RecordingRepository
) {
    suspend operator fun invoke(recording: Recording): Pair<String, String> {
        val auth = "Bearer ${BuildConfig.GROQ_API_KEY}"

        val audioFile = File(recording.filePath)
        check(audioFile.exists()) { "Audio file not found: ${recording.filePath}" }

        val filePart = MultipartBody.Part.createFormData(
            "file",
            audioFile.name,
            audioFile.asRequestBody("audio/m4a".toMediaType())
        )
        val modelPart = "whisper-large-v3".toRequestBody("text/plain".toMediaType())

        val transcription = groqApiService.transcribeAudio(auth, filePart, modelPart).text

        val chatRequest = ChatRequest(
            model = "llama-3.3-70b-versatile",
            messages = listOf(
                ChatMessage(
                    "system",
                    "You are an assistant that summarizes audio transcriptions clearly and concisely. Always respond in the same language as the transcription provided."
                ),
                ChatMessage(
                    "user",
                    "Please provide a concise summary of the following transcription, in the same language it is written:\n\n$transcription"
                )
            )
        )
        val summary = groqApiService.chat(auth, chatRequest)
            .choices.firstOrNull()?.message?.content
            ?: "Could not generate summary."

        repository.saveTranscription(recording.id, transcription, summary)
        return Pair(transcription, summary)
    }
}
