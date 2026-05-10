package com.example.recorder.presentation.record

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.recorder.R
import com.example.recorder.databinding.FragmentRecordBinding
import com.example.recorder.service.RecordingService
import com.example.recorder.util.PermissionUtils
import com.example.recorder.util.toFormattedDuration
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class RecordFragment : Fragment() {

    private var _binding: FragmentRecordBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RecordViewModel by viewModels()
    private var pendingStop: RecordViewModel.StopResult? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results[Manifest.permission.RECORD_AUDIO] == true) {
            viewModel.bindAndStart()
        } else {
            showPermissionDeniedDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        setupBackPress()
        observeViewModel()
        requestPermissionsAndStart()
    }

    private fun setupBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() { handleDiscardClick() }
            }
        )
    }

    private fun requestPermissionsAndStart() {
        if (PermissionUtils.hasRecordAudioPermission(requireContext())) {
            viewModel.bindAndStart()
        } else {
            permissionLauncher.launch(PermissionUtils.requiredPermissions())
        }
    }

    private fun setupClickListeners() {
        binding.btnPauseResume.setOnClickListener { viewModel.pauseOrResume() }
        binding.btnStop.setOnClickListener { handleStopClick() }
        binding.btnDiscard.setOnClickListener { handleDiscardClick() }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.elapsedTime.collect { elapsed ->
                        binding.tvTimer.text = elapsed.toFormattedDuration()
                    }
                }
                launch {
                    viewModel.amplitudeList.collect { amplitudes ->
                        binding.waveformView.setAmplitudes(amplitudes)
                    }
                }
                launch {
                    viewModel.recordingState.collect { state ->
                        updateUiForState(state)
                    }
                }
                launch {
                    viewModel.saveEvent.collect {
                        findNavController().navigateUp()
                    }
                }
            }
        }
    }

    private fun updateUiForState(state: RecordingService.RecordingState) {
        when (state) {
            RecordingService.RecordingState.RECORDING -> {
                binding.btnPauseResume.setImageResource(R.drawable.ic_pause)
                binding.tvStatus.setText(R.string.status_recording)
                binding.recordingIndicator.visibility = View.VISIBLE
            }
            RecordingService.RecordingState.PAUSED -> {
                binding.btnPauseResume.setImageResource(R.drawable.ic_play)
                binding.tvStatus.setText(R.string.status_paused)
                binding.recordingIndicator.visibility = View.INVISIBLE
            }
            else -> Unit
        }
    }

    private fun handleStopClick() {
        val result = viewModel.stopRecording() ?: return
        pendingStop = result
        showSaveDialog(result)
    }

    private fun showSaveDialog(result: RecordViewModel.StopResult) {
        val defaultName = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            .format(Date())
        val editText = EditText(requireContext()).apply {
            setText(defaultName)
            selectAll()
            hint = getString(R.string.hint_recording_name)
        }
        val padding = (16 * resources.displayMetrics.density).toInt()
        editText.setPadding(padding, padding / 2, padding, padding / 2)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_save_title)
            .setView(editText)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val name = editText.text.toString().trim().ifEmpty { defaultName }
                viewModel.saveRecording(name, result.filePath, result.durationMs)
            }
            .setNegativeButton(R.string.action_discard) { _, _ ->
                showDiscardConfirmation(result.filePath)
            }
            .setCancelable(false)
            .show()
    }

    private fun handleDiscardClick() {
        val state = viewModel.recordingState.value
        if (state == RecordingService.RecordingState.RECORDING ||
            state == RecordingService.RecordingState.PAUSED
        ) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_discard_title)
                .setMessage(R.string.dialog_discard_message)
                .setPositiveButton(R.string.action_discard) { _, _ ->
                    viewModel.discardRecording()
                    findNavController().navigateUp()
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        } else {
            viewModel.discardRecording()
            findNavController().navigateUp()
        }
    }

    private fun showDiscardConfirmation(filePath: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_discard_title)
            .setMessage(R.string.dialog_discard_message)
            .setPositiveButton(R.string.action_discard) { _, _ ->
                java.io.File(filePath).takeIf { it.exists() }?.delete()
                findNavController().navigateUp()
            }
            .setNegativeButton(R.string.action_cancel) { _, _ ->
                showSaveDialog(pendingStop ?: return@setNegativeButton)
            }
            .show()
    }

    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_permission_title)
            .setMessage(R.string.dialog_permission_message)
            .setPositiveButton(R.string.action_ok) { _, _ -> findNavController().navigateUp() }
            .setCancelable(false)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
