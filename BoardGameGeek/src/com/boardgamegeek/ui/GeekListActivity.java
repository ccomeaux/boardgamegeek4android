package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.view.MenuItem;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.GeekListUtils;

public class GeekListActivity extends SimpleSinglePaneActivity {
	private int mGeekListId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Intent intent = getIntent();
		mGeekListId = intent.getIntExtra(GeekListUtils.KEY_GEEKLIST_ID, BggContract.INVALID_ID);
		getSupportActionBar().setTitle(intent.getStringExtra(GeekListUtils.KEY_GEEKLIST_TITLE));
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new GeekListFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.search_view;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_view:
				ActivityUtils.linkToBgg(this, "geeklist/" + mGeekListId);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
