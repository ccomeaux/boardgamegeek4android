package com.boardgamegeek.ui;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.tasks.FavoriteGameTask;
import com.boardgamegeek.ui.adapter.GamePagerAdapter;
import com.boardgamegeek.ui.model.Game;
import com.boardgamegeek.ui.model.RefreshableResource;
import com.boardgamegeek.ui.model.Status;
import com.boardgamegeek.ui.viewmodel.GameViewModel;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.ImageUtils.Callback;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.ScrimUtils;
import com.boardgamegeek.util.ShortcutUtils;
import com.boardgamegeek.util.TaskUtils;
import com.boardgamegeek.util.UIUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import butterknife.OnClick;
import hugo.weaving.DebugLog;
import timber.log.Timber;

public class GameActivity extends HeroTabActivity {
	private static final String KEY_GAME_ID = "GAME_ID";
	private static final String KEY_GAME_NAME = "GAME_NAME";
	private static final String KEY_IMAGE_URL = "IMAGE_URL";
	private static final String KEY_THUMBNAIL_URL = "THUMBNAIL_URL";
	private static final String KEY_HERO_IMAGE_URL = "HERO_IMAGE_URL";
	private static final String KEY_FROM_SHORTCUT = "FROM_SHORTCUT";
	private int gameId;
	private String gameName;
	@NonNull private String imageUrl = "";
	@NonNull private String thumbnailUrl = "";
	@NonNull private String heroImageUrl = "";
	private boolean isFavorite;
	private GamePagerAdapter adapter;
	private GameViewModel viewModel;

	public static void start(Context context, int gameId, String gameName) {
		start(context, gameId, gameName, "", "", "");
	}

	public static void start(Context context, int gameId, String gameName, @NonNull String imageUrl, @NonNull String thumbnailUrl, @NonNull String heroImageUrl) {
		final Intent starter = createIntent(context, gameId, gameName, imageUrl, thumbnailUrl, heroImageUrl);
		if (starter == null) return;
		context.startActivity(starter);
	}

	public static void startUp(Context context, int gameId, String gameName) {
		startUp(context, gameId, gameName, "", "", "");
	}

	public static void startUp(Context context, int gameId, String gameName, @NonNull String imageUrl, @NonNull String thumbnailUrl, @NonNull String heroImageUrl) {
		final Intent starter = createIntent(context, gameId, gameName, imageUrl, thumbnailUrl, heroImageUrl);
		if (starter == null) return;
		starter.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		starter.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity(starter);
	}

	@Nullable
	public static Intent createIntent(Context context, int gameId, String gameName, @NonNull String imageUrl, @NonNull String thumbnailUrl, @NonNull String heroImageUrl) {
		if (gameId == BggContract.INVALID_ID) return null;
		final Intent starter = new Intent(context, GameActivity.class);
		starter.putExtra(KEY_GAME_ID, gameId);
		starter.putExtra(KEY_GAME_NAME, gameName);
		starter.putExtra(KEY_IMAGE_URL, imageUrl);
		starter.putExtra(KEY_THUMBNAIL_URL, thumbnailUrl);
		starter.putExtra(KEY_HERO_IMAGE_URL, heroImageUrl);
		return starter;
	}

	@Nullable
	public static Intent createIntentAsShortcut(Context context, int gameId, String gameName, String thumbnailUrl) {
		Intent intent = createIntent(context, gameId, gameName, "", thumbnailUrl, "");
		if (intent == null) return null;
		intent.putExtra(KEY_FROM_SHORTCUT, true);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		return intent;
	}

	@DebugLog
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		gameId = getIntent().getIntExtra(KEY_GAME_ID, BggContract.INVALID_ID);
		if (gameId == BggContract.INVALID_ID) {
			Timber.w("Received an invalid game ID.");
			finish();
		}

		viewModel = ViewModelProviders.of(this).get(GameViewModel.class);

		initializeViewPager();

		changeName(getIntent().getStringExtra(KEY_GAME_NAME));
		changeImage(getIntent().getStringExtra(KEY_IMAGE_URL),
			getIntent().getStringExtra(KEY_THUMBNAIL_URL),
			getIntent().getStringExtra(KEY_HERO_IMAGE_URL));

