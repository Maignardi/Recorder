package com.example.recorder.presentation.list

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ActionMode
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.recorder.R
import com.example.recorder.databinding.FragmentRecordingListBinding
import com.example.recorder.domain.model.Recording
import com.example.recorder.domain.model.RecordingFolder
import com.example.recorder.presentation.SharedTabViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class RecordingListFragment : Fragment() {

    private var _binding: FragmentRecordingListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RecordingListViewModel by viewModels()
    private val sharedTabVM: SharedTabViewModel by activityViewModels()
    private lateinit var adapter: RecordingAdapter
    private var actionMode: ActionMode? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordingListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapter()
        setupBottomNav()
        setupFab()
        observeState()
    }


    private fun setupAdapter() {
        adapter = RecordingAdapter(
            folder = RecordingFolder.ALL,
            onItemClick = { openPlayer(it) },
            onPlayClick = { openPlayer(it) },
            onFavoriteClick = { viewModel.toggleFavorite(it) },
            onMenuClick = { view, rec -> showPopupMenu(view, rec, adapter.folder) },
            onLongClick = { toggleSelection(it) },
            onSelectionToggle = { toggleSelection(it) }
        )
        binding.recyclerView.adapter = adapter
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
            val menuRes = if (viewModel.currentFolder.value == RecordingFolder.TRASH) {
                R.menu.menu_multi_select_trash
            } else {
                R.menu.menu_multi_select
            }
            mode.menuInflater.inflate(menuRes, menu)
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
                R.id.action_multi_restore -> {
                    viewModel.restoreBatch()
                    mode.finish()
                    true
                }
                R.id.action_multi_delete_permanent -> {
                    confirmBatchDeletePermanent(mode)
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

    private fun confirmBatchDeletePermanent(mode: ActionMode) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(R.string.dialog_delete_message)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteBatch()
                mode.finish()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_all -> { sharedTabVM.setFolder(RecordingFolder.ALL); true }
                R.id.nav_favorites -> { sharedTabVM.setFolder(RecordingFolder.FAVORITES); true }
                R.id.nav_folders -> {
                    findNavController().navigate(R.id.action_list_to_folders)
                    true
                }
                R.id.nav_trash -> { sharedTabVM.setFolder(RecordingFolder.TRASH); true }
                else -> false
            }
        }
    }

    private fun setupFab() {
        binding.fabRecord.setOnClickListener {
            findNavController().navigate(R.id.action_list_to_record)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recordings.collect { list ->
                    updateList(list)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedIds.collect { ids ->
                    adapter.setSelectedIds(ids)
                    if (ids.isEmpty()) actionMode?.finish()
                    else actionMode?.title = "${ids.size} selected"
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedTabVM.folder.collect { folder ->
                    viewModel.setFolder(folder)
                    updateUIForFolder(folder)
                }
            }
        }
    }

    private fun updateUIForFolder(folder: RecordingFolder) {
        adapter.folder = folder
        adapter.notifyDataSetChanged()

        val selectedItemId = when (folder) {
            RecordingFolder.ALL -> R.id.nav_all
            RecordingFolder.FAVORITES -> R.id.nav_favorites
            RecordingFolder.TRASH -> R.id.nav_trash
        }
        if (binding.bottomNav.selectedItemId != selectedItemId) {
            binding.bottomNav.selectedItemId = selectedItemId
        }

        binding.fabRecord.visibility = View.VISIBLE
        if (folder == RecordingFolder.TRASH) {
            binding.btnEmptyTrash.visibility = View.VISIBLE
            binding.btnEmptyTrash.setOnClickListener { confirmEmptyTrash() }
        } else {
            binding.btnEmptyTrash.visibility = View.GONE
        }
    }

    private fun updateList(list: List<Recording>) {
        adapter.submitList(list)
        binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showPopupMenu(anchor: View, recording: Recording, folder: RecordingFolder) {
        val popup = PopupMenu(requireContext(), anchor)
        when (folder) {
            RecordingFolder.TRASH -> {
                popup.menuInflater.inflate(R.menu.menu_recording_trash, popup.menu)
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_restore -> { viewModel.restoreFromTrash(recording); true }
                        R.id.action_delete_permanent -> { confirmDeletePermanent(recording); true }
                        else -> false
                    }
                }
            }
            else -> {
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
            }
        }
        popup.show()
    }

    private fun showRenameDialog(recording: Recording) {
        val editText = android.widget.EditText(requireContext()).apply {
            setText(recording.name)
            setSelection(recording.name.length)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_rename_title)
            .setView(editText)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val newName = editText.text.toString()
                if (newName.isNotBlank()) viewModel.renameRecording(recording, newName)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showFolderPicker(recording: Recording) {
        val folders = viewModel.folders.value
        val entries = arrayOf("None (no folder)") + folders.map { it.name }.toTypedArray()
        val currentIndex = folders.indexOfFirst { it.id == recording.folderId }.let { if (it == -1) 0 else it + 1 }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Move to Folder")
            .setSingleChoiceItems(entries, currentIndex) { dialog, which ->
                val folderId = if (which == 0) null else folders[which - 1].id
                viewModel.moveToFolder(recording.id, folderId)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showBatchFolderPicker() {
        val folders = viewModel.folders.value
        val entries = arrayOf("None (no folder)") + folders.map { it.name }.toTypedArray()
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

    private fun openTrim(recording: Recording) {
        val bundle = Bundle().apply { putLong("recordingId", recording.id) }
        findNavController().navigate(R.id.action_list_to_trim, bundle)
    }

    private fun shareRecording(recording: Recording) {
        val file = File(recording.filePath)
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

    private fun openPlayer(recording: Recording) {
        if (findNavController().currentDestination?.id == R.id.recordingListFragment) {
            val bundle = Bundle().apply { putLong("recordingId", recording.id) }
            findNavController().navigate(R.id.action_list_to_player, bundle)
        }
    }

    private fun openTranscription(recording: Recording) {
        if (findNavController().currentDestination?.id == R.id.recordingListFragment) {
            val bundle = Bundle().apply { putLong("recordingId", recording.id) }
            findNavController().navigate(R.id.action_list_to_transcription, bundle)
        }
    }

    private fun confirmEmptyTrash() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_empty_trash_title)
            .setMessage(R.string.dialog_empty_trash_message)
            .setPositiveButton(R.string.action_empty) { _, _ -> viewModel.emptyTrash() }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun confirmDeletePermanent(recording: Recording) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(R.string.dialog_delete_message)
            .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.deletePermanently(recording) }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
