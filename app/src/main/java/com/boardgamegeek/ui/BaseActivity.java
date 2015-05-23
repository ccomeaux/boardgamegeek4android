package com.boardgamegeek.ui;

import android.app.SearchManager;
import android.support.v4.app.NavUtils;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.events.UpdateErrorEvent;
import com.boardgamegeek.service.SyncService;

import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;

/**
 * Provide common menu functions.
 * 1. Search
 * 2. Cancel sync
 * 3. Toggling navigation drawer
 * 4. Inflation helper.
 * Also provides a sign out method.
 */
public abstract class BaseActivity extends AppCompatActivity {
	protected int getOptionsMenuId() {
		return 0;
	}

	@DebugLog
	@Override
	protected void onStart() {
		super.onStart();
		EventBus.getDefault().registerSticky(this);
	}

	@DebugLog
	@Override
	protected void onStop() {
		EventBus.getDefault().unregister(this);
		super.onStop();
	}

	@DebugLog
	public void onEventMainThread(UpdateErrorEvent event) {
		Toast.makeText(this, event.message, Toast.LENGTH_LONG).show();
	}

	@DebugLog
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.base, menu);
		if (getOptionsMenuId() != 0) {
			menuInflater.inflate(getOptionsMenuId(), menu);
			setupSearchMenuItem(menu);
		}
		return true;
	}

	@DebugLog
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.menu_cancel_sync).setVisible(SyncService.isActiveOrPending(this));
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

	@DebugLog
	protected void signOut() {
		Authenticator.signOut(this);
	}

	@DebugLog
	private void setupSearchMenuItem(Menu menu) {
		MenuItem searchItem = menu.findItem(R.id.menu_search);
		if (searchItem != null) {
			SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
			if (searchView != null) {
				SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
				searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
			}
		}
	}
}