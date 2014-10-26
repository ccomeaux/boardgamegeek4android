package com.boardgamegeek.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.GeekListUtils;

public class GeekListDescriptionActivity extends SimpleSinglePaneActivity {
	private int mGeekListId;
	private String mGeekListTitle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Intent intent = getIntent();
		mGeekListId = intent.getIntExtra(GeekListUtils.KEY_ID, BggContract.INVALID_ID);
		mGeekListTitle = intent.getStringExtra(GeekListUtils.KEY_TITLE);
		getSupportActionBar().setTitle(mGeekListTitle);
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new GeekListDescriptionFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.search_view_share;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Uri uri = ActivityUtils.createBggUri("geeklist", mGeekListId);
		switch (item.getItemId()) {
			case android.R.id.home:
				if (mGeekListId != BggContract.INVALID_ID) {
					Intent intent = new Intent(this, GeekListActivity.class);
					intent.putExtra(GeekListUtils.KEY_ID, mGeekListId);
					intent.putExtra(GeekListUtils.KEY_NAME, mGeekListTitle);
					intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(intent);
					finish();
				} else {
					onBackPressed();
				}
				return true;

			case R.id.menu_view:
				ActivityUtils.link(this, uri);
				return true;
			case R.id.menu_share:
				String description = String.format(getString(R.string.share_geeklist_text), mGeekListTitle);
				ActivityUtils.share(this, getString(R.string.share_geeklist_subject), description + "\n\n" + uri,
					R.string.title_share);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
