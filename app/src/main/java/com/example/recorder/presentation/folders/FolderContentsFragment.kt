package com.example.recorder.presentation.folders

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ActionMode
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.recorder.R
import com.example.recorder.databinding.FragmentFolderContentsBinding
import com.example.recorder.domain.model.Recording
import com.example.recorder.domain.model.RecordingFolder
import com.example.recorder.presentation.list.RecordingAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class FolderContentsFragment : Fragment() {

    private var _binding: FragmentFolderContentsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FolderContentsViewModel by viewModels()
    private lateinit var adapter: RecordingAdapter
    private var actionMode: ActionMode? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFolderContentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val folderId = arguments?.getLong("folderId") ?: return
        val folderName = arguments?.getString("folderName") ?: ""
        viewModel.load(folderId, folderName)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        adapter = RecordingAdapter(
            folder = RecordingFolder.ALL,
            onItemClick = { openPlayer(it) },
            onPlayClick = { openPlayer(it) },
            onFavoriteClick = { viewModel.toggleFavorite(it) },
            onMenuClick = { anchor, rec -> showRecordingMenu(anchor, rec) },
            onLongClick = { toggleSelection(it) },
            onSelectionToggle = { toggleSelection(it) }
        )
        binding.recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.folderName.collect { name ->
                        binding.tvFolderName.text = name
                    }
                }
                launch {
                    viewModel.recordings.collect { list ->
                        adapter.submitList(list)
                        binding.recyclerView.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
                        binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.selectedIds.collect { ids ->
                        adapter.setSelectedIds(ids)
                        if (ids.isEmpty()) actionMode?.finish()
                        else actionMode?.title = "${ids.size} selected"
                    }
                }
            }
        }
    }

    private fun toggleSelection(recording: Recording) {
        viewModel.toggleSelection(recording.id)
        if (actionMode == null) {
            actionMode = (requireActivity() as? androidx.appcompat.app.AppCompatActivity)
                ?.startSupportActionMode(actionModeCallback)
        }
    }

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.menu_multi_select, menu)
            return true
        }
        override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.action_multi_favorite -> {
                    viewModel.toggleFavoriteBatch(true)
                    mode.finish()
                    true
                }
                R.id.action_multi_delete -> {
                    viewModel.moveToTrashBatch()
                    mode.finish()
                    true
                }
                R.id.action_multi_share -> {
                    shareBatch()
                    mode.finish()
                    true
                }
                R.id.action_multi_move_folder -> {
                    showBatchFolderPicker()
                    true
                }
                else -> false
            }
        }
        override fun onDestroyActionMode(mode: ActionMode) {
            viewModel.clearSelection()
            actionMode = null
        }
    }

    private fun showRecordingMenu(anchor: View, recording: Recording) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.menu_recording_item, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_rename -> { showRenameDialog(recording); true }
                R.id.action_trim -> { openTrim(recording); true }
                R.id.action_favorite -> { viewModel.toggleFavorite(recording); true }
                R.id.action_transcribe -> { openTranscription(recording); true }
                R.id.action_move_to_folder -> { showFolderPicker(recording); true }
                R.id.action_share -> { shareRecording(recording); true }
                R.id.action_move_trash -> { viewModel.moveToTrash(recording); true }
                else -> false
            }
        }
        popup.show()
    }

    private fun showRenameDialog(recording: Recording) {
        val editText = EditText(requireContext()).apply {
            setText(recording.name)
            setSelection(recording.name.length)
        }
        val padding = (16 * resources.displayMetrics.density).toInt()
        editText.setPadding(padding, padding / 2, padding, padding / 2)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_rename_title)
            .setView(editText)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotBlank()) viewModel.rename(recording, name)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showFolderPicker(recording: Recording) {
        val folders = viewModel.folders.value
        val entries = arrayOf("None (remove from folder)") + folders.map { it.name }.toTypedArray()
        val currentIndex = folders.indexOfFirst { it.id == recording.folderId }.let { if (it == -1) 0 else it + 1 }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Move to Folder")
            .setSingleChoiceItems(entries, currentIndex) { dialog, which ->
                val folderId = if (which == 0) null else folders[which - 1].id
                viewModel.setFolder(recording.id, folderId)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun shareRecording(recording: Recording) {
        val file = File(recording.filePath)
        if (!file.exists()) {
            Toast.makeText(requireContext(), "File not found", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(
            requireContext(), "${requireContext().packageName}.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share recording"))
    }

    private fun showBatchFolderPicker() {
        val folders = viewModel.folders.value
        val entries = arrayOf("None (remove from folder)") + folders.map { it.name }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Move selected to Folder")
            .setItems(entries) { dialog, which ->
                val folderId = if (which == 0) null else folders[which - 1].id
                viewModel.moveToFolderBatch(folderId)
                actionMode?.finish()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun shareBatch() {
        val selectedIds = viewModel.selectedIds.value
        val selectedRecordings = viewModel.recordings.value.filter { selectedIds.contains(it.id) }
        
        val uris = ArrayList<android.net.Uri>()
        selectedRecordings.forEach { recording ->
            val file = File(recording.filePath)
            if (file.exists()) {
                uris.add(FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    file
                ))
            }
        }

        if (uris.isEmpty()) return

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "audio/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share recordings"))
    }

    private fun openPlayer(recording: Recording) {
        if (findNavController().currentDestination?.id == R.id.folderContentsFragment) {
            val bundle = Bundle().apply { putLong("recordingId", recording.id) }
            findNavController().navigate(R.id.action_contents_to_player, bundle)
        }
    }

    private fun openTrim(recording: Recording) {
        if (findNavController().currentDestination?.id == R.id.folderContentsFragment) {
            val bundle = Bundle().apply { putLong("recordingId", recording.id) }
            findNavController().navigate(R.id.action_contents_to_trim, bundle)
        }
    }

    private fun openTranscription(recording: Recording) {
        if (findNavController().currentDestination?.id == R.id.folderContentsFragment) {
            val bundle = Bundle().apply { putLong("recordingId", recording.id) }
            findNavController().navigate(R.id.action_contents_to_transcription, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
