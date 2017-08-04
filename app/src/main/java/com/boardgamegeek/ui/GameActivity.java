package com.boardgamegeek.ui;

import android.content.ContentValues;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.FloatingActionButton.OnVisibilityChangedListener;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.events.GameInfoChangedEvent;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.tasks.FavoriteGameTask;
import com.boardgamegeek.tasks.sync.SyncCollectionByGameTask;
import com.boardgamegeek.tasks.sync.SyncGameTask;
import com.boardgamegeek.tasks.sync.SyncPlaysByGameTask;
import com.boardgamegeek.ui.model.GameToRefresh;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.ImageUtils.Callback;
import com.boardgamegeek.util.PaletteUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.ScrimUtils;
import com.boardgamegeek.util.ShortcutUtils;
import com.boardgamegeek.util.TaskUtils;
import com.boardgamegeek.util.UIUtils;
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

public class GameActivity extends HeroTabActivity implements Callback, LoaderCallbacks<Cursor> {
	private static final int AGE_IN_DAYS_TO_REFRESH = 7;
	private static final int REQUEST_EDIT_PLAY = 1;
	private static final int REFRESH_STATUS_NONE = 0;
	private static final int REFRESH_STATUS_GAME = 1;
	private static final int REFRESH_STATUS_PLAYS = 1 << 1;
	private static final int REFRESH_STATUS_COLLECTION = 1 << 2;

	private int gameId;
	private String gameName;
	private String imageUrl;
	private String thumbnailUrl;
	private boolean arePlayersCustomSorted;
	private boolean isFavorite;
	private boolean mightNeedRefreshing;
	private int refreshStatus;
	private GamePagerAdapter adapter;
	@ColorInt private int iconColor;
	@ColorInt private int darkColor;

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
		changeName(getIntent().getStringExtra(ActivityUtils.KEY_GAME_NAME));

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

		mightNeedRefreshing = true;
		LoaderManager lm = getSupportLoaderManager();
		lm.restartLoader(0, null, this);
	}

	@DebugLog
	@Override
	protected void setUpViewPager() {
		adapter = new GamePagerAdapter(getSupportFragmentManager());
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
			getIntent().putExtra(ActivityUtils.KEY_GAME_NAME, gameName);
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
			if (PreferencesUtils.showLogPlay(this)) {
				adapter.displayFab();
			}
		}
	}

	@Override
	public void onFailedImageLoad() {
		if (PreferencesUtils.showLogPlay(this)) {
			adapter.displayFab();
		}
	}

	//@Override
	public void onRefresh() {
		requestRefresh();
	}

	@DebugLog
	@OnClick(R.id.fab)
	public void onFabClicked() {
		Intent intent = ActivityUtils.createEditPlayIntent(this, gameId, gameName, thumbnailUrl, imageUrl);
		intent.putExtra(ActivityUtils.KEY_CUSTOM_PLAYER_SORT, arePlayersCustomSorted);
		startActivityForResult(intent, REQUEST_EDIT_PLAY);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(this, Games.buildGameUri(gameId), null, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (mightNeedRefreshing) {
			if (cursor == null || !cursor.moveToFirst()) {
				requestRefresh();
			} else {
				GameToRefresh game = GameToRefresh.fromCursor(cursor);
				if (DateTimeUtils.howManyDaysOld(game.getSyncedTimestampInMillis()) > AGE_IN_DAYS_TO_REFRESH || game.getPollsVoteCount() == 0)
					requestRefresh();
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	@DebugLog
	private void requestRefresh() {
		if (triggerRefresh()) {
			mightNeedRefreshing = false;
			//updateRefreshStatus(true);
		}
	}

	@DebugLog
	public boolean triggerRefresh() {
		if (refreshStatus == REFRESH_STATUS_NONE) {
			refreshStatus = REFRESH_STATUS_GAME | REFRESH_STATUS_PLAYS | REFRESH_STATUS_COLLECTION;
			TaskUtils.executeAsyncTask(new SyncGameTask(this, gameId));
			TaskUtils.executeAsyncTask(new SyncCollectionByGameTask(this, gameId));
			TaskUtils.executeAsyncTask(new SyncPlaysByGameTask(this, gameId));
			return true;
		}
		return false;
	}

	@SuppressWarnings("unused")
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(SyncCompleteEvent event) {
		if (event.getGameId() == gameId) {
			//updateRefreshStatus(false);
		}
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(SyncGameTask.CompletedEvent event) {
		if (event.getGameId() == gameId) {
			finishSync(REFRESH_STATUS_GAME);
		}
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(SyncPlaysByGameTask.CompletedEvent event) {
		if (event.getGameId() == gameId) {
			finishSync(REFRESH_STATUS_PLAYS);
		}
	}

	@SuppressWarnings("unused")
	@DebugLog
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(SyncCollectionByGameTask.CompletedEvent event) {
		if (event.getGameId() == gameId) {
			finishSync(REFRESH_STATUS_COLLECTION);
		}
	}

	private void finishSync(int syncType) {
		refreshStatus &= ~syncType;
		if (refreshStatus == REFRESH_STATUS_NONE) {
			EventBus.getDefault().post(new SyncCompleteEvent(gameId));
		}
	}

	public static class SyncCompleteEvent {
		private final int gameId;

		public SyncCompleteEvent(int gameId) {
			this.gameId = gameId;
		}

		public int getGameId() {
			return gameId;
		}
	}

	public static class ColorEvent {
		private final int gameId;
		@ColorInt private int iconColor;
		@ColorInt private int darkColor;

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

	private final class GamePagerAdapter extends FragmentPagerAdapter {
		public static final int INVALID_IMAGE_RES_ID = -1;

		private final class Tab {
			@StringRes private int titleResId;
			private String className;
			@DrawableRes private int imageResId;

			public Tab(int titleResId, String className) {
				this(titleResId, className, INVALID_IMAGE_RES_ID);
			}

			public Tab(int titleResId, String className, int imageResId) {
				this.titleResId = titleResId;
				this.className = className;
				this.imageResId = imageResId;
			}

			public int getTitleResId() {
				return titleResId;
			}

			public String getClassName() {
				return className;
			}

			public int getImageResId() {
				return imageResId;
			}
		}

		private List<Tab> tabs = new ArrayList<>();
		private int currentPosition;

		public GamePagerAdapter(FragmentManager fragmentManager) {
			super(fragmentManager);
			updateTabs();
		}

		@Override
		public void notifyDataSetChanged() {
			super.notifyDataSetChanged();
			updateTabs();
		}

		private void updateTabs() {
			tabs.clear();
			tabs.add(new Tab(R.string.title_description, GameFragment.class.getName()));
			if (shouldShowCollection())
				tabs.add(new Tab(R.string.title_collection, GameCollectionFragment.class.getName(), R.drawable.fab_add));
			if (shouldShowPlays())
				tabs.add(new Tab(R.string.title_plays, GamePlaysFragment.class.getName(), R.drawable.fab_log_play));
			tabs.add(new Tab(R.string.links, GameLinksFragment.class.getName()));
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
				String className = tabs.get(position).getClassName();
				if (!TextUtils.isEmpty(className)) {
					Bundle args = UIUtils.intentToFragmentArguments(getIntent());
					args.putInt(ActivityUtils.KEY_ICON_COLOR, iconColor);
					args.putInt(ActivityUtils.KEY_DARK_COLOR, darkColor);
					return Fragment.instantiate(GameActivity.this, className, args);
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
							public void onShown(FloatingActionButton fab) {
								super.onShown(fab);
							}

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
