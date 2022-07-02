package com.kithlo.noteboox.common

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kithlo.noteboox.data.file.FileTree
import com.kithlo.noteboox.data.file.LeafNode

class FileTreeViewModel(private val onSave: (fileTree: FileTree) -> Unit) : ViewModel() {
    companion object {
        val TAG: String = FileTreeViewModel::class.java.simpleName
    }

    val fileTree = FileTree()

    lateinit var baseDir: Uri

    var currentRoot = fileTree.root

    var currentLeaf: LeafNode? = null

    fun save() {
        onSave(fileTree)
    }

    class Factory(private val onMetadataChangeListener: (fileTree: FileTree) -> Unit) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return FileTreeViewModel(onMetadataChangeListener) as T
        }
    }
}