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
import com.boardgamegeek.util.GeekListUtils;

public class GeekListItemActivity extends SimpleSinglePaneActivity {
	private int mId;
	private String mTitle;
	private int mObjectId;
	private String mObjectName;
	private String mUrl;
	private boolean mIsBoardGame;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		mTitle = intent.getStringExtra(GeekListUtils.KEY_TITLE);
		mId = intent.getIntExtra(GeekListUtils.KEY_ID, BggContract.INVALID_ID);
		mObjectId = intent.getIntExtra(GeekListUtils.KEY_OBJECT_ID, BggContract.INVALID_ID);
		mObjectName = intent.getStringExtra(GeekListUtils.KEY_NAME);
		mUrl = intent.getStringExtra(GeekListUtils.KEY_OBJECT_URL);
		mIsBoardGame = intent.getBooleanExtra(GeekListUtils.KEY_IS_BOARD_GAME, false);

		final ActionBar actionBar = getSupportActionBar();
		if (!TextUtils.isEmpty(mTitle)) {
			actionBar.setTitle(mTitle);
		}
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new GeekListItemFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.search_view;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				if (mId != BggContract.INVALID_ID) {
					Intent intent = new Intent(this, GeekListActivity.class);
					intent.putExtra(GeekListUtils.KEY_ID, mId);
					intent.putExtra(GeekListUtils.KEY_NAME, mTitle);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					finish();
				} else {
					onBackPressed();
				}
				return true;
			case R.id.menu_view:
				if (mIsBoardGame) {
					ActivityUtils.launchGame(this, mObjectId, mObjectName);
				} else {
					ActivityUtils.link(this, mUrl);
				}
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
