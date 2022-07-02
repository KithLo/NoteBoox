package com.kithlo.noteboox.common

import com.kithlo.noteboox.reactive.Computed
import com.kithlo.noteboox.reactive.Ref

class Paging(totalValue: Int, currentValue: Int = 1, private val onChange: (page: Int, oldPage: Int) -> Unit = { _, _ -> }) {

    val total = Ref(totalValue)
    val current = Ref(currentValue)

    val canGoBackward = Computed {
        current.value > 1
    }

    val canGoForward = Computed {
        current.value < total.value
    }

    val text = Computed {
        "Page ${"${current.value}".padStart(2, ' ')}/${total.value}"
    }

    private fun changeCurrentBy(delta: Int) {
        val c = current.value
        val v = c + delta
        val n = if (v <= 1) {
            1
        } else if (v > total.value) {
            total.value
        } else {
            v
        }
        if (c != n) {
            current.value = n
            onChange(n, c)
        }
    }

    fun goBackward() {
        changeCurrentBy(-1)
    }

    fun goForward() {
        changeCurrentBy(1)
    }
}