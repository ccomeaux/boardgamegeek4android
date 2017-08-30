package com.boardgamegeek.pref;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.util.AttributeSet;

import com.boardgamegeek.ui.BuddyActivity;

public class BuddyPreference extends Preference {
	public BuddyPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		Intent intent = BuddyActivity.createIntent(context, getSummary().toString(), null);
		setIntent(intent);
	}
}
