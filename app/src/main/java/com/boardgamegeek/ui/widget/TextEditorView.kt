package com.boardgamegeek.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.palette.graphics.Palette
import com.boardgamegeek.R
import com.boardgamegeek.databinding.WidgetTextEditorBinding
import com.boardgamegeek.extensions.setSelectableBackground
import com.boardgamegeek.extensions.setTextMaybeHtml
import com.boardgamegeek.util.XmlConverter

class TextEditorView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0) : ForegroundLinearLayout(context, attrs, defStyleAttr) {

    private val binding = WidgetTextEditorBinding.inflate(LayoutInflater.from(context), this, true)
    private val xmlConverter = XmlConverter()

    init {
        visibility = View.GONE

        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = resources.getDimensionPixelSize(R.dimen.edit_row_height)
        orientation = HORIZONTAL
        this.setSelectableBackground()

        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.TextEditorView, defStyleAttr, 0)
            try {
                binding.headerView.text = a.getString(R.styleable.TextEditorView_headerText)
            } finally {
                a.recycle()
            }
        }
    }

    private var isEditMode: Boolean = false

    val contentText: String
        get() = binding.contentView.tag.toString()

    val headerText: String
        get() = binding.headerView.text.toString()

    fun setContent(text: CharSequence, timestamp: Long) {
        binding.contentView.tag = text
        binding.contentView.setTextMaybeHtml(xmlConverter.toHtml(text.toString()), HtmlCompat.FROM_HTML_MODE_COMPACT)
        binding.contentView.isVisible = text.isNotBlank()

        binding.timestampView.timestamp = timestamp
        setEditMode()
    }

    fun setHeaderColor(swatch: Palette.Swatch) {
        binding.headerView.setTextColor(swatch.rgb)
    }

    fun enableEditMode(enable: Boolean) {
        isEditMode = enable
        setEditMode()
    }

    private fun setEditMode() {
        binding.imageView.isVisible = isEditMode
        isVisible = isEditMode || !binding.contentView.text.isNullOrBlank() || binding.timestampView.timestamp != 0L
        isClickable = isEditMode
    }
}
