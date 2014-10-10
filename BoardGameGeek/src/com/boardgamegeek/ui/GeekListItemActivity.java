package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.GeekListUtils;

public class GeekListItemActivity extends SimpleSinglePaneActivity {
	private int mId;
	private String mTitle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		mId = intent.getIntExtra(GeekListUtils.KEY_GEEKLIST_ID, BggContract.INVALID_ID);
		mTitle = intent.getStringExtra(GeekListUtils.KEY_GEEKLIST_TITLE);

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
		return R.menu.search_only;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				if (mId != BggContract.INVALID_ID) {
					Intent intent = new Intent(this, GeekListActivity.class);
					intent.putExtra(GeekListUtils.KEY_GEEKLIST_ID, mId);
					intent.putExtra(GeekListUtils.KEY_GEEKLIST_TITLE, mTitle);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					finish();
				} else {
					onBackPressed();
				}
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
