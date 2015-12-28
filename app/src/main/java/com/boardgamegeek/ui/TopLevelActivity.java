package com.boardgamegeek.ui;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.view.View;

import com.boardgamegeek.R;

public abstract class TopLevelActivity extends DrawerActivity {
	private CharSequence title;
	private CharSequence drawerTitle;
	private ActionBarDrawerToggle drawerToggle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		drawerTitle = getString(R.string.app_name);
		title = getTitle();

		drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close) {
			// TODO: finish and start CAB with the drawer open/close
			public void onDrawerClosed(View view) {
				final ActionBar actionBar = getSupportActionBar();
				actionBar.setTitle(title);
				if (isTitleHidden()) {
					actionBar.setDisplayShowTitleEnabled(false);
					actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
				}
				supportInvalidateOptionsMenu();
			}

			public void onDrawerOpened(View drawerView) {
				final ActionBar actionBar = getSupportActionBar();
				if (isTitleHidden()) {
					actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
					actionBar.setDisplayShowTitleEnabled(true);
				}
				actionBar.setTitle(drawerTitle);
				supportInvalidateOptionsMenu();
			}
		};
		drawerLayout.setDrawerListener(drawerToggle);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		drawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		drawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public void setTitle(CharSequence title) {
		this.title = title;
		getSupportActionBar().setTitle(this.title);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
	}

	protected boolean isTitleHidden() {
		return false;
	}
}
