package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;

import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.util.PreferencesUtils;

import hugo.weaving.DebugLog;

public class HomeActivity extends TopLevelActivity {
	@DebugLog
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = new Intent(this, HotnessActivity.class);
		if (Authenticator.isSignedIn(this)) {
			if (Authenticator.isOldAuth(this)) {
				Authenticator.signOut(this);
			} else {
				String[] statuses = PreferencesUtils.getSyncStatuses(this);
				if (statuses != null && statuses.length > 0) {
					intent = new Intent(this, CollectionActivity.class);
				} else if (PreferencesUtils.getSyncPlays(this)) {
					intent = new Intent(this, PlaysActivity.class);
				} else if (PreferencesUtils.getSyncBuddies(this)) {
					intent = new Intent(this, BuddiesActivity.class);
				}
			}
		}
		startActivity(intent);
		finish();
	}
}