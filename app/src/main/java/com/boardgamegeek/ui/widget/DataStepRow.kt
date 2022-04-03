package com.boardgamegeek.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import com.boardgamegeek.R
import com.boardgamegeek.livedata.ProgressData

class DataStepRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {
    init {
        LayoutInflater.from(context).inflate(R.layout.widget_data_step_row, this)

        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        gravity = Gravity.CENTER_VERTICAL
        orientation = VERTICAL
        minimumHeight = resources.getDimensionPixelSize(R.dimen.view_row_height)
        val verticalPadding = resources.getDimensionPixelSize(R.dimen.padding_half)
        setPadding(0, verticalPadding, 0, verticalPadding)

        context.withStyledAttributes(attrs, R.styleable.DataStepRow, defStyleAttr) {
            findViewById<TextView>(R.id.typeView).text = getString(R.styleable.DataStepRow_titleLabel)
            findViewById<TextView>(R.id.descriptionView).text = getString(R.styleable.DataStepRow_descriptionLabel)
        }
    }

    fun onExport(l: OnClickListener) {
        findViewById<Button>(R.id.exportButton).setOnClickListener(l)
    }

    fun onImport(l: OnClickListener) {
        findViewById<Button>(R.id.importButton).setOnClickListener(l)
    }

    fun initProgressBar() {
        findViewById<ProgressBar>(R.id.progressBar).apply {
            isIndeterminate = true
            isVisible = true
        }
        findViewById<Button>(R.id.importButton).isEnabled = false
        findViewById<Button>(R.id.exportButton).isEnabled = false
    }

    fun updateProgressBar(progressData: ProgressData) {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        when (progressData.mode) {
            ProgressData.Mode.OFF -> {
                progressBar.isVisible = false
                findViewById<Button>(R.id.importButton).isEnabled = true
                findViewById<Button>(R.id.exportButton).isEnabled = true
            }
            ProgressData.Mode.INDETERMINATE -> {
                progressBar.isIndeterminate = true
            }
            ProgressData.Mode.DETERMINATE -> {
                progressBar.apply {
                    isIndeterminate = false
                    max = progressData.max
                    progress = progressData.current
                }
            }
        }
    }
}
