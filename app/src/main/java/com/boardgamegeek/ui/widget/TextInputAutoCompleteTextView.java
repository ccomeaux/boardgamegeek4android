package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.support.v7.widget.WithHint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewParent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

/**
 * A special sub-class of {@link android.widget.EditText} designed for use as a child of
 * {@link TextInputLayout}.
 * <p>
 * <p>Using this class allows us to display a hint in the IME when in 'extract' mode.</p>
 */
public class TextInputAutoCompleteTextView extends AppCompatAutoCompleteTextView {

	public TextInputAutoCompleteTextView(Context context) {
		super(context);
	}

	public TextInputAutoCompleteTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public TextInputAutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
		final InputConnection ic = super.onCreateInputConnection(outAttrs);
		if (ic != null && outAttrs.hintText == null) {
			// If we don't have a hint and our parent implements WithHint, use its hint for the
			// EditorInfo. This allows us to display a hint in 'extract mode'.
			ViewParent parent = getParent();
			while (parent instanceof View) {
				if (parent instanceof WithHint) {
					outAttrs.hintText = ((WithHint) parent).getHint();
					break;
				}
				parent = parent.getParent();
			}
		}
		return ic;
	}
}
