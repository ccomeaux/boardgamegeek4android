package com.boardgamegeek.ui;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.events.GameInfoChangedEvent;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.ShortcutUtils;

import hugo.weaving.DebugLog;

public class GameActivity extends SimpleSinglePaneActivity {
	private static final int REQUEST_EDIT_PLAY = 1;
	private int gameId;
	private String gameName;
	private String imageUrl;
	private String thumbnailUrl;
	private boolean arePlayersCustomSorted;

	@DebugLog
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final ActionBar supportActionBar = getSupportActionBar();
		if (supportActionBar != null) {
			supportActionBar.setDisplayHomeAsUpEnabled(true);
		}

		gameId = Games.getGameId(getIntent().getData());
		changeName(getIntent().getStringExtra(ActivityUtils.KEY_GAME_NAME));

		new Handler().post(new Runnable() {
			@Override
			public void run() {
				ContentValues values = new ContentValues();
				values.put(Games.LAST_VIEWED, System.currentTimeMillis());
				getContentResolver().update(getIntent().getData(), values, null, null);
			}
		});
	}

	@DebugLog
	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new GameFragment();
	}

	@DebugLog
	@Override
	protected int getOptionsMenuId() {
		return R.menu.game;
	}

	@DebugLog
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.findItem(R.id.menu_log_play).setVisible(PreferencesUtils.showLogPlay(this));
		menu.findItem(R.id.menu_log_play_quick).setVisible(PreferencesUtils.showQuickLogPlay(this));
		return true;
	}

	@DebugLog
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				Intent upIntent = new Intent(this, HotnessActivity.class);
				if (Authenticator.isSignedIn(this)) {
					upIntent = new Intent(this, CollectionActivity.class);
				}
				if (shouldUpRecreateTask()) {
					TaskStackBuilder.create(this).addNextIntentWithParentStack(upIntent).startActivities();
				} else {
					NavUtils.navigateUpTo(this, upIntent);
				}
				return true;
			case R.id.menu_language_poll:
				Bundle arguments = new Bundle(2);
				arguments.putInt(ActivityUtils.KEY_GAME_ID, gameId);
				arguments.putString(ActivityUtils.KEY_TYPE, "language_dependence");
				DialogUtils.launchDialog(getFragment(), new PollFragment(), "poll-dialog", arguments);
				return true;
			case R.id.menu_share:
				ActivityUtils.shareGame(this, gameId, gameName);
				return true;
			case R.id.menu_shortcut:
				ShortcutUtils.createShortcut(this, gameId, gameName, thumbnailUrl);
				return true;
			case R.id.menu_log_play:
				Intent intent = ActivityUtils.createEditPlayIntent(this, 0, gameId, gameName, thumbnailUrl, imageUrl);
				intent.putExtra(LogPlayActivity.KEY_CUSTOM_PLAYER_SORT, arePlayersCustomSorted);
				startActivityForResult(intent, REQUEST_EDIT_PLAY);
				return true;
			case R.id.menu_log_play_quick:
				Toast.makeText(this, R.string.msg_logging_play, Toast.LENGTH_SHORT).show();
				ActivityUtils.logQuickPlay(this, gameId, gameName);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private boolean shouldUpRecreateTask() {
		return getIntent().getBooleanExtra(ActivityUtils.KEY_FROM_SHORTCUT, false);
	}

	@SuppressWarnings("unused")
	@DebugLog
	public void onEventMainThread(GameInfoChangedEvent event) {
		changeName(event.getGameName());
		changeSubtype(event.getSubtype());
		imageUrl = event.getImageUrl();
		thumbnailUrl = event.getThumbnailUrl();
		arePlayersCustomSorted = event.arePlayersCustomSorted();
	}

	@DebugLog
	private void changeName(String gameName) {
		this.gameName = gameName;
		if (!TextUtils.isEmpty(gameName)) {
			getIntent().putExtra(ActivityUtils.KEY_GAME_NAME, gameName);
			final ActionBar supportActionBar = getSupportActionBar();
			if (supportActionBar != null) {
				supportActionBar.setTitle(gameName);
			}
		}
	}

	@DebugLog
	private void changeSubtype(String subtype) {
		if (subtype == null) {
			return;
		}
		int resId = R.string.title_game;
		switch (subtype) {
			case BggService.THING_SUBTYPE_BOARDGAME:
				resId = R.string.title_board_game;
				break;
			case BggService.THING_SUBTYPE_BOARDGAME_EXPANSION:
				resId = R.string.title_board_game_expansion;
				break;
			case BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY:
				resId = R.string.title_board_game_accessory;
				break;
		}
		final ActionBar supportActionBar = getSupportActionBar();
		if (supportActionBar != null) {
			supportActionBar.setSubtitle(getString(resId));
		}
	}
}
