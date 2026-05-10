package com.example.recorder.presentation.player

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.recorder.R
import com.example.recorder.databinding.BottomSheetPlayerBinding
import com.example.recorder.util.toFormattedDuration
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class PlayerBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPlayerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PlayerViewModel by viewModels()
    private var isSeeking = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recordingId = arguments?.getLong("recordingId") ?: return
        viewModel.loadRecording(recordingId)
        setupControls()
        setupSeekBar()
        observeViewModel()
    }

    private fun setupControls() {
        binding.btnPlayPause.setOnClickListener { viewModel.playOrPause() }
        binding.btnForward.setOnClickListener { viewModel.skipForward() }
        binding.btnRewind.setOnClickListener { viewModel.skipBackward() }
        binding.btnSpeed.setOnClickListener { showSpeedDialog() }
        binding.btnTranscribe.setOnClickListener {
            val id = arguments?.getLong("recordingId") ?: return@setOnClickListener
            val bundle = Bundle().apply { putLong("recordingId", id) }
            findNavController().navigate(R.id.transcriptionFragment, bundle)
        }
        binding.btnTrim.setOnClickListener {
            val id = arguments?.getLong("recordingId") ?: return@setOnClickListener
            val bundle = Bundle().apply { putLong("recordingId", id) }
            findNavController().navigate(R.id.trimFragment, bundle)
        }
        binding.btnShare.setOnClickListener {
            viewModel.recording.value?.let { shareRecording(it.filePath) }
        }
    }

    private fun shareRecording(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(requireContext(), "File not found", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share recording"))
    }

    private fun showSpeedDialog() {
        val speeds = arrayOf("0.5x", "1.0x", "1.25x", "1.5x", "2.0x")
        val speedValues = floatArrayOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f)
        
        var currentIdx = speedValues.indexOfFirst { it == viewModel.playbackSpeed.value }
        if (currentIdx == -1) currentIdx = 1

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_playback_speed)
            .setSingleChoiceItems(speeds, currentIdx) { dialog, which ->
                viewModel.setPlaybackSpeed(speedValues[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun setupSeekBar() {
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) { isSeeking = true }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                isSeeking = false
                viewModel.seekTo(seekBar.progress)
            }
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) binding.tvCurrentTime.text = progress.toLong().toFormattedDuration()
            }
        })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.recording.collect { rec ->
                        rec ?: return@collect
                        binding.tvTitle.text = rec.name
                        binding.tvTotalTime.text = rec.durationMs.toFormattedDuration()
                        binding.seekBar.max = rec.durationMs.toInt()
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
                            binding.seekBar.progress = pos
                            binding.tvCurrentTime.text = pos.toLong().toFormattedDuration()
                        }
                    }
                }
                launch {
                    viewModel.playbackSpeed.collect { speed ->
                        binding.btnSpeed.text = if (speed % 1 == 0f) "${speed.toInt()}x" else "${speed}x"
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
