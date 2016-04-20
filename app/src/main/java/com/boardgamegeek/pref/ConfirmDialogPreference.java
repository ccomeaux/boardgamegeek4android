package com.boardgamegeek.pref;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.TypedValue;

import com.boardgamegeek.R;

public abstract class ConfirmDialogPreference extends DialogPreference {
	public ConfirmDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		TypedValue typedValue = new TypedValue();
		getContext().getTheme().resolveAttribute(android.R.attr.alertDialogIcon, typedValue, true);
		setDialogIcon(typedValue.resourceId);
		setDialogLayoutResource(R.layout.widget_dialogpreference_textview);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			execute();
		}
	}

	protected abstract void execute();
}
