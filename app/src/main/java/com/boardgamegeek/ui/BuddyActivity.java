package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;

import com.boardgamegeek.events.BuddySelectedEvent;
import com.boardgamegeek.util.ActivityUtils;

import de.greenrobot.event.EventBus;

public class BuddyActivity extends SimpleSinglePaneActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		final String name = getIntent().getStringExtra(ActivityUtils.KEY_PLAYER_NAME);
		final String username = getIntent().getStringExtra(ActivityUtils.KEY_BUDDY_NAME);
		setSubtitle(username, name);

		EventBus.getDefault().removeStickyEvent(BuddySelectedEvent.class);
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new BuddyFragment();
	}

	private void setSubtitle(String username, String name) {
		String subtitle;
		if (TextUtils.isEmpty(username)) {
			subtitle = name;
		} else {
			subtitle = username;
		}
		getSupportActionBar().setSubtitle(subtitle);
	}
}
