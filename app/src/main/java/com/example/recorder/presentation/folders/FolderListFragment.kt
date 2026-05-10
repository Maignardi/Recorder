package com.example.recorder.presentation.folders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.recorder.R
import com.example.recorder.databinding.FragmentFolderListBinding
import com.example.recorder.domain.model.Folder
import com.example.recorder.domain.model.RecordingFolder
import com.example.recorder.presentation.SharedTabViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FolderListFragment : Fragment() {

    private var _binding: FragmentFolderListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FolderListViewModel by viewModels()
    private val sharedTabVM: SharedTabViewModel by activityViewModels()
    private lateinit var adapter: FolderAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFolderListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapter()
        setupBottomNav()
        setupFab()
        binding.btnCreateFolder.setOnClickListener { showCreateFolderDialog() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.foldersWithCount.collect { list ->
                    val items = list.map { FolderAdapter.FolderItem(it.folder, it.count) }
                    adapter.submitList(items)
                    binding.recyclerViewFolders.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
                    binding.tvEmptyFolders.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.selectedItemId = R.id.nav_folders
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_all -> {
                    sharedTabVM.setFolder(RecordingFolder.ALL)
                    findNavController().popBackStack(R.id.recordingListFragment, false)
                    true
                }
                R.id.nav_favorites -> {
                    sharedTabVM.setFolder(RecordingFolder.FAVORITES)
                    findNavController().popBackStack(R.id.recordingListFragment, false)
                    true
                }
                R.id.nav_folders -> true
                R.id.nav_trash -> {
                    sharedTabVM.setFolder(RecordingFolder.TRASH)
                    findNavController().popBackStack(R.id.recordingListFragment, false)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupFab() {
        binding.fabRecord.setOnClickListener {
            findNavController().navigate(R.id.action_folders_to_record)
        }
    }

    private fun setupAdapter() {
        adapter = FolderAdapter(
            onFolderClick = { folder ->
                val bundle = Bundle().apply {
                    putLong("folderId", folder.id)
                    putString("folderName", folder.name)
                }
                findNavController().navigate(R.id.action_folders_to_contents, bundle)
            },
            onMenuClick = { anchor, folder -> showFolderMenu(anchor, folder) }
        )
        binding.recyclerViewFolders.adapter = adapter
    }

    private fun showFolderMenu(anchor: View, folder: Folder) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.menu_folder_item, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_folder_rename -> { showRenameFolderDialog(folder); true }
                R.id.action_folder_delete -> { confirmDeleteFolder(folder); true }
                else -> false
            }
        }
        popup.show()
    }

    private fun showCreateFolderDialog() {
        val editText = EditText(requireContext()).apply { hint = "Folder name" }
        val padding = (16 * resources.displayMetrics.density).toInt()
        editText.setPadding(padding, padding / 2, padding, padding / 2)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("New Folder")
            .setView(editText)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotBlank()) viewModel.create(name)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showRenameFolderDialog(folder: Folder) {
        val editText = EditText(requireContext()).apply {
            setText(folder.name)
            setSelection(folder.name.length)
        }
        val padding = (16 * resources.displayMetrics.density).toInt()
        editText.setPadding(padding, padding / 2, padding, padding / 2)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Rename Folder")
            .setView(editText)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotBlank()) viewModel.rename(folder, name)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun confirmDeleteFolder(folder: Folder) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Folder?")
            .setMessage("Recordings inside will not be deleted.")
            .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.delete(folder) }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
