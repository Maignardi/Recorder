# 🎙️ Recorder

**An Android voice recorder with AI transcription and summaries.**

Record, organize, trim and play back audio, then turn any recording into a full transcript and a short summary in the same language it was spoken.

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-API_24+-3DDC84?style=flat-square&logo=android&logoColor=white)
![Hilt](https://img.shields.io/badge/DI-Hilt-2196F3?style=flat-square)
![Room](https://img.shields.io/badge/DB-Room-4285F4?style=flat-square)
![Groq](https://img.shields.io/badge/AI-Whisper_+_Llama_3.3-F55036?style=flat-square)

---

## ✨ Features

- **Background recording** in a foreground service with a live timer notification, plus pause, resume and discard
- **Live waveform** drawn from the microphone amplitude while recording
- **AI transcription** with Whisper large-v3 and **AI summaries** with Llama 3.3 70B, both through the Groq API
- **Lossless trimming**: cuts M4A/AAC files with `MediaExtractor` + `MediaMuxer`, no re-encoding
- **Folders**, favorites, rename and move, with batch actions on multiple recordings
- **Trash** with restore and empty-trash, so nothing is deleted by accident
- **Player** in a bottom sheet

## 🏗️ Architecture

Clean Architecture with MVVM, split into three layers:

```
presentation/   Fragments + ViewModels (record, list, folders, trim, player, transcription)
domain/         Models, repository interfaces and single-purpose use cases
data/           Room (DAOs, entities, mappers), Retrofit (Groq API) and repository implementations
di/             Hilt modules (app, database, network)
service/        RecordingService (foreground MediaRecorder)
```

Each user action maps to its own use case (`SaveRecordingUseCase`, `TrimRecordingUseCase`, `TranscribeAndSummarizeUseCase`, `MoveToTrashBatchUseCase`, and so on), which keeps ViewModels thin and the logic easy to test.

## 🧰 Tech stack

Kotlin · Coroutines · ViewModel + LiveData · Navigation Component · Hilt · Room · Retrofit + OkHttp · Gson · MediaRecorder · MediaExtractor/MediaMuxer · Material Components

## 🚀 Getting started

1. Clone the repository and open it in Android Studio.
2. Get a free API key at [console.groq.com](https://console.groq.com).
3. Add it to `local.properties` (never committed):

   ```properties
   GROQ_API_KEY=gsk_your_key
   ```

4. Run the `app` configuration on a device or emulator with API 24 or newer.

Recording, folders, trimming and playback work without a key. Only transcription and summaries need it.

---

Made by [Matheus Maignardi](https://github.com/Maignardi)
