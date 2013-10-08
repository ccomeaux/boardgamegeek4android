package com.boardgamegeek.pref;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import com.boardgamegeek.util.ActivityUtils;

public class BuddyPreference extends Preference {

	public BuddyPreference(Context context) {
		super(context);
	}

	public BuddyPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public BuddyPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onClick() {
		ActivityUtils.link(getContext(), "http://boardgamegeek.com/user/" + getSummary());
	}
}
