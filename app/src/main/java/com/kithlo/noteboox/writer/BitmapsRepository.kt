package com.kithlo.noteboox.writer

import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import java.lang.Runnable

class BitmapsRepository(
    private val scope: CoroutineScope,
    private val bitmaps: MutableList<Bitmap?>,
    private val callback: Callback
) {
    companion object {
        private const val AUTO_SAVE_INTERVAL = 5000L
    }

    private var cursor: Int = 0
    private val channel = Channel<Runnable>()
    private var job: Job? = null

    init {
        scope.launch {
            channel.receiveAsFlow().collect { runnable ->
                withContext(Dispatchers.IO) {
                    runnable.run()
                }
            }
        }
    }

    fun start() {
        job = scope.launch {
            while (true) {
                delay(AUTO_SAVE_INTERVAL)
                channel.send(AutoSaveEvent())
            }
        }
    }

    fun stop() {
        scope.launch {
            channel.send(AutoSaveEvent())
            job?.cancelAndJoin()
        }
    }

    fun add(page: Int) {
        scope.launch {
            channel.send(AddPageEvent(page))
        }
    }

    fun change(page: Int, prev: Int) {
        scope.launch {
            channel.send(ChangePageEvent(page, prev))
        }
    }

    fun remove(page: Int) {
        scope.launch {
            channel.send(RemovePageEvent(page))
        }
    }

    private fun save(index: Int) {
        val bitmap = bitmaps[index] ?: return
        callback.save(bitmap, index + 1)
    }

    fun finish() {
        scope.launch {
            channel.send(FinishEvent())
        }
    }

    private inner class AddPageEvent(private val page: Int) : Runnable {
        override fun run() {
            val index = page - 1
            callback.changeBitmap(null)
            cursor = index
            for (i in bitmaps.size downTo page) {
                callback.move(i, i + 1)
            }
            bitmaps.add(index, callback.getBitmap())
        }
    }

    private inner class ChangePageEvent(private val page: Int, private val prev: Int) : Runnable {
        override fun run() {
            while (bitmaps.size < page) {
                bitmaps.add(null)
            }
            val bitmap = bitmaps[page - 1]
            val prevBitmap = callback.changeBitmap(bitmap)
            bitmaps[prev - 1] = prevBitmap
            cursor = page - 1
        }
    }

    private inner class RemovePageEvent(private val page: Int) : Runnable {
        override fun run() {
            val index = page - 1
            bitmaps.removeAt(index)
            val current = if (index >= bitmaps.size) index - 1 else index
            cursor = current
            callback.changeBitmap(bitmaps[current])
            callback.remove(page)
            for (i in page..bitmaps.size) {
                callback.move(i + 1, i)
            }
        }
    }

    private inner class AutoSaveEvent : Runnable {
        override fun run() {
            bitmaps[cursor] = callback.getBitmap()
            save(cursor)
        }
    }

    private inner class FinishEvent : Runnable {
        override fun run() {
            channel.close()
            callback.finish()
        }
    }

    interface Callback {
        fun getBitmap(): Bitmap?
        fun changeBitmap(bitmap: Bitmap?): Bitmap?
        fun save(bitmap: Bitmap, page: Int)
        fun move(from: Int, to: Int)
        fun remove(page: Int)
        fun finish()
    }
}