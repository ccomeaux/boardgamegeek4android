package com.boardgamegeek.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatTextView

abstract class SelfUpdatingView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {
    private var isVisible: Boolean = false
    private var isRunning: Boolean = false

    var timeHintUpdateInterval = 30_000L

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isVisible = false
        updateRunning()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        isVisible = visibility == View.VISIBLE
        updateRunning()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        updateRunning()
    }

    private fun updateRunning() {
        val running = isVisible && isShown
        if (running != isRunning) {
            if (running) {
                updateText()
                postDelayed(mTickRunnable, timeHintUpdateInterval)
            } else {
                removeCallbacks(mTickRunnable)
            }
            isRunning = running
        }
    }

    private val mTickRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                updateText()
                postDelayed(this, timeHintUpdateInterval)
            }
        }
    }

    abstract fun updateText()
}