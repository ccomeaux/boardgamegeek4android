package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.view.Menu;
import com.boardgamegeek.R;
import com.boardgamegeek.util.ActivityUtils;

public class LocationActivity extends SimpleSinglePaneActivity implements PlaysFragment.Callbacks {
	public static final String KEY_LOCATION_NAME = "LOCATION_NAME";
	private int mCount;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		String name = intent.getStringExtra(KEY_LOCATION_NAME);

		getSupportActionBar().setSubtitle(name);
	}

	@Override
	protected Bundle onBeforeArgumentsSet(Bundle arguments) {
		final Intent intent = getIntent();
		arguments.putInt(PlaysFragment.KEY_MODE, PlaysFragment.MODE_LOCATION);
		arguments.putString(PlaysFragment.KEY_LOCATION, intent.getStringExtra(KEY_LOCATION_NAME));
		return arguments;
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new PlaysFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.text_only;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		ActivityUtils.setActionBarText(menu, R.id.menu_list_count,
			(isDrawerOpen() || mCount <= 0) ? "" : String.valueOf(mCount));
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onPlaySelected(int playId, int gameId, String gameName, String thumbnailUrl, String imageUrl) {
		ActivityUtils.startPlayActivity(this, playId, gameId, gameName, thumbnailUrl, imageUrl);
		return false;
	}

	@Override
	public void onPlayCountChanged(int count) {
		mCount = count;
		supportInvalidateOptionsMenu();
	}

	@Override
	public void onSortChanged(String sortName) {
		// sorting not allowed
	}
}
