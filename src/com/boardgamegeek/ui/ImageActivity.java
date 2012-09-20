package com.boardgamegeek.ui;

import android.support.v4.app.Fragment;

import com.boardgamegeek.R;

public class ImageActivity extends SimpleSinglePaneActivity {
	public static final String KEY_IMAGE_URL = "IMAGE_URL";

	@Override
	protected Fragment onCreatePane() {
		return new ImageFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.search_only;
	}
}