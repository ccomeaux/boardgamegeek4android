package com.boardgamegeek.ui;

import android.app.SearchManager;
import android.content.Intent;
import android.support.v4.app.NavUtils;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.pref.Preferences;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.HelpUtils;

public abstract class BaseActivity extends SherlockFragmentActivity {
	protected int getOptionsMenuId() {
		return 0;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater menuInflater = getSupportMenuInflater();
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
			case R.id.menu_settings:
				startActivity(new Intent(this, Preferences.class));
				return true;
			case R.id.menu_about:
				HelpUtils.showAboutDialog(this);
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
			SearchView searchView = (SearchView) searchItem.getActionView();
			if (searchView != null) {
				SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
				searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
			}
		}
	}
}