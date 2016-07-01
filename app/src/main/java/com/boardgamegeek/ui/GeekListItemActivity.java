package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.ActivityUtils;

public class GeekListItemActivity extends SimpleSinglePaneActivity {
	private int geekListId;
	private String geekListTitle;
	private int objectId;
	private String objectName;
	private String url;
	private boolean isBoardGame;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		geekListTitle = intent.getStringExtra(ActivityUtils.KEY_TITLE);
		geekListId = intent.getIntExtra(ActivityUtils.KEY_ID, BggContract.INVALID_ID);
		objectId = intent.getIntExtra(ActivityUtils.KEY_OBJECT_ID, BggContract.INVALID_ID);
		objectName = intent.getStringExtra(ActivityUtils.KEY_NAME);
		url = intent.getStringExtra(ActivityUtils.KEY_OBJECT_URL);
		isBoardGame = intent.getBooleanExtra(ActivityUtils.KEY_IS_BOARD_GAME, false);

		if (!TextUtils.isEmpty(geekListTitle)) {
			final ActionBar actionBar = getSupportActionBar();
			if (actionBar != null) {
				actionBar.setTitle(geekListTitle);
			}
		}
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new GeekListItemFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.view;
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
				if (isBoardGame) {
					ActivityUtils.launchGame(this, objectId, objectName);
				} else {
					ActivityUtils.link(this, url);
				}
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
