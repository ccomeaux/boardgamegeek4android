package com.boardgamegeek.ui;

import android.support.v4.app.Fragment;

public class BuddyActivity extends SimpleSinglePaneActivity {
	@Override
	protected Fragment onCreatePane() {
		return new BuddyFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return 0;
	}
}
