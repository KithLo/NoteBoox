package com.kithlo.noteboox.reactive

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer

class Computed<T>(private val get: () -> T, private val set: ((t: T) -> Unit)? = null) :
    MediatorLiveData<T>() {
    constructor(get: () -> T) : this(get, null)

    private val observer = Observer<Any> {
        value = internalGet()
    }

    private var currentSources = emptySet<LiveData<*>>()

    @MainThread
    override fun getValue(): T {
        ReactiveListener.touch(this)
        return super.getValue()!!
    }

    @MainThread
    override fun setValue(value: T) {
        set?.invoke(value)
        super.setValue(value)
    }

    init {
        value = internalGet()
    }

    private fun internalGet(): T {
        val sources = mutableSetOf<LiveData<*>>()
        val result = ReactiveListener.collectDependencies(this, sources, get)
        for (source in sources) {
            addSource(source, observer)
        }
        for (source in currentSources) {
            if (!sources.contains(source)) {
                removeSource(source)
            }
        }
        currentSources = sources
        return result
    }
}