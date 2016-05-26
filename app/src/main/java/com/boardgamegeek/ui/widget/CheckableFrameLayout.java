package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.FrameLayout;

public class CheckableFrameLayout extends FrameLayout implements Checkable {
	private static final int[] CHECKED_STATE_SET = { android.R.attr.state_checked };
	private boolean isChecked;

	public CheckableFrameLayout(Context context) {
		super(context);
	}

	public CheckableFrameLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CheckableFrameLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public boolean isChecked() {
		return isChecked;
	}

	@Override
	public void setChecked(boolean checked) {
		if (isChecked != checked) {
			isChecked = checked;
			refreshDrawableState();
		}
	}

	@Override
	public void toggle() {
		setChecked(!isChecked);
	}

	@Override
	protected int[] onCreateDrawableState(int extraSpace) {
		final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
		if (isChecked()) {
			mergeDrawableStates(drawableState, CHECKED_STATE_SET);
		}
		return drawableState;
	}
}
