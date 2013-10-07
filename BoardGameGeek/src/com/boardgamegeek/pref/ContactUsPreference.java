package com.boardgamegeek.pref;

import com.boardgamegeek.R;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.util.AttributeSet;

public class ContactUsPreference extends Preference {
	private Context mContext;

	public ContactUsPreference(Context context) {
		super(context);
		mContext = context;
	}

	public ContactUsPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}

	public ContactUsPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
	}

	@Override
	protected void onClick() {
		Intent emailIntent = new Intent(Intent.ACTION_SEND);
		emailIntent.setType("text/email");
		emailIntent.putExtra(Intent.EXTRA_EMAIL,
			new String[] { mContext.getString(R.string.pref_about_contact_us_summary) });
		emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback");
		mContext.startActivity(emailIntent);
	}
}
