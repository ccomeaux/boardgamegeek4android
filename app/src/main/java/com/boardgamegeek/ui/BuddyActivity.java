package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;

import com.boardgamegeek.events.BuddySelectedEvent;
import com.boardgamegeek.tasks.BuddyNicknameUpdateTask;
import com.boardgamegeek.util.ActivityUtils;

import de.greenrobot.event.EventBus;

public class BuddyActivity extends SimpleSinglePaneActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
		final String name = getIntent().getStringExtra(ActivityUtils.KEY_PLAYER_NAME);
		final String username = getIntent().getStringExtra(ActivityUtils.KEY_BUDDY_NAME);
		setSubtitle(username, name);

		EventBus.getDefault().removeStickyEvent(BuddySelectedEvent.class);
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new BuddyFragment();
	}

	@SuppressWarnings("unused")
	public void onEvent(BuddyNicknameUpdateTask.Event event) {
		Snackbar.make(rootContainer, event.getMessage(), Snackbar.LENGTH_LONG).show();
	}

	private void setSubtitle(String username, String name) {
		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			String subtitle;
			if (TextUtils.isEmpty(username)) {
				subtitle = name;
			} else {
				subtitle = username;
			}
			actionBar.setSubtitle(subtitle);
		}
	}
}
