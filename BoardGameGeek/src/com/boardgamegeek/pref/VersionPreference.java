package com.boardgamegeek.pref;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import com.boardgamegeek.util.HelpUtils;

public class VersionPreference extends Preference {

	public VersionPreference(Context context) {
		super(context);
	}

	public VersionPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public VersionPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public CharSequence getSummary() {
		return HelpUtils.getVersionName(getContext());
	}
}
