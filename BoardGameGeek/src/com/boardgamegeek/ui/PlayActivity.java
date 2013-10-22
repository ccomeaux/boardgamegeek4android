package com.boardgamegeek.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;

import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.UIUtils;

public class PlayActivity extends SimpleSinglePaneActivity implements PlayFragment.Callbacks {
	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	private static final String KEY_PLAY_ID = "PLAY_ID";
	private static final int REQUEST_EDIT_PLAY = 0;
	private int mPlayId = BggContract.INVALID_ID;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		changeName(getIntent().getStringExtra(KEY_GAME_NAME));

		if (savedInstanceState == null) {
			maybeEditPlay(getIntent());
		} else {
			newPlayId(savedInstanceState.getInt(KEY_PLAY_ID, BggContract.INVALID_ID));
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(KEY_PLAY_ID, mPlayId);
	}

	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		maybeEditPlay(intent);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_EDIT_PLAY) {
			switch (resultCode) {
				case RESULT_OK:
					// new play was deleted
					finish();
					break;
				case RESULT_CANCELED:
					// do nothing
					break;
				default:
					// resultCode is a new playId
					newPlayId(resultCode);
					break;
			}
		}
	}

	@Override
	protected Bundle onBeforeArgumentsSet(Bundle arguments) {
		if (mPlayId != BggContract.INVALID_ID) {
			arguments = UIUtils.replaceData(arguments, Plays.buildPlayUri(mPlayId));
		}
		return arguments;
	}

	private void newPlayId(int playId) {
		if (playId != BggContract.INVALID_ID) {
			mPlayId = playId;
			((PlayFragment) getFragment()).setNewPlayId(playId);
		}
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new PlayFragment();
	}

	@Override
	public void onNameChanged(String gameName) {
		changeName(gameName);
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

	private void changeName(String gameName) {
		if (!TextUtils.isEmpty(gameName)) {
			getIntent().putExtra(KEY_GAME_NAME, gameName);
			getSupportActionBar().setSubtitle(gameName);
		}
	}

	private void maybeEditPlay(Intent intent) {
		if (Intent.ACTION_EDIT.equals(intent.getAction())) {
			Intent editIntent = new Intent(intent);
			editIntent.setClass(this, LogPlayActivity.class);
			editIntent.setAction(Intent.ACTION_EDIT);
			editIntent.putExtra(LogPlayActivity.KEY_PLAY_ID, Plays.getPlayId(intent.getData()));
			startActivityForResult(editIntent, REQUEST_EDIT_PLAY);
		}
	}
}