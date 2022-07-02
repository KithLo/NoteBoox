package com.kithlo.noteboox.reactive

import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData

class Ref<T>(value: T) : MutableLiveData<T>(value) {
    @MainThread
    override fun getValue(): T {
        ReactiveListener.touch(this)
        return super.getValue()!!
    }

    @MainThread
    override fun setValue(value: T) {
        super.setValue(value)
    }
}