package com.kithlo.noteboox.data.note

import android.graphics.Canvas

data class LinePaper(val rowHeight: Int) : Paper(PaperType.LINE) {
    override fun drawOn(canvas: Canvas) {
        val width = canvas.width
        val height = canvas.height
        val lines = height / rowHeight
        for (i in 1..lines) {
            val y = (rowHeight * i).toFloat()
            canvas.drawLine(0f, y, width.toFloat(), y, paint)
        }
    }
}
