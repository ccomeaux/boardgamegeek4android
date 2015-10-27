package com.boardgamegeek.ui;

import android.support.v4.app.Fragment;
import android.view.Menu;

import com.boardgamegeek.R;
import com.boardgamegeek.events.PlaySelectedEvent;
import com.boardgamegeek.events.PlaysCountChangedEvent;
import com.boardgamegeek.events.PlaysFilterChangedEvent;
import com.boardgamegeek.events.PlaysSortChangedEvent;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ToolbarUtils;

import hugo.weaving.DebugLog;

public class PlaysActivity extends TopLevelSinglePaneActivity {
	private int mCount;
	private String mSortName;

	@Override
	protected Fragment onCreatePane() {
		return new PlaysFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.plays;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean hide = (isDrawerOpen() || mCount <= 0);
		ToolbarUtils.setActionBarText(menu, R.id.menu_list_count,
			hide ? "" : String.valueOf(mCount),
			hide ? "" : mSortName);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	protected int getDrawerResId() {
		return R.string.title_plays;
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

	@DebugLog
	public void onEvent(PlaysFilterChangedEvent event) {
		if (event.getType() == Play.SYNC_STATUS_ALL) {
			getSupportActionBar().setSubtitle("");
		} else {
			getSupportActionBar().setSubtitle(event.getDescription());
		}
	}

	@DebugLog
	public void onEvent(PlaysSortChangedEvent event) {
		mSortName = event.getDescription();
		supportInvalidateOptionsMenu();
	}
}
