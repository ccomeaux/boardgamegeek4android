package com.boardgamegeek.ui;

import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.service.UpdateService;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;

public class PlayActivity extends SimpleSinglePaneActivity implements PlayFragment.Callbacks {
	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		changeName(getIntent().getStringExtra(KEY_GAME_NAME));
	}

	@Override
	protected Fragment onCreatePane() {
		return new PlayFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return 0;
	}

	@Override
	public void onNameChanged(String gameName) {
		changeName(gameName);
	}

	private void changeName(String gameName) {
		if (!TextUtils.isEmpty(gameName)) {
			getIntent().putExtra(KEY_GAME_NAME, gameName);
			getSupportActionBar().setSubtitle(gameName);
		}
	}

	@Override
	public void onDeleted() {
		finish();
		// TODO: hook this back up
		// UpdateService.start(this, UpdateService.SYNC_TYPE_PLAYS_UPLOAD, BggContract.INVALID_ID, null);
	}
}