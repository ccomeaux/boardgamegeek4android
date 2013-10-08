package com.boardgamegeek.pref;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import com.boardgamegeek.util.ActivityUtils;

public class BuddyPreference extends Preference {
	private Context mContext;

	public BuddyPreference(Context context) {
		super(context);
		mContext = context;
	}

	public BuddyPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}

	public BuddyPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
	}

	@Override
	protected void onClick() {
		ActivityUtils.link(mContext, "http://boardgamegeek.com/user/" + getSummary());
	}
}
