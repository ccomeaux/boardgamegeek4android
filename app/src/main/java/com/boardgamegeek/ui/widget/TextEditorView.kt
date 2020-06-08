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
import com.boardgamegeek.extensions.setSelectableBackground
import com.boardgamegeek.extensions.setTextMaybeHtml
import com.boardgamegeek.util.XmlApiMarkupConverter
import kotlinx.android.synthetic.main.widget_text_editor.view.*

class TextEditorView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0) : ForegroundLinearLayout(context, attrs, defStyleAttr) {

    private val markupConverter = XmlApiMarkupConverter(context)

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_text_editor, this, true)

        visibility = View.GONE

        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = resources.getDimensionPixelSize(R.dimen.edit_row_height)
        orientation = HORIZONTAL
        this.setSelectableBackground()

        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.TextEditorView, defStyleAttr, 0)
            try {
                headerView.text = a.getString(R.styleable.TextEditorView_headerText)
            } finally {
                a.recycle()
            }
        }
    }

    private var isEditMode: Boolean = false

    val contentText: String
        get() = contentView.tag.toString()

    val headerText: String
        get() = headerView.text.toString()

    fun setContent(text: CharSequence, timestamp: Long) {
        contentView.tag = text
        contentView.setTextMaybeHtml(markupConverter.toHtml(text.toString()), HtmlCompat.FROM_HTML_MODE_COMPACT)
        contentView.isVisible = text.isNotBlank()

        timestampView.timestamp = timestamp
        setEditMode()
    }

    fun setHeaderColor(swatch: Palette.Swatch) {
        headerView.setTextColor(swatch.rgb)
    }

    fun enableEditMode(enable: Boolean) {
        isEditMode = enable
        setEditMode()
    }

    private fun setEditMode() {
        imageView.isVisible = isEditMode
        isVisible = isEditMode || !contentView.text.isNullOrBlank() || timestampView.timestamp != 0L
        isClickable = isEditMode
    }
}
