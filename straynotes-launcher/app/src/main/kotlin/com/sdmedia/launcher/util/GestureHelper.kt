package com.sdmedia.launcher.util

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import kotlin.math.abs

enum class Gesture {
    SWIPE_UP, SWIPE_DOWN, SWIPE_LEFT, SWIPE_RIGHT, DOUBLE_TAP
}

class GestureHelper(
    context: Context,
    private val onGesture: (Gesture) -> Unit
) : GestureDetector.SimpleOnGestureListener() {

    private val detector = GestureDetector(context, this)

    private val SWIPE_THRESHOLD = 100
    private val SWIPE_VELOCITY = 100

    fun onTouchEvent(event: MotionEvent): Boolean = detector.onTouchEvent(event)

    override fun onDoubleTap(e: MotionEvent): Boolean {
        onGesture(Gesture.DOUBLE_TAP)
        return true
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        val dx = e2.x - (e1?.x ?: 0f)
        val dy = e2.y - (e1?.y ?: 0f)
        val absDx = abs(dx)
        val absDy = abs(dy)

        return if (absDx < absDy && absDy > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY) {
            onGesture(if (dy < 0) Gesture.SWIPE_UP else Gesture.SWIPE_DOWN)
            true
        } else if (absDx > absDy && absDx > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY) {
            onGesture(if (dx < 0) Gesture.SWIPE_LEFT else Gesture.SWIPE_RIGHT)
            true
        } else {
            false
        }
    }
}
