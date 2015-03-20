package com.boardgamegeek.ui;

import android.content.ContentResolver;
import android.content.SyncStatusObserver;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.events.PlaySelectedEvent;
import com.boardgamegeek.events.PlaysCountChangedEvent;
import com.boardgamegeek.events.PlaysFilterChangedEvent;
import com.boardgamegeek.events.PlaysSortChangedEvent;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ToolbarUtils;

import hugo.weaving.DebugLog;

public class PlaysActivity extends TopLevelSinglePaneActivity {
	private Menu mOptionsMenu;
	private Object mSyncObserverHandle;
	private int mCount;
	private String mSortName;

	@Override
	protected void onPause() {
		super.onPause();
		if (mSyncObserverHandle != null) {
			ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
			mSyncObserverHandle = null;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		mSyncStatusObserver.onStatusChanged(0);
		mSyncObserverHandle = ContentResolver.addStatusChangeListener(ContentResolver.SYNC_OBSERVER_TYPE_PENDING
			| ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE, mSyncStatusObserver);
	}

	@Override
	protected Fragment onCreatePane() {
		return new PlaysFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.plays;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		mOptionsMenu = menu;
		mSyncStatusObserver.onStatusChanged(0);
		return super.onCreateOptionsMenu(menu);
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

	private void setRefreshActionButtonState(boolean refreshing) {
		if (mOptionsMenu == null) {
			return;
		}

		final MenuItem refreshItem = mOptionsMenu.findItem(R.id.menu_refresh);
		if (refreshItem != null) {
			if (refreshing) {
				MenuItemCompat.setActionView(refreshItem, R.layout.actionbar_indeterminate_progress);
			} else {
				MenuItemCompat.setActionView(refreshItem, null);
			}
		}
	}

	private final SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
		@Override
		public void onStatusChanged(int which) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					setRefreshActionButtonState(SyncService.isActiveOrPending(PlaysActivity.this));
				}
			});
		}
	};

	@DebugLog
	public void onEvent(PlaySelectedEvent event) {
		ActivityUtils.startPlayActivity(this, event.playId, event.gameId, event.gameName, event.thumbnailUrl, event.imageUrl);
	}

	@DebugLog
	public void onEvent(PlaysCountChangedEvent event) {
		mCount = event.count;
		supportInvalidateOptionsMenu();
	}

	@DebugLog
	public void onEvent(PlaysFilterChangedEvent event) {
		if (event.type == Play.SYNC_STATUS_ALL) {
			getSupportActionBar().setSubtitle("");
		} else {
			getSupportActionBar().setSubtitle(event.description);
		}
	}

	@DebugLog
	public void onEvent(PlaysSortChangedEvent event) {
		mSortName = event.description;
		supportInvalidateOptionsMenu();
	}
}
