package com.boardgamegeek.ui;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.BuddyUtils;

public class BuddiesActivity extends SimpleSinglePaneActivity implements BuddiesFragment.Callbacks {
	private Menu mOptionsMenu;
	private Object mSyncObserverHandle;

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
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_refresh:
				triggerRefresh();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.refresh_only;
	}

	@Override
	public boolean onBuddySelected(int buddyId, String name, String fullName) {
		Intent intent = new Intent(Intent.ACTION_VIEW, Buddies.buildBuddyUri(buddyId));
		intent.putExtra(BuddyUtils.KEY_BUDDY_NAME, name);
		intent.putExtra(BuddyUtils.KEY_BUDDY_FULL_NAME, fullName);
		startActivity(intent);
		return false;
	}

	private void triggerRefresh() {
		SyncService.sync(this, SyncService.FLAG_SYNC_BUDDIES);
	}

	private void setRefreshActionButtonState(boolean refreshing) {
		if (mOptionsMenu == null) {
			return;
		}

		final MenuItem refreshItem = mOptionsMenu.findItem(R.id.menu_refresh);
		if (refreshItem != null) {
			if (refreshing) {
				refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
			} else {
				refreshItem.setActionView(null);
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
