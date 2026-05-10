package com.example.recorder.presentation.transcription

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.recorder.databinding.FragmentTranscriptionBinding
import com.example.recorder.util.toFormattedDuration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TranscriptionFragment : Fragment() {

    private var _binding: FragmentTranscriptionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TranscriptionViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTranscriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recordingId = arguments?.getLong("recordingId") ?: return
        viewModel.loadRecording(recordingId)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnTranscribe.setOnClickListener { viewModel.transcribe() }
        binding.btnCopyTranscription.setOnClickListener {
            copyToClipboard("Transcription", binding.tvTranscription.text.toString())
        }
        binding.btnCopySummary.setOnClickListener {
            copyToClipboard("Summary", binding.tvSummary.text.toString())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.recording.collect { rec -> rec?.let { updateRecordingInfo(it) } } }
                launch { viewModel.uiState.collect { updateUiState(it) } }
            }
        }
    }

    private fun updateRecordingInfo(recording: com.example.recorder.domain.model.Recording) {
        binding.tvRecordingName.text = recording.name
        binding.tvRecordingDuration.text = recording.durationMs.toFormattedDuration()
    }

    private fun updateUiState(state: TranscriptionUiState) {
        when (state) {
            is TranscriptionUiState.Idle -> {
                binding.progressIndicator.visibility = View.GONE
                binding.tvLoadingStep.visibility = View.GONE
                binding.cardTranscription.visibility = View.GONE
                binding.cardSummary.visibility = View.GONE
                binding.tvError.visibility = View.GONE
                binding.btnTranscribe.isEnabled = true
                binding.btnTranscribe.text = "Transcribe and Summarize"
            }
            is TranscriptionUiState.Loading -> {
                binding.progressIndicator.visibility = View.VISIBLE
                binding.tvLoadingStep.visibility = View.VISIBLE
                binding.tvLoadingStep.text = state.step
                binding.cardTranscription.visibility = View.GONE
                binding.cardSummary.visibility = View.GONE
                binding.tvError.visibility = View.GONE
                binding.btnTranscribe.isEnabled = false
            }
            is TranscriptionUiState.HasResult -> {
                binding.progressIndicator.visibility = View.GONE
                binding.tvLoadingStep.visibility = View.GONE
                binding.tvError.visibility = View.GONE
                binding.cardTranscription.visibility = View.VISIBLE
                binding.cardSummary.visibility = View.VISIBLE
                binding.tvTranscription.text = state.transcription
                binding.tvSummary.text = state.summary
                binding.btnTranscribe.isEnabled = true
                binding.btnTranscribe.text = "Re-transcribe"
            }
            is TranscriptionUiState.Error -> {
                binding.progressIndicator.visibility = View.GONE
                binding.tvLoadingStep.visibility = View.GONE
                binding.cardTranscription.visibility = View.GONE
                binding.cardSummary.visibility = View.GONE
                binding.tvError.visibility = View.VISIBLE
                binding.tvError.text = state.message
                binding.btnTranscribe.isEnabled = true
                binding.btnTranscribe.text = "Try Again"
            }
        }
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(requireContext(), "$label copied!", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
