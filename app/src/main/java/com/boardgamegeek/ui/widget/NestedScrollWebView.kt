package com.boardgamegeek.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.webkit.WebView
import androidx.core.view.NestedScrollingChild
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.ViewCompat

/*
 * Copyright (C) 2016 Tobias Rohloff
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Copied here because the artifact is no longer available at jitpack.tio
class NestedScrollWebView : WebView, NestedScrollingChild {
    private var lastY = 0
    private val consumed = IntArray(2)
    private val offsetInWindow = IntArray(2)
    private var offsetInWindowY = 0
    private var childHelper = NestedScrollingChildHelper(this)

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        isNestedScrollingEnabled = true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val trackedEvent = MotionEvent.obtain(event)
        val action = event.action and MotionEvent.ACTION_MASK
        if (action == MotionEvent.ACTION_DOWN) {
            offsetInWindowY = 0
        }
        val y = event.y.toInt()
        event.offsetLocation(0f, offsetInWindowY.toFloat())
        return when (action) {
            MotionEvent.ACTION_DOWN -> {
                lastY = y
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)
                super.onTouchEvent(event)
            }
            MotionEvent.ACTION_MOVE -> {
                var deltaY = lastY - y
                if (dispatchNestedPreScroll(0, deltaY, consumed, offsetInWindow)) {
                    deltaY -= consumed[1]
                    trackedEvent.offsetLocation(0f, offsetInWindow[1].toFloat())
                    offsetInWindowY += offsetInWindow[1]
                }
                val oldY = scrollY
                lastY = y - offsetInWindow[1]
                if (deltaY < 0) {
                    val newScrollY = (oldY + deltaY).coerceAtLeast(0)
                    deltaY -= newScrollY - oldY
                    if (dispatchNestedScroll(0, newScrollY - deltaY, 0, deltaY, offsetInWindow)) {
                        lastY -= offsetInWindow[1]
                        trackedEvent.offsetLocation(0f, offsetInWindow[1].toFloat())
                        offsetInWindowY += offsetInWindow[1]
                    }
                }
                trackedEvent.recycle()
                super.onTouchEvent(trackedEvent)
            }
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                stopNestedScroll()
                super.onTouchEvent(event)
            }
            else -> false
        }
    }

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        childHelper.isNestedScrollingEnabled = enabled
    }

    override fun isNestedScrollingEnabled(): Boolean = childHelper.isNestedScrollingEnabled

    override fun startNestedScroll(axes: Int): Boolean = childHelper.startNestedScroll(axes)

    override fun stopNestedScroll() {
        childHelper.stopNestedScroll()
    }

    override fun hasNestedScrollingParent(): Boolean = childHelper.hasNestedScrollingParent()

    override fun dispatchNestedScroll(dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, offsetInWindow: IntArray?): Boolean =
            childHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow)

    override fun dispatchNestedPreScroll(dx: Int, dy: Int, consumed: IntArray?, offsetInWindow: IntArray?): Boolean =
            childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)

    override fun dispatchNestedFling(velocityX: Float, velocityY: Float, consumed: Boolean): Boolean =
            childHelper.dispatchNestedFling(velocityX, velocityY, consumed)

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean =
            childHelper.dispatchNestedPreFling(velocityX, velocityY)
}