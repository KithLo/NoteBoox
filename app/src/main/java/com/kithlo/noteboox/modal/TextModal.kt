package com.kithlo.noteboox.modal

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.kithlo.noteboox.reactive.Ref

class TextModal(
    val title: String,
    private val confirm: () -> Unit,
    private val close: () -> Unit = {}
) {
    val visible = Ref(false)

    fun onClose(view: View) {
        internalClose(view)
    }

    fun onConfirm(view: View): Boolean {
        confirm()
        internalClose(view)
        return true
    }

    private fun internalClose(view: View) {
        val context = view.context
        val manager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        manager.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        visible.value = false
        close()
    }
}