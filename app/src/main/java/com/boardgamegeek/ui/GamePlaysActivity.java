package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.events.PlaySelectedEvent;
import com.boardgamegeek.events.PlaysCountChangedEvent;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.ToolbarUtils;

import org.greenrobot.eventbus.Subscribe;

import hugo.weaving.DebugLog;
import icepick.Icepick;
import icepick.State;

public class GamePlaysActivity extends SimpleSinglePaneActivity {
	private static final String KEY_GAME_ID = "GAME_ID";
	private static final String KEY_GAME_NAME = "GAME_NAME";
	private static final String KEY_IMAGE_URL = "IMAGE_URL";
	private static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";
	private static final String KEY_CUSTOM_PLAYER_SORT = "CUSTOM_PLAYER_SORT";
	private static final String KEY_ICON_COLOR = "ICON_COLOR";
	private int gameId;
	private String gameName;
	@State int playCount = -1;

	public static void start(Context context, int gameId, String gameName, String imageUrl, String thumbnailUrl, boolean arePlayersCustomSorted, @ColorInt int iconColor) {
		Intent starter = createIntent(context, gameId, gameName, imageUrl, thumbnailUrl, arePlayersCustomSorted, iconColor);
		context.startActivity(starter);
	}

	public static Intent createIntent(Context context, int gameId, String gameName, String imageUrl, String thumbnailUrl) {
		return createIntent(context, gameId, gameName, imageUrl, thumbnailUrl, false, 0);
	}

	public static Intent createIntent(Context context, int gameId, String gameName, String imageUrl, String thumbnailUrl, boolean arePlayersCustomSorted, @ColorInt int iconColor) {
		Intent intent = new Intent(context, GamePlaysActivity.class);
		intent.putExtra(KEY_GAME_ID, gameId);
		intent.putExtra(KEY_GAME_NAME, gameName);
		intent.putExtra(KEY_IMAGE_URL, imageUrl);
		intent.putExtra(KEY_THUMBNAIL_URL, thumbnailUrl);
		intent.putExtra(KEY_CUSTOM_PLAYER_SORT, arePlayersCustomSorted);
		intent.putExtra(KEY_ICON_COLOR, iconColor);
		return intent;
	}

	@DebugLog
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);

		if (!TextUtils.isEmpty(gameName)) {
			ActionBar actionBar = getSupportActionBar();
			if (actionBar != null) {
				actionBar.setSubtitle(gameName);
			}
		}
	}

	@Override
	protected void readIntent(Intent intent) {
		gameId = intent.getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID);
		gameName = intent.getStringExtra(KEY_GAME_NAME);
	}

	@DebugLog
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
	}

	@DebugLog
	@Override
	protected Fragment onCreatePane(Intent intent) {
		return PlaysFragment.newInstanceForGame(gameId, gameName, null, null, false, 0);
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.text_only;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		String countDescription = playCount <= 0 ? "" : String.valueOf(playCount);
		ToolbarUtils.setActionBarText(menu, R.id.menu_text, countDescription);
		return super.onPrepareOptionsMenu(menu);
	}

	@DebugLog
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				GameActivity.startUp(this, gameId, gameName);
				finish();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe
	public void onEvent(PlaySelectedEvent event) {
		PlayActivity.start(this, event);
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(sticky = true)
	public void onEvent(PlaysCountChangedEvent event) {
		playCount = event.getCount();
		supportInvalidateOptionsMenu();
	}
}
