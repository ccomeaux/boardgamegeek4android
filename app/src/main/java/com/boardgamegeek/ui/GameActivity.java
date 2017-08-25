package com.boardgamegeek.ui;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
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

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.events.GameInfoChangedEvent;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.tasks.AddCollectionItemTask;
import com.boardgamegeek.tasks.FavoriteGameTask;
import com.boardgamegeek.ui.dialog.CollectionStatusDialogFragment;
import com.boardgamegeek.ui.dialog.CollectionStatusDialogFragment.CollectionStatusDialogListener;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.ImageUtils.Callback;
import com.boardgamegeek.util.PaletteUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.ScrimUtils;
import com.boardgamegeek.util.ShortcutUtils;
import com.boardgamegeek.util.TaskUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.OnClick;
import hugo.weaving.DebugLog;
import timber.log.Timber;

public class GameActivity extends HeroTabActivity implements Callback {
	private static final String KEY_GAME_NAME = "GAME_NAME";
	private static final String KEY_FROM_SHORTCUT = "FROM_SHORTCUT";
	private static final int REQUEST_EDIT_PLAY = 1;
	private int gameId;
	private String gameName;
	private String imageUrl;
	private String thumbnailUrl;
	private boolean arePlayersCustomSorted;
	private boolean isFavorite;
	private GamePagerAdapter adapter;
	@ColorInt private int iconColor;
	@ColorInt private int darkColor;

	public static void start(Context context, int gameId, String gameName) {
		final Intent starter = createIntent(gameId, gameName);
		if (starter == null) return;
		context.startActivity(starter);
	}

	public static void startUp(Context context, int gameId, String gameName) {
		final Intent starter = createIntent(gameId, gameName);
		if (starter == null) return;
		starter.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		starter.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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

	@Nullable
	public static Intent createIntentAsShortcut(int gameId, String gameName) {
		Intent intent = createIntent(gameId, gameName);
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

		final Uri gameUri = getIntent().getData();
		if (gameUri == null) {
			Timber.w("Received a null gameUri");
			finish();
		}

		gameId = Games.getGameId(gameUri);
		changeName(getIntent().getStringExtra(KEY_GAME_NAME));

		initializeViewPager();

		new Handler().post(new Runnable() {
			@Override
			public void run() {
				if (gameUri == null) return;
				ContentValues values = new ContentValues();
				values.put(Games.LAST_VIEWED, System.currentTimeMillis());
				getContentResolver().update(gameUri, values, null, null);
			}
		});

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
				ImageActivity.start(this, imageUrl);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private boolean shouldUpRecreateTask() {
		return getIntent().getBooleanExtra(KEY_FROM_SHORTCUT, false);
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(GameInfoChangedEvent event) {
		changeName(event.getGameName());
		if (!event.getImageUrl().equals(imageUrl)) {
			imageUrl = event.getImageUrl();
			ImageUtils.safelyLoadImage(toolbarImage, event.getImageUrl(), this);
		}
		thumbnailUrl = event.getThumbnailUrl();
		arePlayersCustomSorted = event.arePlayersCustomSorted();
		isFavorite = event.isFavorite();
		ScrimUtils.applyDarkScrim(scrimView);
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
		if (palette != null) {
			Palette.Swatch iconSwatch = PaletteUtils.getIconSwatch(palette);
			Palette.Swatch darkSwatch = PaletteUtils.getDarkSwatch(palette);

			iconColor = iconSwatch.getRgb();
			darkColor = darkSwatch.getRgb();
			EventBus.getDefault().post(new ColorEvent(gameId, iconColor, darkColor));
			fab.setBackgroundTintList(ColorStateList.valueOf(PaletteUtils.getIconSwatch(palette).getRgb()));
			adapter.displayFab();
		}
	}

	@Override
	public void onFailedImageLoad() {
		adapter.displayFab();
	}

	@DebugLog
	@OnClick(R.id.fab)
	public void onFabClicked() {
		adapter.onFabClicked();

	}

	public static class ColorEvent {
		private final int gameId;
		@ColorInt private final int iconColor;
		@ColorInt private final int darkColor;

		public ColorEvent(int gameId, int iconColor, int darkColor) {
			this.gameId = gameId;
			this.iconColor = iconColor;
			this.darkColor = darkColor;
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
	}

	interface TabListener {
		void onFabClicked();
	}

	private final class GamePagerAdapter extends FragmentPagerAdapter {
		public static final int INVALID_IMAGE_RES_ID = -1;

		private final class Tab {
			@StringRes private final int titleResId;
			private final Fragment fragment;
			@DrawableRes private final int imageResId;
			private final TabListener listener;

			public Tab(int titleResId, Fragment fragment) {
				this(titleResId, fragment, INVALID_IMAGE_RES_ID, null);
			}

			public Tab(int titleResId, Fragment fragment, int imageResId, TabListener listener) {
				this.titleResId = titleResId;
				this.fragment = fragment;
				this.imageResId = imageResId;
				this.listener = listener;
			}

			public int getTitleResId() {
				return titleResId;
			}

			public Fragment getFragment() {
				return fragment;
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
			tabs.add(new Tab(R.string.title_info, GameFragment.newInstance(gameId, gameName, iconColor, darkColor)));
			if (shouldShowCollection())
				tabs.add(new Tab(
					R.string.title_collection,
					GameCollectionFragment.newInstance(gameId, gameName),
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
					GamePlaysFragment.newInstance(gameId, gameName, iconColor),
					R.drawable.fab_log_play,
					new TabListener() {
						@Override
						public void onFabClicked() {
							onPlayFabClicked();
						}
					})
				);
			tabs.add(new Tab(R.string.links, GameLinksFragment.newInstance(gameId, gameName, iconColor)));
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
				return tabs.get(position).getFragment();
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
			Intent intent = ActivityUtils.createEditPlayIntent(context, gameId, gameName, thumbnailUrl, imageUrl);
			intent.putExtra(ActivityUtils.KEY_CUSTOM_PLAYER_SORT, arePlayersCustomSorted);
			startActivityForResult(intent, REQUEST_EDIT_PLAY);
		}

		@DebugLog
		private boolean shouldShowPlays() {
			return Authenticator.isSignedIn(getApplicationContext()) && PreferencesUtils.getSyncPlays(getApplicationContext());
		}

		@DebugLog
		private boolean shouldShowCollection() {
			String[] syncStatuses = PreferencesUtils.getSyncStatuses(getApplicationContext());
			return Authenticator.isSignedIn(getApplicationContext()) && syncStatuses != null && syncStatuses.length > 0;
		}
	}
}
