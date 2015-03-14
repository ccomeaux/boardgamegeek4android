package com.boardgamegeek.ui;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.events.BuddiesCountChangedEvent;
import com.boardgamegeek.events.BuddySelectedEvent;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ToolbarUtils;

import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;

public class BuddiesActivity extends TopLevelSinglePaneActivity {
	private Menu mOptionsMenu;
	private Object mSyncObserverHandle;
	private int mCount = -1;

	@DebugLog
	@Override
	protected void onStart() {
		super.onStart();
		EventBus.getDefault().registerSticky(this);
	}

	@DebugLog
	@Override
	protected void onResume() {
		super.onResume();
		mSyncStatusObserver.onStatusChanged(0);
		mSyncObserverHandle = ContentResolver.addStatusChangeListener(ContentResolver.SYNC_OBSERVER_TYPE_PENDING
			| ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE, mSyncStatusObserver);
	}

	@DebugLog
	@Override
	protected void onPause() {
		super.onPause();
		if (mSyncObserverHandle != null) {
			ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
			mSyncObserverHandle = null;
		}
	}

	@DebugLog
	@Override
	protected void onStop() {
		EventBus.getDefault().unregister(this);
		super.onStop();
	}

	@DebugLog
	@Override
	protected Fragment onCreatePane() {
		return new BuddiesFragment();
	}

	@DebugLog
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		mOptionsMenu = menu;
		mSyncStatusObserver.onStatusChanged(0);
		return super.onCreateOptionsMenu(menu);
	}

	@DebugLog
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		ToolbarUtils.setActionBarText(menu, R.id.menu_list_count,
			(isDrawerOpen() || mCount <= 0) ? "" : String.valueOf(mCount));
		menu.findItem(R.id.menu_refresh).setVisible(!isDrawerOpen());
		return super.onPrepareOptionsMenu(menu);
	}

	@DebugLog
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_refresh:
				SyncService.sync(this, SyncService.FLAG_SYNC_BUDDIES);
				return true;
		}
		return super.onOptionsItemSelected(item);
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

	@DebugLog
	public void onEvent(BuddiesCountChangedEvent event) {
		mCount = event.count;
		supportInvalidateOptionsMenu();
	}

	@DebugLog
	public void onEvent(BuddySelectedEvent event) {
		Intent intent = new Intent(this, BuddyActivity.class);
		intent.putExtra(ActivityUtils.KEY_BUDDY_NAME, event.buddyName);
		startActivity(intent);
	}

	@DebugLog
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
					setRefreshActionButtonState(SyncService.isActiveOrPending(BuddiesActivity.this));
				}
			});
		}
	};
}
