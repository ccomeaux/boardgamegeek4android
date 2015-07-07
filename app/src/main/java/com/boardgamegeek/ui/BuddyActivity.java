package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.boardgamegeek.events.BuddySelectedEvent;
import com.boardgamegeek.util.ActivityUtils;

import de.greenrobot.event.EventBus;

public class BuddyActivity extends SimpleSinglePaneActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setSubtitle(getIntent().getStringExtra(ActivityUtils.KEY_BUDDY_NAME));

		EventBus.getDefault().removeStickyEvent(BuddySelectedEvent.class);
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new BuddyFragment();
	}
}
