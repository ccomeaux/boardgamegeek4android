package com.boardgamegeek.pref;

import com.boardgamegeek.R;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.util.AttributeSet;

public class ContactUsPreference extends Preference {
	public ContactUsPreference(Context context) {
		super(context);
	}

	public ContactUsPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ContactUsPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onClick() {
		Intent emailIntent = new Intent(Intent.ACTION_SEND);
		emailIntent.setType("text/email");
		emailIntent.putExtra(Intent.EXTRA_EMAIL,
			new String[] { getContext().getString(R.string.pref_about_contact_us_summary) });
		emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback");
		getContext().startActivity(emailIntent);
	}
}
