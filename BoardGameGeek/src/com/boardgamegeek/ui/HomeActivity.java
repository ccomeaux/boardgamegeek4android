package com.boardgamegeek.ui;

import android.content.ContentResolver;
import android.content.SyncStatusObserver;
import android.os.Bundle;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.boardgamegeek.R;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.HelpUtils;
import com.boardgamegeek.util.UIUtils;

public class HomeActivity extends BaseActivity {
	private static final int HELP_VERSION = 2;
	private Menu mOptionsMenu;
	private Object mSyncObserverHandle;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
		UIUtils.showHelpDialog(this, HelpUtils.HELP_HOME_KEY, HELP_VERSION, R.string.help_home);
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
	protected int getOptionsMenuId() {
		return R.menu.home;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		mOptionsMenu = menu;
		mSyncStatusObserver.onStatusChanged(0);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.menu_cancel_sync).setVisible(SyncService.isActiveOrPending(this));
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_refresh:
				triggerRefresh();
				return true;
			case R.id.menu_cancel_sync:
				SyncService.cancelSync(this);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void triggerRefresh() {
		SyncService.sync(this, SyncService.FLAG_SYNC_ALL);
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
					setRefreshActionButtonState(SyncService.isActiveOrPending(HomeActivity.this));
				}
			});
		}
	};
}