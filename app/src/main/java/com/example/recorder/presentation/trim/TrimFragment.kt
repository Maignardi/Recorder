package com.example.recorder.presentation.trim

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.recorder.R
import com.example.recorder.databinding.FragmentTrimBinding
import com.example.recorder.util.toFormattedDuration
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TrimFragment : Fragment() {

    private var _binding: FragmentTrimBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TrimViewModel by viewModels()
    private var isSeeking = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrimBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recordingId = arguments?.getLong("recordingId") ?: return
        viewModel.loadRecording(recordingId)
        setupControls()
        observeViewModel()
    }

    private fun setupControls() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnPlayPause.setOnClickListener { viewModel.playOrPause() }

        binding.seekBarPlayback.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) { isSeeking = true }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                isSeeking = false
                viewModel.seekTo(seekBar.progress)
            }
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) binding.tvCurrentTime.text = progress.toLong().toFormattedDuration()
            }
        })

        binding.trimRangeSlider.addOnChangeListener { slider, _, _ ->
            val values = slider.values
            val start = values[0].toLong()
            val end = values[1].toLong()
            viewModel.setTrimRange(start, end)
            binding.tvTrimStart.text = start.toFormattedDuration()
            binding.tvTrimEnd.text = end.toFormattedDuration()
        }

        binding.btnTrim.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Trim Audio")
                .setMessage("This will create a new file with the selected region. Continue?")
                .setPositiveButton("Trim") { _, _ -> viewModel.executeTrim() }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.recording.collect { rec ->
                        rec ?: return@collect
                        binding.tvRecordingName.text = rec.name
                        binding.tvTotalDuration.text = rec.durationMs.toFormattedDuration()
                        binding.tvTotalTime.text = rec.durationMs.toFormattedDuration()
                        binding.seekBarPlayback.max = rec.durationMs.toInt()
                        val dur = rec.durationMs.toFloat().coerceAtLeast(1f)
                        binding.trimRangeSlider.valueTo = dur
                        binding.trimRangeSlider.setValues(0f, dur)
                        binding.tvTrimStart.text = 0L.toFormattedDuration()
                        binding.tvTrimEnd.text = rec.durationMs.toFormattedDuration()
                    }
                }
                launch {
                    viewModel.isPlaying.collect { playing ->
                        binding.btnPlayPause.setImageResource(
                            if (playing) R.drawable.ic_pause else R.drawable.ic_play
                        )
                    }
                }
                launch {
                    viewModel.currentPositionMs.collect { pos ->
                        if (!isSeeking) {
                            binding.seekBarPlayback.progress = pos
                            binding.tvCurrentTime.text = pos.toLong().toFormattedDuration()
                        }
                    }
                }
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is TrimUiState.Idle -> {
                                binding.progressTrim.visibility = View.GONE
                                binding.tvTrimStatus.visibility = View.GONE
                                binding.btnTrim.isEnabled = true
                            }
                            is TrimUiState.Trimming -> {
                                binding.progressTrim.visibility = View.VISIBLE
                                binding.tvTrimStatus.visibility = View.VISIBLE
                                binding.btnTrim.isEnabled = false
                            }
                            is TrimUiState.Success -> {
                                binding.progressTrim.visibility = View.GONE
                                binding.tvTrimStatus.visibility = View.GONE
                                Snackbar.make(binding.root, "Audio trimmed successfully!", Snackbar.LENGTH_SHORT).show()
                                findNavController().popBackStack()
                            }
                            is TrimUiState.Error -> {
                                binding.progressTrim.visibility = View.GONE
                                binding.tvTrimStatus.visibility = View.GONE
                                binding.btnTrim.isEnabled = true
                                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
