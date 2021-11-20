package com.boardgamegeek.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.withStyledAttributes
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.palette.graphics.Palette
import com.boardgamegeek.R
import com.boardgamegeek.extensions.setSelectableBackground
import com.boardgamegeek.extensions.setTextMaybeHtml
import com.boardgamegeek.util.XmlApiMarkupConverter

class TextEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ForegroundLinearLayout(context, attrs, defStyleAttr, defStyleRes) {
    private val markupConverter = XmlApiMarkupConverter(context)

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_text_editor, this)

        visibility = View.GONE

        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = resources.getDimensionPixelSize(R.dimen.edit_row_height)
        orientation = HORIZONTAL
        this.setSelectableBackground()

        context.withStyledAttributes(attrs, R.styleable.TextEditorView, defStyleAttr, defStyleRes) {
            findViewById<TextView>(R.id.headerView).text = getString(R.styleable.TextEditorView_headerText).orEmpty()
        }
    }

    private var isEditMode: Boolean = false

    val contentText: String
        get() = findViewById<TextView>(R.id.contentView).tag.toString()

    val headerText: String
        get() = findViewById<TextView>(R.id.headerView).text.toString()

    fun setContent(content: CharSequence, timestamp: Long) {
        findViewById<TextView>(R.id.contentView).apply {
            tag = content
            setTextMaybeHtml(markupConverter.toHtml(content.toString()), HtmlCompat.FROM_HTML_MODE_COMPACT)
            isVisible = content.isNotBlank()
        }

        findViewById<TimestampView>(R.id.timestampView).timestamp = timestamp
        setEditMode()
    }

    fun setHeaderColor(swatch: Palette.Swatch) {
        findViewById<TextView>(R.id.headerView).setTextColor(swatch.rgb)
    }

    fun enableEditMode(enable: Boolean) {
        isEditMode = enable
        setEditMode()
    }

    private fun setEditMode() {
        findViewById<ImageView>(R.id.imageView).isVisible = isEditMode
        isVisible = isEditMode ||
                findViewById<TextView>(R.id.contentView).text.isNullOrBlank() ||
                findViewById<TimestampView>(R.id.timestampView).timestamp != 0L
        isClickable = isEditMode
    }
}
