package com.boardgamegeek.pref;

import com.boardgamegeek.R;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

public class LicensePreference extends DialogPreference {

	public LicensePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public LicensePreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		setDialogLayoutResource(R.layout.widget_dialogpreference_textview);
		setPositiveButtonText(R.string.close);
		setNegativeButtonText("");
	}
}
