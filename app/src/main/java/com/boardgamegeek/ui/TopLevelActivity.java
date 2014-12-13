package com.boardgamegeek.ui;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.view.View;

import com.boardgamegeek.R;

public abstract class TopLevelActivity extends DrawerActivity {
	private CharSequence mTitle;
	private CharSequence mDrawerTitle;
	private ActionBarDrawerToggle mDrawerToggle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mDrawerTitle = getString(R.string.app_name);
		mTitle = getTitle();

		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {
			// TODO: finish and start CAB with the drawer open/close
			public void onDrawerClosed(View view) {
				final ActionBar actionBar = getSupportActionBar();
				actionBar.setTitle(mTitle);
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
				actionBar.setTitle(mDrawerTitle);
				supportInvalidateOptionsMenu();
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		// TODO open the drawer upon launch until user opens it themselves
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		getSupportActionBar().setTitle(mTitle);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	protected boolean isTitleHidden() {
		return false;
	}
}
