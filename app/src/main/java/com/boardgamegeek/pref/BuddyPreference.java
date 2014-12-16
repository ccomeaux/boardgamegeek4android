package com.boardgamegeek.pref;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.util.AttributeSet;

import com.boardgamegeek.ui.BuddyActivity;
import com.boardgamegeek.util.BuddyUtils;

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
		Intent intent = new Intent(getContext(), BuddyActivity.class);
		intent.putExtra(BuddyUtils.KEY_BUDDY_NAME, getSummary());
		getContext().startActivity(intent);
	}
}
