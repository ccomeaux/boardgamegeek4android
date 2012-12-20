package com.boardgamegeek.ui;

import android.content.ContentResolver;
import android.content.SyncStatusObserver;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.ArrayAdapter;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.boardgamegeek.R;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.service.SyncService2;

public class PlaysActivity extends SimpleSinglePaneActivity implements ActionBar.OnNavigationListener {
	private Menu mOptionsMenu;
	private Object mSyncObserverHandle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		ArrayAdapter<CharSequence> mSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.plays_filter,
			R.layout.sherlock_spinner_item);
		mSpinnerAdapter.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
		actionBar.setListNavigationCallbacks(mSpinnerAdapter, this);
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
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		int filter = Play.SYNC_STATUS_ALL;
		switch (itemPosition) {
			case 1:
				filter = Play.SYNC_STATUS_SYNCED;
				break;
			case 2:
				filter = Play.SYNC_STATUS_IN_PROGRESS;
				break;
			case 3:
				filter = Play.SYNC_STATUS_PENDING_UPDATE;
				break;
			case 4:
				filter = Play.SYNC_STATUS_PENDING_DELETE;
				break;
		}
		((PlaysFragment) mFragment).filter(filter);
		return true;
	}

	@Override
	protected Fragment onCreatePane() {
		return new PlaysFragment();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		mOptionsMenu = menu;
		mSyncStatusObserver.onStatusChanged(0);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.plays;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_refresh:
				((PlaysFragment) mFragment).triggerRefresh();
				return true;
		}
		return super.onOptionsItemSelected(item);
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
					setRefreshActionButtonState(SyncService2.isActiveOrPending(PlaysActivity.this));
				}
			});
		}
	};
}
