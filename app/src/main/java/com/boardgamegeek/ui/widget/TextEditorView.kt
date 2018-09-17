package com.boardgamegeek.ui.widget

import android.content.Context
import android.support.v7.graphics.Palette
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.boardgamegeek.R
import com.boardgamegeek.extensions.setSelectableBackground
import com.boardgamegeek.extensions.setTextOrHide
import kotlinx.android.synthetic.main.widget_text_editor.view.*

class TextEditorView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0) : ForegroundLinearLayout(context, attrs, defStyleAttr) {

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_text_editor, this, true)

        visibility = View.GONE

        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = resources.getDimensionPixelSize(R.dimen.edit_row_height)
        orientation = LinearLayout.HORIZONTAL
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
        get() = contentView.text.toString()

    val headerText: String
        get() = headerView.text.toString()

    fun setContent(text: CharSequence, timestamp: Long) {
        contentView.setTextOrHide(text)
        timestampView.setTimestamp(timestamp)
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
        if (isEditMode) {
            imageView.visibility = View.VISIBLE
            visibility = View.VISIBLE
            isClickable = true
        } else {
            imageView.visibility = View.GONE
            visibility = if (contentView.text.isNullOrBlank() && timestampView.getTimestamp() == 0L) View.GONE else View.VISIBLE
            isClickable = false
        }
    }
}
