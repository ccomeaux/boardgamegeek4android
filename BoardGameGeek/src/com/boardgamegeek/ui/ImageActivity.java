package com.boardgamegeek.ui;

import android.content.Intent;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.view.MenuItem;
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
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				startActivity(new Intent(this, CollectionActivity.class));
				finish();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}