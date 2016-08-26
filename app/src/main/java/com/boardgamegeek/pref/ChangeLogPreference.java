package com.boardgamegeek.pref;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.Preference;
import android.util.AttributeSet;

import com.boardgamegeek.BuildConfig;

public class ChangeLogPreference extends Preference {
	public ChangeLogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		String changeLogUrl = String.format("https://github.com/ccomeaux/boardgamegeek4android/blob/%s/CHANGELOG.md", BuildConfig.BRANCH);
		final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(changeLogUrl));
		setIntent(intent);
	}
}
