package com.boardgamegeek.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;

import com.boardgamegeek.events.PlayDeletedEvent;
import com.boardgamegeek.events.PlaySelectedEvent;
import com.boardgamegeek.events.PlaySentEvent;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.UIUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import icepick.Icepick;
import icepick.State;

public class PlayActivity extends SimpleSinglePaneActivity {
	public static final String KEY_PLAY_ID = "PLAY_ID";
	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";
	public static final String KEY_IMAGE_URL = "IMAGE_URL";
	private BroadcastReceiver broadcastReceiver;
	@State int playId = BggContract.INVALID_ID;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);

		EventBus.getDefault().removeStickyEvent(PlaySelectedEvent.class);

		final int originalPlayId = getPlayId();
		broadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				int playId = PreferencesUtils.getNewPlayId(PlayActivity.this, originalPlayId);
				if (playId != BggContract.INVALID_ID) {
					newPlayId(playId);
				}
			}
		};
	}

	private int getPlayId() {
		if (playId != BggContract.INVALID_ID) {
			return playId;
		}
		return getIntent().getIntExtra(KEY_PLAY_ID, BggContract.INVALID_ID);
	}

	@Override
	protected void onStart() {
		super.onStart();
		LocalBroadcastManager.getInstance(this).registerReceiver((broadcastReceiver), new IntentFilter(SyncService.ACTION_PLAY_ID_CHANGED));
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
	}

	@Override
	protected void onStop() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
		super.onStop();
	}

	@Override
	protected Bundle onBeforeArgumentsSet(Bundle arguments) {
		if (playId != BggContract.INVALID_ID) {
			arguments = UIUtils.replaceData(arguments, Plays.buildPlayUri(playId));
		}
		return arguments;
	}

	private void newPlayId(int playId) {
		if (playId != BggContract.INVALID_ID) {
			this.playId = playId;
			((PlayFragment) getFragment()).setNewPlayId(playId);
		}
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new PlayFragment();
	}

	@SuppressWarnings({ "unused", "UnusedParameters" })
	@Subscribe
	public void onEvent(PlaySentEvent event) {
		SyncService.sync(this, SyncService.FLAG_SYNC_PLAYS_UPLOAD);
	}

	@SuppressWarnings({ "unused", "UnusedParameters" })
	@Subscribe
	public void onEvent(PlayDeletedEvent event) {
		finish();
		SyncService.sync(this, SyncService.FLAG_SYNC_PLAYS_UPLOAD);
	}
}
