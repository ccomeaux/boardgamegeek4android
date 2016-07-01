package com.boardgamegeek.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.ActivityUtils;

public class GeekListActivity extends SimpleSinglePaneActivity {
	private int geekListId;
	private String geekListTitle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Intent intent = getIntent();
		geekListId = intent.getIntExtra(ActivityUtils.KEY_ID, BggContract.INVALID_ID);
		geekListTitle = intent.getStringExtra(ActivityUtils.KEY_TITLE);
		final ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setTitle(geekListTitle);
		}
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new GeekListFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.view_share;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Uri uri = ActivityUtils.createBggUri("geeklist", geekListId);
		switch (item.getItemId()) {
			case R.id.menu_view:
				ActivityUtils.link(this, uri);
				return true;
			case R.id.menu_share:
				String description = String.format(getString(R.string.share_geeklist_text), geekListTitle);
				ActivityUtils.share(this, getString(R.string.share_geeklist_subject), description + "\n\n" + uri, R.string.title_share);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
