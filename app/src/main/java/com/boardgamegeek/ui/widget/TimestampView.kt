package com.boardgamegeek.ui.widget

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.text.Html
import android.text.SpannedString
import android.util.AttributeSet
import android.view.View
import androidx.core.view.ViewCompat
import com.boardgamegeek.R
import com.boardgamegeek.extensions.formatTimestamp
import com.boardgamegeek.extensions.trimTrailingWhitespace

class TimestampView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = android.R.attr.textViewStyle
) : SelfUpdatingView(context, attrs, defStyleAttr) {

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
    override fun updateText() {
        if (!ViewCompat.isAttachedToWindow(this@TimestampView)) return
        if (timestamp <= 0) {
            if (hideWhenEmpty) visibility = View.GONE
            text = defaultMessage
        } else {
            if (hideWhenEmpty) visibility = View.VISIBLE
            val formattedTimestamp = timestamp.formatTimestamp(context, isForumTimeStamp, includeTime)
            text = if (format.isNotEmpty()) {
                @Suppress("DEPRECATION")
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
}
