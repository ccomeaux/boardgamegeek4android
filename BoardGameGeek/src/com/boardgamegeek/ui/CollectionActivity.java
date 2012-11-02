package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.app.ActionBar;
import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.UIUtils;

public class CollectionActivity extends SimpleSinglePaneActivity implements CollectionFragment.Callbacks {
	private static final int HELP_VERSION = 1;

	private boolean mShortcut;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mShortcut = "android.intent.action.CREATE_SHORTCUT".equals(getIntent().getAction());

		if (mShortcut) {
			final ActionBar actionBar = getSupportActionBar();
			actionBar.setHomeButtonEnabled(false);
			actionBar.setDisplayHomeAsUpEnabled(false);
			actionBar.setTitle(R.string.menu_create_shortcut);
		}

		if (DateTimeUtils.howManyHoursOld(BggApplication.getInstance().getLastCollectionSync()) > 2) {
			BggApplication.getInstance().putLastCollectionSync();
			SyncService.start(this, null, SyncService.SYNC_TYPE_COLLECTION);
		}

		UIUtils.showHelpDialog(this, BggApplication.HELP_COLLECTION_KEY, HELP_VERSION, R.string.help_collection);
	}

	@Override
	protected Fragment onCreatePane() {
		return new CollectionFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return mShortcut ? 0 : R.menu.search_only;
	}

	@Override
	public boolean onGameSelected(int gameId, String gameName) {
		ActivityUtils.launchGame(this, gameId, gameName);
		return true;
	}

	@Override
	public void onSetShortcut(Intent intent) {
		setResult(RESULT_OK, intent);
		finish();
	}
}