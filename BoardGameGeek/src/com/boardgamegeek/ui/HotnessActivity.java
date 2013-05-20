package com.boardgamegeek.ui;

import android.support.v4.app.Fragment;

import com.boardgamegeek.R;

public class HotnessActivity extends TopLevelSinglePaneActivity {
	@Override
	protected Fragment onCreatePane() {
		return new HotnessFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.search_only;
	}
}