		viewModel.getGame(gameId).observe(this, new Observer<RefreshableResource<Game>>() {
			@Override
			public void onChanged(@Nullable RefreshableResource<Game> refreshableResource) {
				if (refreshableResource == null) return;
				if (refreshableResource.getStatus() == Status.ERROR) {
					Toast.makeText(GameActivity.this, refreshableResource.getMessage(), Toast.LENGTH_SHORT).show();
				}
				final Game game = refreshableResource.getData();
				if (game == null) return;
				changeName(game.getName());
				changeImage(game.getImageUrl(), game.getThumbnailUrl(), game.getHeroImageUrl());

				adapter.setGameName(game.getName());
				adapter.setImageUrl(game.getImageUrl());
				adapter.setThumbnailUrl(game.getThumbnailUrl());
				adapter.setHeroImageUrl(game.getHeroImageUrl());
				adapter.setArePlayersCustomSorted(game.getCustomPlayerSort());
				adapter.setIconColor(game.getIconColor());

				PresentationUtils.colorFab(fab, game.getIconColor());
				adapter.displayFab();

				isFavorite = game.isFavorite();
			}
		});

		viewModel.updateLastViewed(System.currentTimeMillis());

		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent()
				.putContentType("Game")
				.putContentId(String.valueOf(gameId))
				.putContentName(gameName));
		}
	}

	@DebugLog
	@Override
	protected void setUpViewPager() {
		adapter = new GamePagerAdapter(getSupportFragmentManager(), this, gameId, getIntent().getStringExtra(KEY_GAME_NAME));
		viewPager.setAdapter(adapter);
		viewPager.addOnPageChangeListener(new OnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			}

			@Override
			public void onPageSelected(int position) {
				adapter.setCurrentPosition(position);
				adapter.displayFab();
			}

			@Override
			public void onPageScrollStateChanged(int state) {
			}
		});
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.game;
	}

	@Override
	public boolean onCreateOptionsMenu(@NonNull Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.findItem(R.id.menu_log_play_quick).setVisible(PreferencesUtils.showQuickLogPlay(this));
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(@NonNull Menu menu) {
		MenuItem menuItem = menu.findItem(R.id.menu_favorite);
		if (menuItem != null) {
			menuItem.setTitle(isFavorite ? R.string.menu_unfavorite : R.string.menu_favorite);
		}
		UIUtils.enableMenuItem(menu, R.id.menu_view_image, !TextUtils.isEmpty(imageUrl));
		return super.onPrepareOptionsMenu(menu);
	}

	@DebugLog
	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
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
				ImageActivity.start(this, imageUrl);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private boolean shouldUpRecreateTask() {
		return getIntent().getBooleanExtra(KEY_FROM_SHORTCUT, false);
	}

	@DebugLog
	private void changeName(String gameName) {
		if (!TextUtils.isEmpty(gameName) && !gameName.equals(this.gameName)) {
			this.gameName = gameName;
			getIntent().putExtra(KEY_GAME_NAME, gameName);
			safelySetTitle(gameName);
		}
	}

	private void changeImage(String imageUrl, String thumbnailUrl, String heroImageUrl) {
		if (!this.imageUrl.equals(imageUrl) ||
			!this.thumbnailUrl.equals(thumbnailUrl) ||
			!this.heroImageUrl.equals(heroImageUrl)) {
			this.imageUrl = imageUrl == null ? "" : imageUrl;
			this.thumbnailUrl = thumbnailUrl == null ? "" : thumbnailUrl;
			this.heroImageUrl = heroImageUrl == null ? "" : heroImageUrl;
			ImageUtils.safelyLoadImage(toolbarImage, this.imageUrl, this.thumbnailUrl, this.heroImageUrl, imageLoadCallback);
		} else {
			this.imageUrl = imageUrl;
			this.thumbnailUrl = thumbnailUrl;
			this.heroImageUrl = heroImageUrl;
		}
	}

	private final Callback imageLoadCallback = new Callback() {
		@DebugLog
		@Override
		public void onSuccessfulImageLoad(Palette palette) {
			viewModel.updateColors(palette);
			ScrimUtils.applyDarkScrim(scrimView);
			viewModel.updateHeroImageUrl((String) toolbarImage.getTag(R.id.url));
		}

		@Override
		public void onFailedImageLoad() {
			adapter.displayFab();
		}
	};

	@DebugLog
	@OnClick(R.id.fab)
	public void onFabClicked() {
		adapter.onFabClicked();
	}
}
