package com.boardgamegeek.ui.widget

import android.content.Context
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import android.widget.ProgressBar
import androidx.core.view.isVisible

@Suppress("SpellCheckingInspection")
/**
 * ContentLoadingProgressBar implements a ProgressBar that waits a minimum time to be
 * dismissed before showing. Once visible, the progress bar will be visible for
 * a minimum amount of time to avoid "flashes" in the UI when an event could take
 * a largely variable time to complete (from none, to a user perceivable amount).
 *
 *
 * This version is similar to the support library version but implemented "the right way".
 *
 * @author Christophe Beyls
 */
class ContentLoadingProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.progressBarStyle,
) : ProgressBar(context, attrs, defStyleAttr) {

    private var attachedToWindow = false
    private var shown: Boolean = false
    internal var startTime = -1L

    private val delayedHide = Runnable {
        visibility = View.GONE
        startTime = -1L
    }

    private val delayedShow = Runnable {
        startTime = SystemClock.uptimeMillis()
        visibility = View.VISIBLE
    }

    init {
        shown = isVisible
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attachedToWindow = true
        if (shown && visibility != View.VISIBLE) {
            postDelayed(delayedShow, MIN_DELAY_MILLIS)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        attachedToWindow = false
        removeCallbacks(delayedHide)
        removeCallbacks(delayedShow)
        if (!shown && startTime != -1L) visibility = View.GONE
        startTime = -1L
    }

    /**
     * Hide the progress view if it is visible. The progress view will not be
     * hidden until it has been shown for at least a minimum show time. If the
     * progress view was not yet visible, cancels showing the progress view.
     */
    fun hide() {
        if (shown) {
            shown = false
            if (attachedToWindow) removeCallbacks(delayedShow)
            val diff = SystemClock.uptimeMillis() - startTime
            if (startTime == -1L || diff >= MIN_SHOW_TIME_MILLIS) {
                // The progress spinner has been shown long enough OR was not shown yet.
                // If it wasn't shown yet, it will just never be shown.
                visibility = View.GONE
                startTime = -1L
            } else {
                // The progress spinner is shown, but not long enough, so put a delayed
                // message in to hide it when its been shown long enough.
                postDelayed(delayedHide, MIN_SHOW_TIME_MILLIS - diff)
            }
        }
    }

    /**
     * Show the progress view after waiting for a minimum delay. If
     * during that time, hide() is called, the view is never made visible.
     */
    fun show() {
        if (!shown) {
            shown = true
            if (attachedToWindow) {
                removeCallbacks(delayedHide)
                if (startTime == -1L) {
                    postDelayed(delayedShow, MIN_DELAY_MILLIS)
                }
            }
        }
    }

    companion object {
        private const val MIN_SHOW_TIME_MILLIS = 500L
        private const val MIN_DELAY_MILLIS = 500L
    }
}
