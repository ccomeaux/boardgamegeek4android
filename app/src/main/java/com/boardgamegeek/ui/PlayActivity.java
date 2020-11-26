package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.boardgamegeek.provider.BggContract;
import com.google.firebase.analytics.FirebaseAnalytics.Event;
import com.google.firebase.analytics.FirebaseAnalytics.Param;

import org.jetbrains.annotations.NotNull;

import androidx.fragment.app.Fragment;

public class PlayActivity extends SimpleSinglePaneActivity {
	private static final String KEY_ID = "ID";
	private static final String KEY_GAME_ID = "GAME_ID";
	private static final String KEY_GAME_NAME = "GAME_NAME";
	private static final String KEY_IMAGE_URL = "IMAGE_URL";
	private static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";
	private static final String KEY_HERO_IMAGE_URL = "HERO_IMAGE_URL";
	private static final String KEY_CUSTOM_PLAYER_SORT = "CUSTOM_PLAYER_SORT";
	private long internalId = BggContract.INVALID_ID;
	private int gameId = BggContract.INVALID_ID;
	private String gameName;
	private String thumbnailUrl;
	private String imageUrl;
	private String heroImageUrl;
	private boolean customPlayerSort;

	public static void start(Context context, long internalId, int gameId, String gameName, String thumbnailUrl, String imageUrl, String heroImageUrl, boolean customPlayerSort) {
		Intent intent = createIntent(context, internalId, gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl, customPlayerSort);
		context.startActivity(intent);
	}

	public static Intent createIntent(Context context, long internalId, int gameId, String gameName, String thumbnailUrl, String imageUrl, String heroImageUrl, boolean customPlayerSort) {
		Intent intent = new Intent(context, PlayActivity.class);
		intent.putExtra(KEY_ID, internalId);
		intent.putExtra(KEY_GAME_ID, gameId);
		intent.putExtra(KEY_GAME_NAME, gameName);
		intent.putExtra(KEY_THUMBNAIL_URL, thumbnailUrl);
		intent.putExtra(KEY_IMAGE_URL, imageUrl);
		intent.putExtra(KEY_HERO_IMAGE_URL, heroImageUrl);
		intent.putExtra(KEY_CUSTOM_PLAYER_SORT, customPlayerSort);
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
		customPlayerSort = intent.getBooleanExtra(KEY_CUSTOM_PLAYER_SORT, false);
	}

	@Override
	protected void onSaveInstanceState(@NotNull Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	protected @NotNull Fragment onCreatePane(@NotNull Intent intent) {
		return PlayFragment.newInstance(internalId, gameId, gameName, imageUrl, thumbnailUrl, heroImageUrl, customPlayerSort);
	}

	// TODO finish activity when delete is successful
}
