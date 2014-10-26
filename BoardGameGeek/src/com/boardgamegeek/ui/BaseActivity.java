package com.boardgamegeek.ui;

import android.app.SearchManager;
import android.support.v4.app.NavUtils;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.service.SyncService;

public abstract class BaseActivity extends ActionBarActivity {
	protected int getOptionsMenuId() {
		return 0;
	}

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

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.menu_cancel_sync).setVisible(SyncService.isActiveOrPending(this));
		return super.onPrepareOptionsMenu(menu);
	}

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

	protected void signOut() {
		Authenticator.signOut(this);
	}

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