package com.boardgamegeek.ui.widget

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.text.Html
import android.text.SpannedString
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.ViewCompat
import com.boardgamegeek.R
import com.boardgamegeek.extensions.formatTimestamp
import com.boardgamegeek.extensions.trimTrailingWhitespace

class TimestampView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {
    private var isVisible: Boolean = false
    private var isRunning: Boolean = false
    
    var timestamp: Long = 0
        set(value) {
            field = value
            updateText()
        }

    var format: String = ""
        set(value) {
            field = value
            updateText()
        }

    var formatArg: String? = null
        set(value) {
            field = value
            updateText()
        }

    private val isForumTimeStamp: Boolean
    private val includeTime: Boolean
    private val defaultMessage: String
    private val hideWhenEmpty: Boolean

    init {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.TimestampView, defStyleAttr, 0)
        try {
            isForumTimeStamp = a.getBoolean(R.styleable.TimestampView_isForumTimestamp, false)
            includeTime = a.getBoolean(R.styleable.TimestampView_includeTime, false)
            defaultMessage = a.getString(R.styleable.TimestampView_emptyMessage) ?: ""
            hideWhenEmpty = a.getBoolean(R.styleable.TimestampView_hideWhenEmpty, false)
            format = a.getString(R.styleable.TimestampView_format) ?: ""
        } finally {
            a.recycle()
        }
        if (maxLines == -1 || maxLines == Integer.MAX_VALUE) {
            maxLines = 1
        }

        updateText()
    }

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

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        return if (superState != null) {
            val savedState = SavedState(superState)
            savedState.timestamp = timestamp
            savedState.format = format
            savedState.formatArg = formatArg
            savedState
        } else {
            superState
        }
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val ss = state as SavedState
        super.onRestoreInstanceState(ss.superState)
        timestamp = ss.timestamp
        format = ss.format
        formatArg = ss.formatArg
    }

    @Synchronized
    @Suppress("DEPRECATION")
    private fun updateText() {
        if (!ViewCompat.isAttachedToWindow(this@TimestampView)) return
        if (timestamp <= 0) {
            if (hideWhenEmpty) visibility = View.GONE
            text = defaultMessage
        } else {
            if (hideWhenEmpty) visibility = View.VISIBLE
            val formattedTimestamp = timestamp.formatTimestamp(context, isForumTimeStamp, includeTime)
            text = if (format.isNotEmpty()) {
                Html.fromHtml(String.format(
                        Html.toHtml(SpannedString(this@TimestampView.format)),
                        formattedTimestamp,
                        formatArg)
                ).trimTrailingWhitespace()
            } else {
                formattedTimestamp
            }
        }
    }

    private fun updateRunning() {
        val running = isVisible && isShown
        if (running != isRunning) {
            if (running) {
                updateText()
                postDelayed(mTickRunnable, TIME_HINT_UPDATE_INTERVAL)
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
                postDelayed(this, TIME_HINT_UPDATE_INTERVAL)
            }
        }
    }

    internal class SavedState : BaseSavedState {
        internal var timestamp: Long = 0
        internal var format: String = ""
        internal var formatArg: String? = null

        constructor(superState: Parcelable) : super(superState)

        constructor(source: Parcel) : super(source) {
            timestamp = source.readLong()
            format = source.readString() ?: ""
            formatArg = source.readString()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeLong(timestamp)
            out.writeString(format)
            out.writeString(formatArg)
        }

        companion object {
            @JvmField
            val CREATOR = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel): SavedState {
                    return SavedState(source)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    companion object {
        private const val TIME_HINT_UPDATE_INTERVAL: Long = 30_000L
    }
}
