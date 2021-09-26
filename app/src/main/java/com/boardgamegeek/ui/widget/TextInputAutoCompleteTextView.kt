package com.boardgamegeek.ui.widget

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout

/**
 * A special sub-class of AppCompatAutoCompleteTextView designed for use as a child of
 * [TextInputLayout].
 *
 * Using this class allows us to display a hint in the IME when in 'extract' mode.
 */
class TextInputAutoCompleteTextView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = android.R.attr.autoCompleteTextViewStyle)
    : AppCompatAutoCompleteTextView(context, attrs, defStyleAttr) {

    override fun enoughToFilter(): Boolean {
        return true
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (focused && filter != null) {
            performFiltering(text, 0);
        }
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val inputConnection = super.onCreateInputConnection(outAttrs)
        if (inputConnection != null && outAttrs.hintText == null) {
            // If we don't have a hint and our parent us a TextInputLayout, use its hint for the
            // EditorInfo. This allows us to display a hint in 'extract mode'.
            var parent = parent
            while (parent is View) {
                if (parent is TextInputLayout) {
                    outAttrs.hintText = parent.hint
                    break
                }
                parent = parent.getParent()
            }
        }
        return inputConnection
    }
}
