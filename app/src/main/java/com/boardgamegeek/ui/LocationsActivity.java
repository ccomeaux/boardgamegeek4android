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

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import hugo.weaving.DebugLog;

public class LocationsActivity extends SimpleSinglePaneActivity {
	private int locationCount = -1;
	private int sortType = LocationsSorterFactory.TYPE_DEFAULT;

	@DebugLog
	@Override
	protected Fragment onCreatePane(Intent intent) {
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
		menu.findItem(R.id.menu_sort).setVisible(true);
		if (sortType == LocationsSorterFactory.TYPE_QUANTITY) {
			menu.findItem(R.id.menu_sort_quantity).setChecked(true);
		} else {
			menu.findItem(R.id.menu_sort_name).setChecked(true);
		}
		ToolbarUtils.setActionBarText(menu, R.id.menu_list_count, locationCount <= 0 ? "" : String.format("%,d", locationCount));
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

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(sticky = true)
	public void onEvent(LocationsCountChangedEvent event) {
		locationCount = event.getCount();
		supportInvalidateOptionsMenu();
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe
	public void onEvent(LocationSelectedEvent event) {
		Intent intent = ActivityUtils.createLocationIntent(this, event.getLocationName());
		startActivity(intent);
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(sticky = true)
	public void onEvent(LocationSortChangedEvent event) {
		sortType = event.getSortType();
	}
}
