package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.boardgamegeek.events.PlayDeletedEvent;
import com.boardgamegeek.events.PlaySelectedEvent;
import com.boardgamegeek.events.PlaySentEvent;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.service.SyncService;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import icepick.Icepick;

public class PlayActivity extends SimpleSinglePaneActivity {
	private static final String KEY_ID = "ID";
	private static final String KEY_GAME_ID = "GAME_ID";
	private static final String KEY_GAME_NAME = "GAME_NAME";
	private static final String KEY_IMAGE_URL = "IMAGE_URL";
	private static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";
	private long internalId = BggContract.INVALID_ID;
	private int gameId = BggContract.INVALID_ID;
	private String gameName;
	private String thumbnailUrl;
	private String imageUrl;

	public static void start(Context context, PlaySelectedEvent event) {
		Intent intent = createIntent(context, event.getInternalId(), event.getGameId(), event.getGameName(), event.getThumbnailUrl(), event.getImageUrl());
		context.startActivity(intent);
	}

	public static Intent createIntent(Context context, long internalId, int gameId, String gameName, String thumbnailUrl, String imageUrl) {
		Intent intent = new Intent(context, PlayActivity.class);
		intent.putExtra(KEY_ID, internalId);
		intent.putExtra(KEY_GAME_ID, gameId);
		intent.putExtra(KEY_GAME_NAME, gameName);
		intent.putExtra(KEY_THUMBNAIL_URL, thumbnailUrl);
		intent.putExtra(KEY_IMAGE_URL, imageUrl);
		return intent;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);

		EventBus.getDefault().removeStickyEvent(PlaySelectedEvent.class);

		if (savedInstanceState == null) {
			final ContentViewEvent event = new ContentViewEvent().putContentType("Play");
			Answers.getInstance().logContentView(event);
		}
	}

	@Override
	protected void readIntent(Intent intent) {
		internalId = intent.getLongExtra(KEY_ID, BggContract.INVALID_ID);
		if (internalId == BggContract.INVALID_ID) finish();
		gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID);
		gameName = intent.getStringExtra(KEY_GAME_NAME);
		thumbnailUrl = intent.getStringExtra(KEY_THUMBNAIL_URL);
		imageUrl = intent.getStringExtra(KEY_IMAGE_URL);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return PlayFragment.newInstance(internalId, gameId, gameName, imageUrl, thumbnailUrl);
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
