package com.kithlo.noteboox.filesystem

import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.*
import com.kithlo.noteboox.R
import com.kithlo.noteboox.databinding.FragmentFilesystemBinding
import com.kithlo.noteboox.data.file.FileTree
import com.kithlo.noteboox.common.FileTreeViewModel
import com.kithlo.noteboox.data.file.BranchNode
import com.kithlo.noteboox.data.file.LeafNode
import com.kithlo.noteboox.common.FileSystem
import com.kithlo.noteboox.writer.WriterFragment
import org.json.JSONObject

class FilesystemFragment : Fragment(R.layout.fragment_filesystem) {

    companion object {
        val TAG: String = FilesystemFragment::class.java.simpleName
        const val NOTES_FILE_NAME = "notes.json"
    }

    private lateinit var binding: FragmentFilesystemBinding

    private lateinit var fileSystem: FileSystem
    private lateinit var notesFile: FileSystem.Entry

    private lateinit var adapter: BreadcrumbAdapter

    private val fileTreeModel: FileTreeViewModel by viewModels(
        ownerProducer = {
            requireActivity()
        },
        factoryProducer = {
            FileTreeViewModel.Factory {
                saveFileTree(it)
            }
        }
    )

    private val model: FilesystemViewModel by viewModels {
        FilesystemViewModel.Factory(
            fileTreeModel,
            object : FilesystemViewModel.Callback {
                override fun onOpenFile(leaf: LeafNode) {
                    toWriter(leaf)
                }

                override fun onOpenFolder(root: BranchNode) {
                    adapter.setRoot(root)
                }

                override fun onExit() {
                    requireActivity().finish()
                }
            }
        )
    }

    private val fragmentManagerListener = FragmentManager.OnBackStackChangedListener {
        if (parentFragmentManager.fragments.size == 1) {
            model.reload()
        }
    }

    private fun loadBaseDir() {
        val activity = requireActivity()
        val baseDir = RootDirSelector.get(activity)
        if (baseDir == null) {
            RootDirSelector.launch(activity) { uri ->
                RootDirSelector.put(activity, uri)
                onBaseDirLoaded(uri)
            }
        } else {
            onBaseDirLoaded(baseDir)
        }
    }

    private fun saveFileTree(tree: FileTree) {
        val str = tree.save().toString(2).replace("\\/", "/")
        notesFile.write { s -> s.bufferedWriter().use { it.write(str) } }
    }

    private fun loadFileTree(fileTree: FileTree) {
        val fileStr = notesFile.read { s -> s.bufferedReader().use { it.readText() } }
        if (fileStr != null && fileStr != "") {
            val obj = JSONObject(fileStr)
            fileTree.load(obj)
        }
        saveFileTree(fileTree)
    }

    private fun onBaseDirLoaded(uri: Uri) {
        val baseDir = DocumentsContract.buildDocumentUriUsingTree(
            uri,
            DocumentsContract.getTreeDocumentId(uri)
        )
        fileTreeModel.baseDir = baseDir
        fileSystem = FileSystem(requireActivity().contentResolver, baseDir)
        notesFile = fileSystem.resolve(NOTES_FILE_NAME, "application/json")
        loadFileTree(fileTreeModel.fileTree)
        model.reload()
    }

    private fun toWriter(leaf: LeafNode) {
        fileTreeModel.currentLeaf = leaf
        parentFragmentManager.commit {
            setReorderingAllowed(true)
            add<WriterFragment>(R.id.fragment_container_view)
            hide(this@FilesystemFragment)
            addToBackStack(null)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFilesystemBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = model
        adapter = BreadcrumbAdapter(fileTreeModel.fileTree.root) {
            model.currentRoot = it
        }
        binding.breadcrumb.adapter = adapter
        loadBaseDir()
        parentFragmentManager.addOnBackStackChangedListener(fragmentManagerListener)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        parentFragmentManager.removeOnBackStackChangedListener(fragmentManagerListener)
    }
}