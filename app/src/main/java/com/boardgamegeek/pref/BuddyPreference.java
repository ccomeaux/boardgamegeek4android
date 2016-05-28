package com.boardgamegeek.pref;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.util.AttributeSet;

import com.boardgamegeek.ui.BuddyActivity;
import com.boardgamegeek.util.ActivityUtils;

public class BuddyPreference extends Preference {

	public BuddyPreference(Context context) {
		super(context);
		init();
	}

	public BuddyPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public BuddyPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		Intent intent = new Intent(getContext(), BuddyActivity.class);
		intent.putExtra(ActivityUtils.KEY_BUDDY_NAME, getSummary());
		setIntent(intent);
	}
}
