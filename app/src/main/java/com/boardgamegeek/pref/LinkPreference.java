package com.boardgamegeek.pref;

import com.boardgamegeek.util.ActivityUtils;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

public class LinkPreference extends Preference {
	public LinkPreference(Context context) {
		super(context);
	}

	public LinkPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public LinkPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onClick() {
		ActivityUtils.link(getContext(), getSummary().toString());
	}
}
