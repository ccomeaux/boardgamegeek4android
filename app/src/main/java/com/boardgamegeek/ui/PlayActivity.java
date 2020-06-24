package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.boardgamegeek.events.PlayDeletedEvent;
import com.boardgamegeek.events.PlaySentEvent;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.service.SyncService;
import com.google.firebase.analytics.FirebaseAnalytics.Event;
import com.google.firebase.analytics.FirebaseAnalytics.Param;

import org.greenrobot.eventbus.Subscribe;

import androidx.fragment.app.Fragment;

public class PlayActivity extends SimpleSinglePaneActivity {
	private static final String KEY_ID = "ID";
	private static final String KEY_GAME_ID = "GAME_ID";
	private static final String KEY_GAME_NAME = "GAME_NAME";
	private static final String KEY_IMAGE_URL = "IMAGE_URL";
	private static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";
	private static final String KEY_HERO_IMAGE_URL = "HERO_IMAGE_URL";
	private long internalId = BggContract.INVALID_ID;
	private int gameId = BggContract.INVALID_ID;
	private String gameName;
	private String thumbnailUrl;
	private String imageUrl;
	private String heroImageUrl;

	public static void start(Context context, long internalId, int gameId, String gameName, String thumbnailUrl, String imageUrl, String heroImageUrl) {
		Intent intent = createIntent(context, internalId, gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl);
		context.startActivity(intent);
	}

	public static Intent createIntent(Context context, long internalId, int gameId, String gameName, String thumbnailUrl, String imageUrl, String heroImageUrl) {
		Intent intent = new Intent(context, PlayActivity.class);
		intent.putExtra(KEY_ID, internalId);
		intent.putExtra(KEY_GAME_ID, gameId);
		intent.putExtra(KEY_GAME_NAME, gameName);
		intent.putExtra(KEY_THUMBNAIL_URL, thumbnailUrl);
		intent.putExtra(KEY_IMAGE_URL, imageUrl);
		intent.putExtra(KEY_HERO_IMAGE_URL, heroImageUrl);
		return intent;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			Bundle bundle = new Bundle();
			bundle.putString(Param.CONTENT_TYPE, "Play");
			firebaseAnalytics.logEvent(Event.VIEW_ITEM, bundle);
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
		heroImageUrl = intent.getStringExtra(KEY_HERO_IMAGE_URL);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return PlayFragment.newInstance(internalId, gameId, gameName, imageUrl, thumbnailUrl, heroImageUrl);
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
