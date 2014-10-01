package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.GeeklistUtils;

public class GeeklistActivity extends SimpleSinglePaneActivity {
	private int mGeeklistId;
	private String mGeeklistTitle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		mGeeklistId = intent.getIntExtra(GeeklistUtils.KEY_GEEKLIST_ID, BggContract.INVALID_ID);
		mGeeklistTitle = intent.getStringExtra(GeeklistUtils.KEY_GEEKLIST_TITLE);

		final ActionBar actionBar = getSupportActionBar();
		actionBar.setTitle(mGeeklistTitle);
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new GeeklistFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.search_view;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				Intent intent = new Intent(this, GeeklistsActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				finish();
				return true;
			case R.id.menu_view:
				ActivityUtils.linkToBgg(this, "geeklist/" + mGeeklistId);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
