package com.kithlo.noteboox.data.note

import android.graphics.Canvas

data class GridPaper(val rowHeight: Int, val columnWidth: Int) : Paper(PaperType.GRID) {
    override fun drawOn(canvas: Canvas) {
        val width = canvas.width
        val height = canvas.height
        val hLines = height / rowHeight
        val vLines = width / columnWidth
        for (i in 1..hLines) {
            val y = (rowHeight * i).toFloat()
            canvas.drawLine(0f, y, width.toFloat(), y, paint)
        }
        for (i in 1..vLines) {
            val x = (columnWidth * i).toFloat()
            canvas.drawLine(x, 0f, x, height.toFloat(), paint)
        }
    }
}
