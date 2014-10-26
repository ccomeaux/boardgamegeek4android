package com.boardgamegeek.ui;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.BuddyUtils;

public class BuddiesActivity extends TopLevelSinglePaneActivity implements BuddiesFragment.Callbacks {
	private static final String KEY_COUNT = "KEY_COUNT";
	private Menu mOptionsMenu;
	private Object mSyncObserverHandle;
	private int mCount = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			mCount = savedInstanceState.getInt(KEY_COUNT);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(KEY_COUNT, mCount);
	}

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
		return new BuddiesFragment();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		mOptionsMenu = menu;
		mSyncStatusObserver.onStatusChanged(0);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		ActivityUtils.setActionBarText(menu, R.id.menu_list_count,
			(isDrawerOpen() || mCount <= 0) ? "" : String.valueOf(mCount));
		menu.findItem(R.id.menu_refresh).setVisible(!isDrawerOpen());
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_refresh:
				SyncService.sync(this, SyncService.FLAG_SYNC_BUDDIES);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.buddies;
	}

	@Override
	protected int getDrawerResId() {
		return R.string.title_buddies;
	}

	@Override
	public boolean onBuddySelected(int buddyId, String name, String fullName) {
		Intent intent = new Intent(this, BuddyActivity.class);
		intent.putExtra(BuddyUtils.KEY_BUDDY_NAME, name);
		startActivity(intent);
		return false;
	}

	@Override
	public void onBuddyCountChanged(int count) {
		mCount = count;
		supportInvalidateOptionsMenu();
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
					setRefreshActionButtonState(SyncService.isActiveOrPending(BuddiesActivity.this));
				}
			});
		}
	};
}
