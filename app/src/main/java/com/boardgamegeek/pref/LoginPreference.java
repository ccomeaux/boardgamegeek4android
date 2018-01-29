package com.boardgamegeek.pref;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.util.AttributeSet;

import com.boardgamegeek.auth.AccountUtils;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.ui.LoginActivity;

public class LoginPreference extends Preference {
	public LoginPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		final Intent intent = new Intent(getContext(), LoginActivity.class);
		setIntent(intent);
	}

	@Override
	public boolean isEnabled() {
		return !Authenticator.isSignedIn(getContext());
	}

	@Override
	public CharSequence getSummary() {
		return AccountUtils.getUsername(getContext());
	}
}
