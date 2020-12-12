package com.boardgamegeek.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import kotlinx.android.synthetic.main.widget_data_step_row.view.*

@SuppressLint("ViewConstructor")
class DataStepRow(context: Context) : LinearLayout(context) {
    private var type: String? = null
    private var listener: Listener? = null

    interface Listener {
        fun onExportClicked(type: String?)

        fun onImportClicked(type: String?)
    }

    init {
        LayoutInflater.from(getContext()).inflate(R.layout.widget_data_step_row, this, true)

        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        gravity = Gravity.CENTER_VERTICAL
        orientation = VERTICAL
        minimumHeight = resources.getDimensionPixelSize(R.dimen.view_row_height)
        val verticalPadding = resources.getDimensionPixelSize(R.dimen.padding_half)
        setPadding(0, verticalPadding, 0, verticalPadding)

        exportButton.setOnClickListener {
            listener?.onExportClicked(type)
        }
        importButton.setOnClickListener {
            listener?.onImportClicked(type)
        }
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun bind(type: String, @StringRes typeResId: Int, @StringRes descriptionResId: Int) {
        this.type = type
        typeView.setText(typeResId)
        descriptionView.setText(descriptionResId)
    }

    fun initProgressBar() {
        progressBar.isIndeterminate = false
        progressBar.fadeIn()
        importButton?.isEnabled = false
        exportButton?.isEnabled = false
    }

    fun updateProgressBar(max: Int, progress: Int) {
        if (max < 0) {
            progressBar.isIndeterminate = true
        } else {
            progressBar.isIndeterminate = false
            progressBar.max = max
            progressBar.progress = progress
        }
    }

    fun hideProgressBar() {
        progressBar.fadeOut()
        importButton?.isEnabled = true
        exportButton?.isEnabled = true
    }
}
