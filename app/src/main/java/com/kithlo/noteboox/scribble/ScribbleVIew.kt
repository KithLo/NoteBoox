package com.kithlo.noteboox.scribble

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.onyx.android.sdk.device.Device

class ScribbleVIew @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val deviceReceiver = GlobalDeviceReceiver()

    private var state = State.INITIAL
    var editable = false
        private set

    var bitmap: Bitmap? = null
        private set

    var backgroundPainter: (canvas: Canvas) -> Unit = {}
        set(value) {
            field = value
            postInvalidate()
        }

    private var canvas: Canvas? = null

    private val device = Device.currentDevice

    var strokeWidth: Float = 1f
        set(value) {
            field = value
            device.setStrokeWidth(value)
        }

    fun clear() {
        if (canvas != null) {
            clearCanvas(canvas!!)
        }
        postInvalidate()
    }

    fun swapBitmap(bitmap: Bitmap?): Bitmap? {
        clearBuffer()
        val b = this.bitmap
        if (bitmap == null) {
            canvas = null
            this.bitmap = null
        } else {
            this.bitmap = bitmap
            canvas = Canvas(bitmap)
        }
        if (layoutDone) {
            resize()
        }
        postInvalidate()
        return b
    }

    private fun start() {
        if (state != State.INITIAL) {
            return
        }
        deviceReceiver.enable(context, true)
        deviceReceiver.setSystemNotificationPanelChangeListener { open ->
            if (editable) {
                if (open) {
                    pause()
                } else {
                    resume()
                }
            }
            if (open) {
                postInvalidate()
            }
        }
        deviceReceiver.setSystemScreenOnListener {
            if (editable) {
                postInvalidate()
            }
        }
        device.setStrokeStyle(1)
        device.setScreenHandWritingRegionMode(this, 1)
        device.setScreenHandWritingPenState(this, 1)
        postInvalidate()
        state = State.STARTED
    }

    private val lastPoint = floatArrayOf(0f, 0f)
    private var erase = false
    private var layoutDone = false

    private val drawPaint = run {
        val p = Paint()
        p.color = Color.BLACK
        p.style = Paint.Style.FILL_AND_STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p
    }

    private val erasePaint = run {
        val p = Paint()
        p.color = Color.TRANSPARENT
        p.style = Paint.Style.FILL_AND_STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        p
    }

    private fun drawStroke(e: MotionEvent) {
        val canvas = this.canvas ?: return
        val paint = if (erase) erasePaint else drawPaint
        for (i in 0 until e.historySize) {
            val x = e.getHistoricalX(i)
            val y = e.getHistoricalY(i)
            val pressure = e.getHistoricalPressure(i)
            val size = getSize(pressure, strokeWidth)
            if (size == 0f) {
                continue
            }
            paint.strokeWidth = if (erase) size * ERASE_STROKE_WIDTH_MULTIPLIER else size
            canvas.drawLine(lastPoint[0], lastPoint[1], x, y, paint)
            lastPoint[0] = x
            lastPoint[1] = y
        }
        val x = e.x
        val y = e.y
        val pressure = e.pressure
        val size = getSize(pressure, strokeWidth)
        if (size == 0f) {
            return
        }
        paint.strokeWidth = size
        canvas.drawLine(lastPoint[0], lastPoint[1], x, y, paint)
        lastPoint[0] = x
        lastPoint[1] = y
    }

    private fun clearBuffer() {
        if (state == State.RUNNING) {
            device.setScreenHandWritingPenState(this, 3)
            device.setScreenHandWritingPenState(this, 2)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (!editable) return false

        return when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                erase = when (e.getToolType(0)) {
                    MotionEvent.TOOL_TYPE_STYLUS -> false
                    MotionEvent.TOOL_TYPE_ERASER -> {
                        clearBuffer()
                        true
                    }
                    else -> return false
                }
                lastPoint[0] = e.x
                lastPoint[1] = e.y
                true
            }
            MotionEvent.ACTION_MOVE -> {
                drawStroke(e)
                postInvalidate()
                true
            }
            MotionEvent.ACTION_UP -> {
                drawStroke(e)
                postInvalidate()
                true
            }
            else -> false
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val bundle = Bundle()
        bundle.putParcelable(SUPER_STATE_KEY, superState)
        if (bitmap != null) {
            bundle.putParcelable(BITMAP_KEY, bitmap)
        }
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state != null) {
            val bundle = state as Bundle
            bitmap = bundle.getParcelable(BITMAP_KEY)
            super.onRestoreInstanceState(bundle.getParcelable(SUPER_STATE_KEY))
        }
    }

    fun resume() {
        if (state == State.INITIAL) {
            start()
        }
        if (state != State.PAUSED && state != State.STARTED) {
            return
        }
        resize()
        device.setStrokeWidth(strokeWidth)
        device.setScreenHandWritingPenState(this, 2)
        editable = true
        state = State.RUNNING
    }

    fun pause() {
        if (state != State.RUNNING) {
            return
        }
        editable = false
        device.setScreenHandWritingPenState(this, 3)
        state = State.PAUSED
    }

    fun quit() {
        if (state == State.INITIAL || state == State.DESTROYED) {
            return
        }
        editable = false
        device.setScreenHandWritingPenState(this, 0)
        deviceReceiver.enable(context.applicationContext, false)
        state = State.DESTROYED
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        layoutDone = true
        resize()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        backgroundPainter(canvas)
        if (bitmap != null) {
            canvas.drawBitmap(bitmap!!, 0f, 0f, emptyPaint)
        }
    }

    private fun resize() {
        val rect = Rect()
        getLocalVisibleRect(rect)
        val b = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        if (bitmap == null) {
            clearCanvas(c)
        } else {
            c.drawBitmap(bitmap!!, 0f, 0f, emptyPaint)
        }
        canvas = c
        bitmap = b
        device.setScreenHandWritingRegionLimit(this, arrayOf(rect))
    }

    companion object {
        val TAG: String = ScribbleVIew::class.java.simpleName
        private const val BITMAP_KEY = "bitmap"
        private const val SUPER_STATE_KEY = "super_state"
        private const val ERASE_STROKE_WIDTH_MULTIPLIER = 8f

        enum class State {
            INITIAL,
            STARTED,
            RUNNING,
            PAUSED,
            DESTROYED
        }

        private val emptyPaint = Paint()

        private fun clearCanvas(canvas: Canvas) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        }

        private fun getSize(pressure: Float, strokeWidth: Float): Float {
            return (strokeWidth + 1) * pressure * pressure + 2
        }
    }
}
