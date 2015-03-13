package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.sorter.LocationsSorterFactory;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ToolbarUtils;

import hugo.weaving.DebugLog;

public class LocationsActivity extends TopLevelSinglePaneActivity implements LocationsFragment.Callbacks {
	private static final String KEY_COUNT = "KEY_COUNT";
	private int mCount = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			mCount = savedInstanceState.getInt(KEY_COUNT);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(KEY_COUNT, mCount);
	}

	@Override
	protected Fragment onCreatePane() {
		return new LocationsFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.locations;
	}

	@DebugLog
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (isDrawerOpen()) {
			menu.findItem(R.id.menu_sort).setVisible(false);
			ToolbarUtils.setActionBarText(menu, R.id.menu_list_count, "");
		} else {
			menu.findItem(R.id.menu_sort).setVisible(true);
			LocationsFragment fragment = (LocationsFragment) getFragment();
			if (fragment != null) {
				if (fragment.getSort() == LocationsSorterFactory.TYPE_QUANTITY) {
					menu.findItem(R.id.menu_sort_quantity).setChecked(true);
				} else {
					menu.findItem(R.id.menu_sort_name).setChecked(true);
				}
			}
			ToolbarUtils.setActionBarText(menu, R.id.menu_list_count,
				mCount <= 0 ? "" : String.valueOf(mCount));
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@DebugLog
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
			case R.id.menu_sort_name:
				((LocationsFragment) getFragment()).setSort(LocationsSorterFactory.TYPE_NAME);
				return true;
			case R.id.menu_sort_quantity:
				((LocationsFragment) getFragment()).setSort(LocationsSorterFactory.TYPE_QUANTITY);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected int getDrawerResId() {
		return R.string.title_locations;
	}

	@DebugLog
	@Override
	public boolean onLocationSelected(String name) {
		Intent intent = new Intent(this, LocationActivity.class);
		intent.putExtra(ActivityUtils.KEY_LOCATION_NAME, name);
		startActivity(intent);
		return true;
	}

	@Override
	public void onLocationCountChanged(int count) {
		mCount = count;
		supportInvalidateOptionsMenu();
	}
}