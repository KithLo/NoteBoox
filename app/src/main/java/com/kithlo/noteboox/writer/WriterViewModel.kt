package com.kithlo.noteboox.writer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kithlo.noteboox.common.FileTreeViewModel
import com.kithlo.noteboox.common.Paging
import com.kithlo.noteboox.modal.TextModal
import com.kithlo.noteboox.reactive.Computed
import com.kithlo.noteboox.reactive.Ref

class WriterViewModel(
    private val fileTreeModel: FileTreeViewModel,
    private val callback: Callback,
) : ViewModel() {
    companion object {
        val TAG: String = WriterViewModel::class.java.simpleName
    }

    val paging = Paging(fileTreeModel.currentLeaf!!.pages) { page, prev ->
        callback.onChangePage(page, prev)
    }
    val editing = Ref(false)

    val canAdd = Computed { !editing.value }

    val canRemove = Computed { !editing.value && paging.total.value > 1 }

    val title = Ref(fileTreeModel.currentLeaf!!.name)

    val removeModal = TextModal(
        title = "Are you sure you want to delete this page?",
        confirm = {
            val currentPage = paging.current.value
            val totalPage = paging.total.value
            if (totalPage == currentPage) {
                paging.current.value = currentPage - 1
            }
            paging.total.value = totalPage - 1
            fileTreeModel.currentLeaf!!.pages -= 1
            callback.onRemove(currentPage)
            fileTreeModel.save()
        },
    )

    fun onAdd() {
        paging.total.value += 1
        fileTreeModel.currentLeaf!!.pages += 1
        val current = paging.current.value
        paging.current.value = current + 1
        callback.onAddPage(current + 1)
        fileTreeModel.save()
    }

    fun onRemove() {
        removeModal.visible.value = true
    }

    fun onBack() {
        callback.onBack()
    }

    fun onEdit() {
        editing.value = true
        callback.onEdit()
    }

    fun onDiscard() {
        editing.value = false
        callback.onDiscard()
    }

    fun onSave() {
        editing.value = false
        fileTreeModel.currentLeaf!!.name = title.value
        callback.onSave()
    }

    class Factory(
        private val fileTreeModel: FileTreeViewModel,
        private val callback: Callback,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return WriterViewModel(fileTreeModel, callback) as T
        }
    }

    interface Callback {
        fun onBack()
        fun onEdit()
        fun onDiscard()
        fun onSave()
        fun onAddPage(page: Int)
        fun onRemove(page: Int)
        fun onChangePage(page: Int, prev: Int)
    }
}