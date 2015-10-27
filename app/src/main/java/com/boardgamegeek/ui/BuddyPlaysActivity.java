package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.events.PlaySelectedEvent;
import com.boardgamegeek.events.PlaysCountChangedEvent;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ToolbarUtils;

import hugo.weaving.DebugLog;

public class BuddyPlaysActivity extends SimpleSinglePaneActivity {
	private String mBuddyName;
	private int mCount = -1;

	@DebugLog
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mBuddyName = getIntent().getStringExtra(ActivityUtils.KEY_BUDDY_NAME);

		if (!TextUtils.isEmpty(mBuddyName)) {
			ActionBar bar = getSupportActionBar();
			bar.setSubtitle(mBuddyName);
		}
	}

	@DebugLog
	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new PlaysFragment();
	}

	@Override
	protected Bundle onBeforeArgumentsSet(Bundle arguments) {
		arguments.putInt(PlaysFragment.KEY_MODE, PlaysFragment.MODE_BUDDY);
		return arguments;
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.text_only;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		String countDescription = mCount <= 0 ? "" : String.valueOf(mCount);
		ToolbarUtils.setActionBarText(menu, R.id.menu_text, countDescription);
		return super.onPrepareOptionsMenu(menu);
	}

	@DebugLog
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				ActivityUtils.navigateUpToBuddy(this, mBuddyName);
				finish();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@DebugLog
	public void onEvent(PlaySelectedEvent event) {
		ActivityUtils.startPlayActivity(this, event.getPlayId(), event.getGameId(), event.getGameName(), event.getThumbnailUrl(), event.getImageUrl());
	}

	@DebugLog
	public void onEvent(PlaysCountChangedEvent event) {
		mCount = event.getCount();
		supportInvalidateOptionsMenu();
	}
}
