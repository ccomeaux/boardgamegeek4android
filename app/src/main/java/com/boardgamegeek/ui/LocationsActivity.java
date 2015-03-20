package com.boardgamegeek.ui;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.events.LocationSelectedEvent;
import com.boardgamegeek.events.LocationSortChangedEvent;
import com.boardgamegeek.events.LocationsCountChangedEvent;
import com.boardgamegeek.sorter.LocationsSorterFactory;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ToolbarUtils;

import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;

public class LocationsActivity extends TopLevelSinglePaneActivity {
	private int mCount = -1;
	private int mSortType = LocationsSorterFactory.TYPE_DEFAULT;

	@DebugLog
	@Override
	protected Fragment onCreatePane() {
		return new LocationsFragment();
	}

	@DebugLog
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
			if (mSortType == LocationsSorterFactory.TYPE_QUANTITY) {
				menu.findItem(R.id.menu_sort_quantity).setChecked(true);
			} else {
				menu.findItem(R.id.menu_sort_name).setChecked(true);
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
				EventBus.getDefault().postSticky(new LocationSortChangedEvent(LocationsSorterFactory.TYPE_NAME));
				return true;
			case R.id.menu_sort_quantity:
				EventBus.getDefault().postSticky(new LocationSortChangedEvent(LocationsSorterFactory.TYPE_QUANTITY));
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@DebugLog
	@Override
	protected int getDrawerResId() {
		return R.string.title_locations;
	}

	@DebugLog
	public void onEvent(LocationsCountChangedEvent event) {
		mCount = event.count;
		supportInvalidateOptionsMenu();
	}

	@DebugLog
	public void onEvent(LocationSelectedEvent event) {
		Intent intent = new Intent(this, LocationActivity.class);
		intent.putExtra(ActivityUtils.KEY_LOCATION_NAME, event.locationName);
		startActivity(intent);
	}

	@DebugLog
	public void onEvent(LocationSortChangedEvent event) {
		mSortType = event.sortType;
	}
}
