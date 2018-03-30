package com.boardgamegeek.ui;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.FloatingActionButton.OnVisibilityChangedListener;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
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
import com.boardgamegeek.tasks.AddCollectionItemTask;
import com.boardgamegeek.tasks.FavoriteGameTask;
import com.boardgamegeek.ui.dialog.CollectionStatusDialogFragment;
import com.boardgamegeek.ui.dialog.CollectionStatusDialogFragment.CollectionStatusDialogListener;
import com.boardgamegeek.ui.model.Game;
import com.boardgamegeek.ui.model.RefreshableResource;
import com.boardgamegeek.ui.model.Status;
import com.boardgamegeek.ui.viewmodel.GameViewModel;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.ImageUtils.Callback;
import com.boardgamegeek.util.PaletteUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.ScrimUtils;
import com.boardgamegeek.util.ShortcutUtils;
import com.boardgamegeek.util.TaskUtils;
import com.boardgamegeek.util.UIUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import butterknife.OnClick;
import hugo.weaving.DebugLog;
import timber.log.Timber;

public class GameActivity extends HeroTabActivity {
	private static final String KEY_GAME_ID = "GAME_ID";
	private static final String KEY_GAME_NAME = "GAME_NAME";
	private static final String KEY_IMAGE_URL = "IMAGE_URL";
	private static final String KEY_FROM_SHORTCUT = "FROM_SHORTCUT";
	private int gameId;
	private String gameName;
	private String imageUrl;
	private String thumbnailUrl;
	private String heroImageUrl;
	private boolean arePlayersCustomSorted;
	private boolean isFavorite;
	private GamePagerAdapter adapter;
	@ColorInt private int iconColor;
	@ColorInt private int darkColor;
	@ColorInt private int[] playCountColors;

	public static void start(Context context, int gameId, String gameName) {
		start(context, gameId, gameName, null);
	}

	public static void start(Context context, int gameId, String gameName, String imageUrl) {
		final Intent starter = createIntent(context, gameId, gameName, imageUrl);
		if (starter == null) return;
		context.startActivity(starter);
	}

	public static void startUp(Context context, int gameId, String gameName) {
		startUp(context, gameId, gameName, null);
	}

	public static void startUp(Context context, int gameId, String gameName, String imageUrl) {
		final Intent starter = createIntent(context, gameId, gameName, imageUrl);
		if (starter == null) return;
		starter.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		starter.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity(starter);
	}

	@Nullable
	public static Intent createIntent(Context context, int gameId, String gameName, String imageUrl) {
		if (gameId == BggContract.INVALID_ID) return null;
		final Intent starter = new Intent(context, GameActivity.class);
		starter.putExtra(KEY_GAME_ID, gameId);
		starter.putExtra(KEY_GAME_NAME, gameName);
		starter.putExtra(KEY_IMAGE_URL, imageUrl);
		return starter;
	}

