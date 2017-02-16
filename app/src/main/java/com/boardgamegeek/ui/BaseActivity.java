package com.boardgamegeek.ui;

import android.support.annotation.MenuRes;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.events.UpdateErrorEvent;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.UIUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import hugo.weaving.DebugLog;

/**
 * Registers/unregisters a sticky event bus, with a default error handler (toast)
 * Provides common menu functions:
 * 1. Cancel sync
 * 2. Toggling navigation drawer
 * 3. Inflation helper.
 * Subtitle setter
 */
public abstract class BaseActivity extends AppCompatActivity {
	@DebugLog
	@Override
	protected void onStart() {
		super.onStart();
		EventBus.getDefault().register(this);
	}

	@DebugLog
	@Override
	protected void onStop() {
		EventBus.getDefault().unregister(this);
		super.onStop();
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(UpdateErrorEvent event) {
		Toast.makeText(this, event.getMessage(), Toast.LENGTH_LONG).show();
	}

	@MenuRes
	protected int getOptionsMenuId() {
		return 0;
	}

	@DebugLog
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.base, menu);
		if (getOptionsMenuId() != 0) {
			menuInflater.inflate(getOptionsMenuId(), menu);
		}
		return true;
	}

	@DebugLog
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		UIUtils.showMenuItem(menu, R.id.menu_cancel_sync, SyncService.isActiveOrPending(this));
		return super.onPrepareOptionsMenu(menu);
	}

	@DebugLog
	protected void setSubtitle(String text) {
		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setSubtitle(text);
		}
	}

	@DebugLog
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				if (this instanceof TopLevelActivity) {
					// bug in ActionBarDrawerToggle
					return false;
				}
				NavUtils.navigateUpFromSameTask(this);
				return true;
			case R.id.menu_cancel_sync:
				SyncService.cancelSync(this);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}