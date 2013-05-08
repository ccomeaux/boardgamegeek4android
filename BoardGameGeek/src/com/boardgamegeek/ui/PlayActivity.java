package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;

import com.boardgamegeek.model.Play;
import com.boardgamegeek.service.SyncService;

public class PlayActivity extends SimpleSinglePaneActivity implements PlayFragment.Callbacks {
	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		changeName(getIntent().getStringExtra(KEY_GAME_NAME));
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == Play.SYNC_STATUS_IN_PROGRESS && resultCode == LogPlayActivity.RESULT_UPDATED) {
			finish();
		}
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
	public void onSent() {
		SyncService.sync(this, SyncService.FLAG_SYNC_PLAYS_UPLOAD);
	}

	@Override
	public void onDeleted() {
		finish();
		SyncService.sync(this, SyncService.FLAG_SYNC_PLAYS_UPLOAD);
	}
}