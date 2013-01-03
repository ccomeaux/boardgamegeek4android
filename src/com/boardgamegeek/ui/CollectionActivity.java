package com.boardgamegeek.ui;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.UIUtils;

public class CollectionActivity extends SimpleSinglePaneActivity implements CollectionFragment.Callbacks {
	private static final int HELP_VERSION = 1;

	private Menu mOptionsMenu;
	private Object mSyncObserverHandle;
	private boolean mShortcut;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mShortcut = "android.intent.action.CREATE_SHORTCUT".equals(getIntent().getAction());

		if (mShortcut) {
			final ActionBar actionBar = getSupportActionBar();
			actionBar.setHomeButtonEnabled(false);
			actionBar.setDisplayHomeAsUpEnabled(false);
			actionBar.setTitle(R.string.menu_create_shortcut);
		}

		UIUtils.showHelpDialog(this, BggApplication.HELP_COLLECTION_KEY, HELP_VERSION, R.string.help_collection);
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
	public boolean onCreateOptionsMenu(Menu menu) {
		mOptionsMenu = menu;
		mSyncStatusObserver.onStatusChanged(0);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	protected Fragment onCreatePane() {
		return new CollectionFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return mShortcut ? 0 : R.menu.search_only;
	}

	@Override
	public boolean onGameSelected(int gameId, String gameName) {
		ActivityUtils.launchGame(this, gameId, gameName);
		return true;
	}

	@Override
	public void onSetShortcut(Intent intent) {
		setResult(RESULT_OK, intent);
		finish();
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
					setRefreshActionButtonState(SyncService.isActiveOrPending(CollectionActivity.this));
				}
			});
		}
	};
}