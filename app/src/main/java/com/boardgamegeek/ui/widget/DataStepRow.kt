package com.boardgamegeek.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.StringRes
import com.boardgamegeek.R
import com.boardgamegeek.databinding.WidgetDataStepRowBinding
import com.boardgamegeek.extensions.fadeIn
import com.boardgamegeek.extensions.fadeOut
import com.boardgamegeek.util.FileUtils

@SuppressLint("ViewConstructor")
class DataStepRow(context: Context) : LinearLayout(context) {
    private val binding = WidgetDataStepRowBinding.inflate(LayoutInflater.from(context), this)
    private var type: String? = null
    private var listener: Listener? = null

    interface Listener {
        fun onExportClicked(tag: String?)

        fun onImportClicked(type: String?)
    }

    init {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        gravity = Gravity.CENTER_VERTICAL
        orientation = LinearLayout.VERTICAL
        minimumHeight = resources.getDimensionPixelSize(R.dimen.view_row_height)
        val verticalPadding = resources.getDimensionPixelSize(R.dimen.padding_half)
        setPadding(0, verticalPadding, 0, verticalPadding)

        binding.exportButton.setOnClickListener {
            listener?.onExportClicked(type)
        }
        binding.importButton.setOnClickListener {
            listener?.onImportClicked(type)
        }
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun bind(type: String, @StringRes typeResId: Int, @StringRes descriptionResId: Int) {
        this.type = type
        binding.typeView.setText(typeResId)
        binding.descriptionView.setText(descriptionResId)
        if (FileUtils.shouldUseDefaultFolders()) {
            binding.fileNameView.text = FileUtils.getExportFile(type).toString()
            binding.fileNameView.visibility = View.VISIBLE
        } else {
            binding.fileNameView.visibility = View.GONE
        }
    }

    fun initProgressBar() {
        binding.progressBar.isIndeterminate = false
        binding.progressBar.fadeIn()
        binding.importButton.isEnabled = false
        binding.exportButton.isEnabled = false
    }

    fun updateProgressBar(max: Int, progress: Int) {
        if (max < 0) {
            binding.progressBar.isIndeterminate = true
        } else {
            binding.progressBar.isIndeterminate = false
            binding.progressBar.max = max
            binding.progressBar.progress = progress
        }
    }

    fun hideProgressBar() {
        binding.progressBar.fadeOut()
        binding.importButton.isEnabled = true
        binding.exportButton.isEnabled = true
    }
}
