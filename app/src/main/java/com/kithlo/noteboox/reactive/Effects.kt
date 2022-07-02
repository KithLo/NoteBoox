package com.kithlo.noteboox.reactive

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer

class Effects {
    private val effects = mutableMapOf<() -> Unit, EffectHolder>()

    fun add(fn: () -> Unit) {
        val eff = EffectHolder(fn)
        eff.plug()
        effects[fn] = eff
    }

    fun remove(fn: () -> Unit) {
        val eff = effects.remove(fn) ?: return
        eff.unplug()
    }

    private class EffectHolder(private val fn: () -> Unit) {
        private val data = MediatorLiveData<Any>()
        private var currentSources = emptySet<LiveData<*>>()

        private val selfObserver = Observer<Any> {}

        private val observer = Observer<Any> {
            internalGet()
        }

        fun plug() {
            internalGet()
            data.observeForever(selfObserver)
        }

        fun unplug() {
            data.removeObserver(selfObserver)
            for (source in currentSources) {
                data.removeSource(source)
            }
            currentSources = emptySet()
        }

        private fun internalGet() {
            val sources = mutableSetOf<LiveData<*>>()
            ReactiveListener.collectDependencies(data, sources, fn)
            for (source in sources) {
                data.addSource(source, observer)
            }
            for (source in currentSources) {
                if (!sources.contains(source)) {
                    data.removeSource(source)
                }
            }
            currentSources = sources
        }
    }
}