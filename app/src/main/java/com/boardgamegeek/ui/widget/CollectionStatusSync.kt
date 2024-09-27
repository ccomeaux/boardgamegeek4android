package com.boardgamegeek.ui.widget

import android.content.Context
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import com.boardgamegeek.R
import com.boardgamegeek.extensions.formatDateTime
import com.google.android.material.progressindicator.LinearProgressIndicator

class CollectionStatusSync @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {
    private var switchListener: ((Boolean) -> Unit)? = null
    private var clickListener: (() -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_collection_sync, this)

        context.withStyledAttributes(attrs, R.styleable.CollectionStatusSync, defStyleAttr) {
            findViewById<SwitchCompat>(R.id.statusSwitch).text = getString(R.styleable.CollectionStatusSync_switchLabel)
        }

        findViewById<SwitchCompat>(R.id.statusSwitch).setOnCheckedChangeListener { _, isChecked ->
            findViewById<View>(R.id.defaultContainer).isVisible = isChecked
            findViewById<View>(R.id.accessoryContainer).isVisible = isChecked
            findViewById<View>(R.id.syncStatusButton).isVisible = isChecked
            switchListener?.invoke(isChecked)
        }

        findViewById<Button>(R.id.syncStatusButton).setOnClickListener {
            clickListener?.invoke()
        }
    }

    fun setEnableListener(listener: (Boolean) -> Unit) {
        switchListener = listener
    }

    fun setProgress(inProgress: Boolean, isOtherInProgress: Boolean) {
        findViewById<LinearProgressIndicator>(R.id.progressIndicator).isVisible = inProgress
        findViewById<Button>(R.id.syncStatusButton).isEnabled = !inProgress && !isOtherInProgress
        findViewById<SwitchCompat>(R.id.statusSwitch).isEnabled = !inProgress && !isOtherInProgress
    }

    fun check(enabled: Boolean) {
        findViewById<SwitchCompat>(R.id.statusSwitch).isChecked = enabled
    }

    fun setDefaultTimestamp(timestamp: Long?) {
        findViewById<TextView>(R.id.defaultTimestampView).text = (timestamp ?: 0L).formatDateTime()
    }

    fun setAccessoryTimestamp(timestamp: Long?) {
        findViewById<TextView>(R.id.accessoryTimestampView).text = (timestamp ?: 0L).formatDateTime()
    }

    fun onSyncClick(listener: () -> Unit) {
        clickListener = listener
    }

    fun Long.formatDateTime() = formatDateTime(
        context,
        flags = DateUtils.FORMAT_ABBREV_ALL or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_YEAR
    )
}