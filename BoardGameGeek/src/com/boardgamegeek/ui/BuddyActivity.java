package com.boardgamegeek.ui;

import com.boardgamegeek.R;

import android.support.v4.app.Fragment;

public class BuddyActivity extends SimpleSinglePaneActivity {
	@Override
	protected Fragment onCreatePane() {
		return new BuddyFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.search_only;
	}
}
