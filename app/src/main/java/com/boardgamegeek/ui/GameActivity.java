package com.boardgamegeek.ui;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBar;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.events.GameInfoChangedEvent;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.tasks.FavoriteGameTask;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.ImageUtils.Callback;
import com.boardgamegeek.util.PaletteUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.ScrimUtils;
import com.boardgamegeek.util.ShortcutUtils;
import com.boardgamegeek.util.TaskUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.OnClick;
import hugo.weaving.DebugLog;
import timber.log.Timber;

public class GameActivity extends HeroActivity implements Callback {
	private static final String KEY_GAME_NAME = "GAME_NAME";
	private static final int REQUEST_EDIT_PLAY = 1;
	private int gameId;
	private String gameName;
	private String imageUrl;
	private String thumbnailUrl;
	private boolean arePlayersCustomSorted;
	private boolean isFavorite;

	public static void start(Context context, int gameId, String gameName) {
		final Intent starter = createIntent(gameId, gameName);
		if (starter == null) return;
		context.startActivity(starter);
	}

	@Nullable
	public static Intent createIntent(int gameId, String gameName) {
		if (gameId == BggContract.INVALID_ID) return null;
		final Uri gameUri = Games.buildGameUri(gameId);
		final Intent starter = new Intent(Intent.ACTION_VIEW, gameUri);
		starter.putExtra(KEY_GAME_NAME, gameName);
		return starter;
	}


	@DebugLog
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final ActionBar supportActionBar = getSupportActionBar();
		if (supportActionBar != null) {
			supportActionBar.setDisplayHomeAsUpEnabled(true);
		}

		final Uri gameUri = getIntent().getData();
		if (gameUri == null) {
			Timber.w("Received a null gameUri");
			finish();
		}

		gameId = Games.getGameId(gameUri);
		changeName(getIntent().getStringExtra(KEY_GAME_NAME));

		new Handler().post(new Runnable() {
			@Override
			public void run() {
				if (gameUri == null) return;
				ContentValues values = new ContentValues();
				values.put(Games.LAST_VIEWED, System.currentTimeMillis());
				getContentResolver().update(gameUri, values, null, null);
			}
		});
		if (PreferencesUtils.showLogPlay(this)) {
			fab.setImageResource(R.drawable.fab_log_play);
			PresentationUtils.ensureFabIsShown(fab);
		}

		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent()
				.putContentType("Game")
				.putContentId(String.valueOf(gameId))
				.putContentName(gameName));
		}
	}

	@DebugLog
	@Override
	protected Fragment onCreatePane() {
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
		menu.findItem(R.id.menu_log_play_quick).setVisible(PreferencesUtils.showQuickLogPlay(this));
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem menuItem = menu.findItem(R.id.menu_favorite);
		if (menuItem != null) {
			menuItem.setTitle(isFavorite ? R.string.menu_unfavorite : R.string.menu_favorite);
		}
		return super.onPrepareOptionsMenu(menu);
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
			case R.id.menu_share:
				ActivityUtils.shareGame(this, gameId, gameName, "Game");
				return true;
			case R.id.menu_favorite:
				TaskUtils.executeAsyncTask(new FavoriteGameTask(this, gameId, !isFavorite));
				return true;
			case R.id.menu_shortcut:
				ShortcutUtils.createGameShortcut(this, gameId, gameName, thumbnailUrl);
				return true;
			case R.id.menu_log_play_quick:
				Snackbar.make(coordinator, R.string.msg_logging_play, Snackbar.LENGTH_SHORT).show();
				ActivityUtils.logQuickPlay(this, gameId, gameName);
				return true;
			case R.id.menu_view_image:
				ActivityUtils.startImageActivity(this, imageUrl);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private boolean shouldUpRecreateTask() {
		return getIntent().getBooleanExtra(ActivityUtils.KEY_FROM_SHORTCUT, false);
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(GameInfoChangedEvent event) {
		changeName(event.getGameName());
		imageUrl = event.getImageUrl();
		thumbnailUrl = event.getThumbnailUrl();
		arePlayersCustomSorted = event.arePlayersCustomSorted();
		isFavorite = event.isFavorite();
		ScrimUtils.applyDarkScrim(scrimView);
		ImageUtils.safelyLoadImage(toolbarImage, event.getImageUrl(), this);
	}

	@DebugLog
	private void changeName(String gameName) {
		this.gameName = gameName;
		if (!TextUtils.isEmpty(gameName)) {
			getIntent().putExtra(KEY_GAME_NAME, gameName);
			safelySetTitle(gameName);
		}
	}

	@DebugLog
	@Override
	public void onSuccessfulImageLoad(Palette palette) {
		((GameFragment) getFragment()).onPaletteGenerated(palette);
		fab.setBackgroundTintList(ColorStateList.valueOf(PaletteUtils.getIconSwatch(palette).getRgb()));
		if (PreferencesUtils.showLogPlay(this)) {
			fab.show();
		}
	}

	@Override
	public void onFailedImageLoad() {
		if (PreferencesUtils.showLogPlay(this)) {
			fab.show();
		}
	}

	@Override
	public void onRefresh() {
		if (((GameFragment) getFragment()).triggerRefresh()) {
			updateRefreshStatus(true);
		}
	}

	@SuppressWarnings("unused")
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(GameFragment.SyncCompleteEvent event) {
		if (event.getGameId() == gameId) {
			updateRefreshStatus(false);
		}
	}

	@DebugLog
	@OnClick(R.id.fab)
	public void onFabClicked() {
		Intent intent = ActivityUtils.createEditPlayIntent(this, gameId, gameName, thumbnailUrl, imageUrl);
		intent.putExtra(ActivityUtils.KEY_CUSTOM_PLAYER_SORT, arePlayersCustomSorted);
		startActivityForResult(intent, REQUEST_EDIT_PLAY);
	}
}
