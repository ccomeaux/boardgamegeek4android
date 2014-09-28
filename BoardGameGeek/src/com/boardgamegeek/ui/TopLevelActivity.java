package com.boardgamegeek.ui;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.view.ActionProvider;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.SubMenu;
import android.view.View;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
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

		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open,
			R.string.drawer_close) {
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
		if (mDrawerToggle.onOptionsItemSelected(getMenuItem(item))) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	protected boolean isTitleHidden() {
		return false;
	}

	private android.view.MenuItem getMenuItem(final MenuItem item) {
		return new android.view.MenuItem() {
			@Override
			public int getItemId() {
				return item.getItemId();
			}

			public boolean isEnabled() {
				return item.isEnabled();
			}

			@Override
			public boolean collapseActionView() {
				return item.collapseActionView();
			}

			@Override
			public boolean expandActionView() {
				return expandActionView();
			}

			@Override
			public ActionProvider getActionProvider() {
				return null;
			}

			@Override
			public View getActionView() {
				return item.getActionView();
			}

			@Override
			public char getAlphabeticShortcut() {
				return item.getAlphabeticShortcut();
			}

			@Override
			public int getGroupId() {
				return item.getGroupId();
			}

			@Override
			public Drawable getIcon() {
				return item.getIcon();
			}

			@Override
			public Intent getIntent() {
				return item.getIntent();
			}

			@Override
			public ContextMenuInfo getMenuInfo() {
				return item.getMenuInfo();
			}

			@Override
			public char getNumericShortcut() {
				return item.getNumericShortcut();
			}

			@Override
			public int getOrder() {
				return item.getOrder();
			}

			@Override
			public SubMenu getSubMenu() {
				return null;
			}

			@Override
			public CharSequence getTitle() {
				return item.getTitle();
			}

			@Override
			public CharSequence getTitleCondensed() {
				return item.getTitleCondensed();
			}

			@Override
			public boolean hasSubMenu() {
				return item.hasSubMenu();
			}

			@Override
			public boolean isActionViewExpanded() {
				return item.isActionViewExpanded();
			}

			@Override
			public boolean isCheckable() {
				return item.isCheckable();
			}

			@Override
			public boolean isChecked() {
				return item.isChecked();
			}

			@Override
			public boolean isVisible() {
				return item.isVisible();
			}

			@Override
			public android.view.MenuItem setActionProvider(ActionProvider actionProvider) {
				return null;
			}

			@Override
			public android.view.MenuItem setActionView(View view) {
				item.setActionView(view);
				return this;
			}

			@Override
			public android.view.MenuItem setActionView(int resId) {
				item.setActionView(resId);
				return this;
			}

			@Override
			public android.view.MenuItem setAlphabeticShortcut(char alphaChar) {
				item.setAlphabeticShortcut(alphaChar);
				return this;
			}

			@Override
			public android.view.MenuItem setCheckable(boolean checkable) {
				item.setCheckable(checkable);
				return this;
			}

			@Override
			public android.view.MenuItem setChecked(boolean checked) {
				item.setChecked(checked);
				return this;
			}

			@Override
			public android.view.MenuItem setEnabled(boolean enabled) {
				item.setEnabled(enabled);
				return this;
			}

			@Override
			public android.view.MenuItem setIcon(Drawable icon) {
				item.setIcon(icon);
				return this;
			}

			@Override
			public android.view.MenuItem setIcon(int iconRes) {
				item.setIcon(iconRes);
				return this;
			}

			@Override
			public android.view.MenuItem setIntent(Intent intent) {
				item.setIntent(intent);
				return this;
			}

			@Override
			public android.view.MenuItem setNumericShortcut(char numericChar) {
				item.setNumericShortcut(numericChar);
				return this;
			}

			@Override
			public android.view.MenuItem setOnActionExpandListener(OnActionExpandListener listener) {
				return null;
			}

			@Override
			public android.view.MenuItem setOnMenuItemClickListener(OnMenuItemClickListener menuItemClickListener) {
				return null;
			}

			@Override
			public android.view.MenuItem setShortcut(char numericChar, char alphaChar) {
				item.setShortcut(numericChar, alphaChar);
				return this;
			}

			@Override
			public void setShowAsAction(int actionEnum) {
				item.setShowAsAction(actionEnum);
			}

			@Override
			public android.view.MenuItem setShowAsActionFlags(int actionEnum) {
				item.setShowAsActionFlags(actionEnum);
				return this;
			}

			@Override
			public android.view.MenuItem setTitle(CharSequence title) {
				item.setTitle(title);
				return this;
			}

			@Override
			public android.view.MenuItem setTitle(int title) {
				item.setTitle(title);
				return this;
			}

			@Override
			public android.view.MenuItem setTitleCondensed(CharSequence title) {
				item.setTitleCondensed(title);
				return this;
			}

			@Override
			public android.view.MenuItem setVisible(boolean visible) {
				item.setVisible(visible);
				return this;
			}
		};
	}
}
