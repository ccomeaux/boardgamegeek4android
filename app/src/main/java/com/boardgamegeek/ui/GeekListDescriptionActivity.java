package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.ActivityUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

public class GeekListDescriptionActivity extends SimpleSinglePaneActivity {
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

		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent()
				.putContentType("GeekListDescription")
				.putContentId(String.valueOf(geekListId))
				.putContentName(geekListTitle));
		}
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new GeekListDescriptionFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.view_share;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				if (geekListId != BggContract.INVALID_ID) {
					Intent intent = new Intent(this, GeekListActivity.class);
					intent.putExtra(ActivityUtils.KEY_ID, geekListId);
					intent.putExtra(ActivityUtils.KEY_TITLE, geekListTitle);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					finish();
				} else {
					onBackPressed();
				}
				return true;
			case R.id.menu_view:
				ActivityUtils.linkToBgg(this, "geeklist", geekListId);
				return true;
			case R.id.menu_share:
				ActivityUtils.shareGeekList(this, geekListId, geekListTitle);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
