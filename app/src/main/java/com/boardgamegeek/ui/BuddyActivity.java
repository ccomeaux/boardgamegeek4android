package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.events.BuddySelectedEvent;
import com.boardgamegeek.events.UpdateCompleteEvent;
import com.boardgamegeek.events.UpdateEvent;
import com.boardgamegeek.service.UpdateService;
import com.boardgamegeek.util.ActivityUtils;

import de.greenrobot.event.EventBus;

public class BuddyActivity extends SimpleSinglePaneActivity {
	private boolean mSyncing = false;
	private Menu mOptionsMenu;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setSubtitle(getIntent().getStringExtra(ActivityUtils.KEY_BUDDY_NAME));

		EventBus.getDefault().removeStickyEvent(BuddySelectedEvent.class);
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new BuddyFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.refresh_only;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		mOptionsMenu = menu;
		updateRefreshStatus();
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_refresh:
				BuddyFragment fragment = (BuddyFragment) getFragment();
				if (fragment != null) {
					fragment.forceRefresh();
				}
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void onEventMainThread(UpdateEvent event) {
		mSyncing = event.type == UpdateService.SYNC_TYPE_BUDDY;
		updateRefreshStatus();
	}

	public void onEventMainThread(UpdateCompleteEvent event) {
		mSyncing = false;
		updateRefreshStatus();
	}

	private void updateRefreshStatus() {
		if (mOptionsMenu == null) {
			return;
		}

		final MenuItem refreshItem = mOptionsMenu.findItem(R.id.menu_refresh);
		if (refreshItem != null) {
			if (mSyncing) {
				MenuItemCompat.setActionView(refreshItem, R.layout.actionbar_indeterminate_progress);
			} else {
				MenuItemCompat.setActionView(refreshItem, null);
			}
		}
	}
}
