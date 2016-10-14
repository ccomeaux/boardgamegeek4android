package com.boardgamegeek.pref;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.util.AttributeSet;

import com.boardgamegeek.R;
import com.boardgamegeek.util.ActivityUtils;

public class ContactUsPreference extends Preference {
	public ContactUsPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		Intent emailIntent = new Intent(Intent.ACTION_SEND);
		emailIntent.setType("text/email");
		emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { getContext().getString(R.string.pref_about_contact_us_summary) });
		emailIntent.putExtra(Intent.EXTRA_SUBJECT, R.string.pref_feedback_title);
		if (ActivityUtils.isIntentAvailable(getContext(), emailIntent)) {
			setIntent(emailIntent);
		}
	}
}
