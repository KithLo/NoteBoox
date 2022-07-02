package com.kithlo.noteboox.reactive

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData

object ReactiveListener {
    private var currentSets = mutableListOf<MutableSet<LiveData<*>>>()

    @MainThread
    fun <T> collectDependencies(
        current: LiveData<*>,
        sources: MutableSet<LiveData<*>>,
        fn: () -> T
    ): T {
        currentSets.add(sources)
        val result = fn()
        currentSets.removeLast()
        sources.remove(current)
        return result
    }

    @MainThread
    fun touch(data: LiveData<*>) {
        currentSets.lastOrNull()?.add(data)
    }
}