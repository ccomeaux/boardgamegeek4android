package com.boardgamegeek.pref;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

import com.boardgamegeek.R;

public abstract class ConfirmDialogPreference extends DialogPreference {
	public ConfirmDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDialogTitle(getDialogTitle() + "?");
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