	@Nullable
	public static Intent createIntentAsShortcut(Context context, int gameId, String gameName, String thumbnailUrl) {
		Intent intent = createIntent(context, gameId, gameName, thumbnailUrl);
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

		changeName(getIntent().getStringExtra(KEY_GAME_NAME));
		//TODO: pass in hero image URL instead
		//changeImage(getIntent().getStringExtra(KEY_IMAGE_URL));

		initializeViewPager();

		GameViewModel viewModel = ViewModelProviders.of(this).get(GameViewModel.class);

		viewModel.getGame(gameId).observe(this, new Observer<RefreshableResource<Game>>() {
			@Override
			public void onChanged(@Nullable RefreshableResource<Game> game) {
				if (game == null) return;
				if (game.getStatus() == Status.ERROR) {
					Toast.makeText(GameActivity.this, game.getMessage(), Toast.LENGTH_SHORT).show();
				}
				final Game g = game.getData();
				if (g == null) return;
				changeName(g.getName());
				changeImage(g);
				arePlayersCustomSorted = g.getCustomPlayerSort();
				isFavorite = g.isFavorite();
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
		adapter = new GamePagerAdapter(getSupportFragmentManager(), this);
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

	private void changeImage(@NonNull Game game) {
		if (!game.getImageUrl().equals(imageUrl) ||
			!game.getThumbnailUrl().equals(thumbnailUrl) ||
			!game.getHeroImageUrl().equals(heroImageUrl)) {
			imageUrl = game.getImageUrl();
			thumbnailUrl = game.getThumbnailUrl();
			heroImageUrl = game.getHeroImageUrl();
			ImageUtils.safelyLoadImage(toolbarImage, imageUrl, thumbnailUrl, heroImageUrl, imageLoadCallback);
		} else {
			imageUrl = game.getImageUrl();
			thumbnailUrl = game.getThumbnailUrl();
			heroImageUrl = game.getHeroImageUrl();
		}
	}

	private final Callback imageLoadCallback = new Callback() {
		@DebugLog
		@Override
		public void onSuccessfulImageLoad(Palette palette) {
			ScrimUtils.applyDarkScrim(scrimView);
			if (palette != null) {
				Palette.Swatch iconSwatch = PaletteUtils.getIconSwatch(palette);
				Palette.Swatch darkSwatch = PaletteUtils.getDarkSwatch(palette);

				iconColor = iconSwatch.getRgb();
				darkColor = darkSwatch.getRgb();
				playCountColors = PaletteUtils.getPlayCountColors(palette, getApplicationContext());
				EventBus.getDefault().post(new ColorEvent(gameId, iconColor, darkColor, playCountColors));
				PresentationUtils.colorFab(fab, PaletteUtils.getIconSwatch(palette).getRgb());
				adapter.displayFab();
			}

			new Handler().post(new Runnable() {
				@Override
				public void run() {
					final String url = (String) toolbarImage.getTag(R.id.url);
					if (!TextUtils.isEmpty(url) &&
						!url.equals(imageUrl) &&
						!url.equals(thumbnailUrl) &&
						!url.equals(heroImageUrl)) {
						ContentValues values = new ContentValues();
						values.put(BggContract.Games.HERO_IMAGE_URL, url);
						getContentResolver().update(BggContract.Games.buildGameUri(gameId), values, null, null);
					}
				}
			});
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

	public static class ColorEvent {
		private final int gameId;
		@ColorInt private final int iconColor;
		@ColorInt private final int darkColor;
		@ColorInt private final int[] playCountColors;

		public ColorEvent(int gameId, int iconColor, int darkColor, int[] playCountColors) {
			this.gameId = gameId;
			this.iconColor = iconColor;
			this.darkColor = darkColor;
			this.playCountColors = playCountColors;
		}

		public int getGameId() {
			return gameId;
		}

		@ColorInt
		public int getIconColor() {
			return iconColor;
		}

		@ColorInt
		public int getDarkColor() {
			return darkColor;
		}

		@ColorInt
		public int[] getPlayCountColors() {
			return playCountColors;
		}
	}

	interface TabListener {
		void onFabClicked();
	}

	private final class GamePagerAdapter extends FragmentPagerAdapter {
		public static final int INVALID_IMAGE_RES_ID = -1;

		private final class Tab {
			@StringRes private final int titleResId;
			@DrawableRes private final int imageResId;
			private final TabListener listener;

			public Tab(int titleResId) {
				this(titleResId, INVALID_IMAGE_RES_ID, null);
			}

			public Tab(int titleResId, int imageResId, TabListener listener) {
				this.titleResId = titleResId;
				this.imageResId = imageResId;
				this.listener = listener;
			}

			public int getTitleResId() {
				return titleResId;
			}

			public int getImageResId() {
				return imageResId;
			}
		}

		private final Context context;
		private final List<Tab> tabs = new ArrayList<>();
		private int currentPosition;

		public GamePagerAdapter(FragmentManager fragmentManager, Context context) {
			super(fragmentManager);
			this.context = context;
			updateTabs();
		}

		@Override
		public void notifyDataSetChanged() {
			super.notifyDataSetChanged();
			updateTabs();
		}

		private void updateTabs() {
			tabs.clear();
			tabs.add(new Tab(
				R.string.title_description,
				R.drawable.fab_log_play,
				new TabListener() {
					@Override
					public void onFabClicked() {
						onPlayFabClicked();
					}
				}
			));
			tabs.add(new Tab(
				R.string.title_info,
				R.drawable.fab_log_play,
				new TabListener() {
					@Override
					public void onFabClicked() {
						onPlayFabClicked();
					}
				}
			));
			if (shouldShowCollection())
				tabs.add(new Tab(
					R.string.title_collection,
					R.drawable.fab_add,
					new TabListener() {
						@Override
						public void onFabClicked() {
							onCollectionFabClicked();
						}
					}
				));
			if (shouldShowPlays())
				tabs.add(new Tab(
					R.string.title_plays,
					R.drawable.fab_log_play,
					new TabListener() {
						@Override
						public void onFabClicked() {
							onPlayFabClicked();
						}
					})
				);
			tabs.add(new Tab(R.string.links));
		}

		@Override
		public CharSequence getPageTitle(int position) {
			if (position < tabs.size()) {
				return getString(tabs.get(position).getTitleResId());
			}
			return "";
		}

		@Override
		public Fragment getItem(int position) {
			if (position < tabs.size()) {
				switch (tabs.get(position).getTitleResId()) {
					case R.string.title_description:
						return GameDescriptionFragment.newInstance(gameId);
					case R.string.title_info:
						return GameFragment.newInstance(gameId, gameName, iconColor, darkColor);
					case R.string.title_collection:
						return GameCollectionFragment.newInstance(gameId);
					case R.string.title_plays:
						return GamePlaysFragment.newInstance(gameId, gameName, iconColor, playCountColors);
					case R.string.links:
						return GameLinksFragment.newInstance(gameId, gameName, iconColor);
				}
			}
			return null;
		}

		@Override
		public int getCount() {
			return tabs.size();
		}

		public void setCurrentPosition(int position) {
			currentPosition = position;
		}

		public void displayFab() {
			if (currentPosition < tabs.size()) {
				final int resId = tabs.get(currentPosition).getImageResId();
				if (resId != INVALID_IMAGE_RES_ID) {
					if (fab.isShown()) {
						fab.hide(new OnVisibilityChangedListener() {
							@Override
							public void onHidden(FloatingActionButton fab) {
								super.onHidden(fab);
								fab.setImageResource(resId);
								fab.show();
							}
						});
					} else {
						fab.setImageResource(resId);
						fab.show();
					}
				} else {
					fab.hide();
				}
			}
		}

		public void onFabClicked() {
			if (currentPosition < tabs.size()) {
				TabListener listener = tabs.get(currentPosition).listener;
				if (listener != null) listener.onFabClicked();
			}
		}

		private void onCollectionFabClicked() {
			CollectionStatusDialogFragment statusDialogFragment = CollectionStatusDialogFragment.newInstance(
				rootContainer,
				new CollectionStatusDialogListener() {
					@Override
					public void onSelectStatuses(List<String> selectedStatuses, int wishlistPriority) {
						AddCollectionItemTask task = new AddCollectionItemTask(context, gameId, selectedStatuses, wishlistPriority);
						TaskUtils.executeAsyncTask(task);
					}
				}
			);
			statusDialogFragment.setTitle(R.string.title_add_a_copy);
			DialogUtils.showFragment(GameActivity.this, statusDialogFragment, "status_dialog");
		}

		private void onPlayFabClicked() {
			LogPlayActivity.logPlay(context, gameId, gameName, thumbnailUrl, imageUrl, heroImageUrl, arePlayersCustomSorted);
		}

		@DebugLog
		private boolean shouldShowPlays() {
			return Authenticator.isSignedIn(getApplicationContext()) && PreferencesUtils.getSyncPlays(getApplicationContext());
		}

		@DebugLog
		private boolean shouldShowCollection() {
			return Authenticator.isSignedIn(getApplicationContext()) &&
				PreferencesUtils.isCollectionSetToSync(getApplicationContext());
		}
	}
}
