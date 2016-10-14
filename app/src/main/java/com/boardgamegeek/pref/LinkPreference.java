package com.boardgamegeek.pref;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.Preference;
import android.util.AttributeSet;

public class LinkPreference extends Preference {
	public LinkPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(getSummary().toString())));
	}
}
