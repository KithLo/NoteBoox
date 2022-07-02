package com.kithlo.noteboox.data.note

import android.graphics.Canvas

object NonePaper : Paper(PaperType.NONE) {
    override fun drawOn(canvas: Canvas) {}
}
