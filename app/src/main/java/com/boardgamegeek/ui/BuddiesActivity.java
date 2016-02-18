package com.boardgamegeek.ui;

import android.support.v4.app.Fragment;
import android.view.Menu;

import com.boardgamegeek.R;
import com.boardgamegeek.events.BuddiesCountChangedEvent;
import com.boardgamegeek.events.BuddySelectedEvent;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ToolbarUtils;

import hugo.weaving.DebugLog;

public class BuddiesActivity extends TopLevelSinglePaneActivity {
	private int numberOfBuddies = -1;

	@DebugLog
	@Override
	protected Fragment onCreatePane() {
		return new BuddiesFragment();
	}

	@DebugLog
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		ToolbarUtils.setActionBarText(menu, R.id.menu_list_count, (isDrawerOpen() || numberOfBuddies <= 0) ? "" : String.valueOf(numberOfBuddies));
		return super.onPrepareOptionsMenu(menu);
	}

	@DebugLog
	@Override
	protected int getOptionsMenuId() {
		return R.menu.buddies;
	}

	@DebugLog
	@Override
	protected int getDrawerResId() {
		return R.string.title_buddies;
	}

	@SuppressWarnings("unused")
	@DebugLog
	public void onEvent(BuddiesCountChangedEvent event) {
		numberOfBuddies = event.getCount();
		supportInvalidateOptionsMenu();
	}

	@SuppressWarnings("unused")
	@DebugLog
	public void onEvent(BuddySelectedEvent event) {
		ActivityUtils.startBuddyActivity(this, event.getBuddyName());
	}
}
