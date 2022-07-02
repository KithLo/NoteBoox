package com.kithlo.noteboox.filesystem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kithlo.noteboox.common.*
import com.kithlo.noteboox.data.file.BranchNode
import com.kithlo.noteboox.data.file.EmptyNode
import com.kithlo.noteboox.data.file.LeafNode
import com.kithlo.noteboox.data.file.Node
import com.kithlo.noteboox.data.note.LinePaper
import com.kithlo.noteboox.modal.InputModal
import com.kithlo.noteboox.modal.TextModal
import com.kithlo.noteboox.reactive.Computed
import com.kithlo.noteboox.reactive.Ref
import kotlin.math.max

class FilesystemViewModel(
    private val fileTreeModel: FileTreeViewModel,
    private val callback: Callback,
) : ViewModel() {
    companion object {
        val TAG: String = FilesystemViewModel::class.java.simpleName
        const val ITEM_PER_PAGE = 8
    }

    var currentRoot
        get() = fileTreeModel.currentRoot
        set(value) {
            fileTreeModel.currentRoot = value
            callback.onOpenFolder(value)
            reload(value)
        }

    val modifyAction = Ref<ModifyAction>(NoAction)

    val createFileModal = InputModal(
        title = "File name:",
        confirm = { newName ->
            if (newName.isNotBlank()) {
                val leaf = LeafNode(newName)
                leaf.parent = currentRoot
                leaf.paper = LinePaper(78)
                if (currentRoot.addChild(leaf)) {
                    fileTreeModel.save()
                    reload()
                }
            }
        },
    )

    val createFolderModal = InputModal(
        title = "Folder name:",
        confirm = { newName ->
            if (newName.isNotBlank()) {
                val newBranch = BranchNode(newName)
                if (currentRoot.addChild(newBranch)) {
                    fileTreeModel.save()
                    reload()
                }
            }
        },
    )

    val renameModal = InputModal(
        title = "New name:",
        confirm = { newName ->
            if (newName.isNotBlank() && selected.value.size == 1) {
                selected.value.first().rename(newName)
                unselectAll()
                fileTreeModel.save()
                reload()
            }
        },
    )

    val removeModal = TextModal(
        title = "Are you sure you want to delete the selected items?",
        confirm = {
            if (selected.value.isNotEmpty()) {
                selected.value.forEach { it.remove() }
                unselectAll()
                fileTreeModel.save()
                reload()
            }
        },
    )

    val exitModal = TextModal(
        title = "Are you sure you want to exit the application?",
        confirm = {
            callback.onExit()
        },
    )

    val list = Ref(emptyList<Node>())

    val selected = Ref(mutableSetOf<Node>())

    val selectedCount = Computed {
        selected.value.size
    }

    val canPaste = Computed { modifyAction.value.canExecute }
    val canPerformMultipleAction = Computed {
        !modifyAction.value.canExecute && selectedCount.value > 0
    }
    val canPerformSingleAction =
        Computed { !modifyAction.value.canExecute && selectedCount.value == 1 }
    val canSelectAll =
        Computed { !modifyAction.value.canExecute && list.value.isNotEmpty() && selectedCount.value != list.value.size }
    val canCancel = Computed { modifyAction.value.canExecute || selectedCount.value > 0 }

    val paging = Paging(1)

    val currentList = Computed {
        sliceListToCurrentPage(list.value) { EmptyNode }
    }

    val onClickEntry = { node: Node ->
        if (selected.value.isEmpty()) {
            when (node) {
                is BranchNode -> {
                    currentRoot = node
                }
                is LeafNode -> {
                    callback.onOpenFile(node)
                }
            }
        }
    }

    val onSelectEntry: (node: Node) -> Unit = { entry ->
        if (!selected.value.remove(entry)) {
            selected.value.add(entry)
        }
        selected.value = selected.value
    }

    fun executeModifyAction() {
        modifyAction.value.execute(currentRoot)
        modifyAction.value = NoAction
        fileTreeModel.save()
        reload()
    }

    fun onBack() {
        if (currentRoot.isRoot) {
            exitModal.visible.value = true
            return
        }
        currentRoot = currentRoot.parent
    }

    fun onRename() {
        renameModal.visible.value = true
    }

    fun onCopy() {
        if (selected.value.isNotEmpty()) {
            modifyAction.value = CopyAction(selected.value.toList())
            unselectAll()
        }
    }

    fun onMove() {
        if (selected.value.isNotEmpty()) {
            modifyAction.value = MoveAction(selected.value.toList())
            unselectAll()
        }
    }

    fun onRemove() {
        removeModal.visible.value = true
    }

    fun onCreateFile() {
        if (selected.value.isEmpty()) {
            createFileModal.visible.value = true
        }
    }

    fun onCreateFolder() {
        if (selected.value.isEmpty()) {
            createFolderModal.visible.value = true
        }
    }

    fun onSelectAll() {
        if (canSelectAll.value) {
            selected.value.addAll(list.value)
            selected.value = selected.value
        }
    }

    fun onCancel() {
        if (canCancel.value) {
            if (modifyAction.value.canExecute) {
                executeModifyAction()
            } else {
                unselectAll()
            }
        }
    }

    fun reload() {
        reload(currentRoot)
    }

    private fun reload(root: BranchNode) {
        changeList(root.children)
    }

    private fun unselectAll() {
        selected.value.clear()
        selected.value = selected.value
    }

    private fun changeList(newList: List<Node>) {
        unselectAll()
        paging.current.value = 1
        paging.total.value = max((newList.size + ITEM_PER_PAGE - 1) / ITEM_PER_PAGE, 1)
        list.value = newList
    }

    private fun <T> sliceListToCurrentPage(list: List<T>, empty: (index: Int) -> T): List<T> {
        val size = list.size
        val start = (paging.current.value - 1) * ITEM_PER_PAGE
        val end = start + ITEM_PER_PAGE
        return if (end > size) {
            val sublist = list.subList(start, size)
            sublist + List(ITEM_PER_PAGE - sublist.size, empty)
        } else {
            list.subList(start, end)
        }
    }

    interface ModifyAction {
        val nodes: List<Node>
        fun execute(base: BranchNode)

        val canExecute: Boolean
            get() = nodes.isNotEmpty()

        val text: String
    }

    private object NoAction : ModifyAction {
        override val nodes: List<Node> = emptyList()
        override val text: String = ""
        override fun execute(base: BranchNode) {}
    }

    private inner class CopyAction(override val nodes: List<Node>) : ModifyAction {
        override val text: String = "Copying ${nodes.size} items"
        override fun execute(base: BranchNode) {
            nodes.forEach { it.copyTo(base) }

        }
    }

    private inner class MoveAction(override val nodes: List<Node>) : ModifyAction {
        override val text: String = "Moving ${nodes.size} items"
        override fun execute(base: BranchNode) {
            nodes.forEach { it.moveTo(base) }
        }
    }

    class Factory(
        private val fileTreeModel: FileTreeViewModel,
        private val callback: Callback,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return FilesystemViewModel(fileTreeModel, callback) as T
        }
    }

    interface Callback {
        fun onOpenFile(leaf: LeafNode)
        fun onOpenFolder(root: BranchNode)
        fun onExit()
    }
}