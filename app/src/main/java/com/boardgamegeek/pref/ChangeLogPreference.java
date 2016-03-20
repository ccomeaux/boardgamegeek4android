package com.boardgamegeek.pref;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import com.boardgamegeek.BuildConfig;
import com.boardgamegeek.util.ActivityUtils;

public class ChangeLogPreference extends Preference {
	public ChangeLogPreference(Context context) {
		super(context);
	}

	public ChangeLogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ChangeLogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onClick() {
		String changeLogUrl = String.format("https://github.com/ccomeaux/boardgamegeek4android/blob/%s/CHANGELOG.md", BuildConfig.BRANCH);
		ActivityUtils.link(getContext(), changeLogUrl);
	}
}
