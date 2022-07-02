package com.kithlo.noteboox.data.note

import android.graphics.Canvas
import android.graphics.Paint

abstract class Paper(val type: PaperType) {
    abstract fun drawOn(canvas: Canvas)

    companion object {
        val paint = run {
            val p = Paint()
            p.color = 0xFFBBBBBB.toInt()
            p.strokeWidth = 1f
            p.style = Paint.Style.FILL_AND_STROKE
            p.strokeCap = Paint.Cap.ROUND
            p.strokeJoin = Paint.Join.ROUND
            p
        }
    }
}