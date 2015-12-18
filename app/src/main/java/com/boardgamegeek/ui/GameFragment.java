package com.boardgamegeek.ui;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.events.GameInfoChangedEvent;
import com.boardgamegeek.events.UpdateCompleteEvent;
import com.boardgamegeek.events.UpdateEvent;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggContract.Categories;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.GameRanks;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.GamesExpansions;
import com.boardgamegeek.provider.BggContract.Mechanics;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.provider.BggContract.Publishers;
import com.boardgamegeek.service.UpdateService;
import com.boardgamegeek.ui.adapter.GameColorAdapter;
import com.boardgamegeek.ui.widget.GameCollectionRow;
import com.boardgamegeek.ui.widget.GameDetailRow;
import com.boardgamegeek.ui.widget.ObservableScrollView;
import com.boardgamegeek.ui.widget.ObservableScrollView.Callbacks;
import com.boardgamegeek.ui.widget.StatBar;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.AnimationUtils;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.CursorUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.HelpUtils;
import com.boardgamegeek.util.ImageUtils;
import com.boardgamegeek.util.ImageUtils.Callback;
import com.boardgamegeek.util.PaletteUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.ScrimUtils;
import com.boardgamegeek.util.UIUtils;
import com.boardgamegeek.util.VersionUtils;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.InjectViews;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;
import timber.log.Timber;

public class GameFragment extends Fragment implements LoaderCallbacks<Cursor>, Callback, Callbacks, OnRefreshListener {
	private static final int HELP_VERSION = 1;
	private static final int AGE_IN_DAYS_TO_REFRESH = 7;
	private static final String KEY_DESCRIPTION_EXPANDED = "DESCRIPTION_EXPANDED";
	private static final String KEY_STATS_EXPANDED = "STATS_EXPANDED";
	private static final String KEY_LINKS_EXPANDED = "LINKS_EXPANDED";
	private static final int TIME_HINT_UPDATE_INTERVAL = 30000; // 30 sec

	private Handler timeHintUpdateHandler = new Handler();
	private Runnable timeHintUpdateRunnable = null;
	private Uri gameUri;
	private String gameName;
	private String imageUrl;
	private String thumbnailUrl;
	private boolean arePlayersCustomSorted;
	private boolean isSyncing;

	@SuppressWarnings("unused") @InjectView(R.id.swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
	@SuppressWarnings("unused") @InjectView(R.id.scroll_root) ObservableScrollView scrollRoot;
	@SuppressWarnings("unused") @InjectView(R.id.progress) View progressView;
	@SuppressWarnings("unused") @InjectView(R.id.hero_container) View heroContainer;
	@SuppressWarnings("unused") @InjectView(R.id.image) ImageView imageView;
	@SuppressWarnings("unused") @InjectView(R.id.header_container) View headerContainer;
	@SuppressWarnings("unused") @InjectView(R.id.game_info_name) TextView nameView;
	@SuppressWarnings("unused") @InjectView(R.id.game_rating) TextView ratingView;
	@SuppressWarnings("unused") @InjectView(R.id.game_description) TextView descriptionView;
	@SuppressWarnings("unused") @InjectView(R.id.game_rank) TextView rankView;
	@SuppressWarnings("unused") @InjectView(R.id.game_year_published) TextView yearPublishedView;

	@SuppressWarnings("unused") @InjectView(R.id.primary_info_container) View primaryInfoContainer;
	@SuppressWarnings("unused") @InjectView(R.id.number_of_players) TextView numberOfPlayersView;
	@SuppressWarnings("unused") @InjectView(R.id.play_time) TextView playTimeView;
	@SuppressWarnings("unused") @InjectView(R.id.player_age) TextView playerAgeView;

	@SuppressWarnings("unused") @InjectView(R.id.game_subtype_container) ViewGroup subtypeContainer;
	@SuppressWarnings("unused") @InjectView(R.id.game_subtype) TextView subtypeView;

	@SuppressWarnings("unused") @InjectView(R.id.game_info_designers) GameDetailRow designersView;
	@SuppressWarnings("unused") @InjectView(R.id.game_info_artists) GameDetailRow artistsView;
	@SuppressWarnings("unused") @InjectView(R.id.game_info_publishers) GameDetailRow publishersView;
	@SuppressWarnings("unused") @InjectView(R.id.game_info_categories) GameDetailRow categoriesView;
	@SuppressWarnings("unused") @InjectView(R.id.game_info_mechanics) GameDetailRow mechanicsView;
	@SuppressWarnings("unused") @InjectView(R.id.game_info_expansions) GameDetailRow expansionsView;
	@SuppressWarnings("unused") @InjectView(R.id.game_info_base_games) GameDetailRow baseGamesView;

	@SuppressWarnings("unused") @InjectView(R.id.collection_card) View collectionCard;
	@SuppressWarnings("unused") @InjectView(R.id.collection_container) ViewGroup collectionContainer;

	@SuppressWarnings("unused") @InjectView(R.id.plays_card) View playsCard;
	@SuppressWarnings("unused") @InjectView(R.id.plays_root) View playsRoot;
	@SuppressWarnings("unused") @InjectView(R.id.plays_label) TextView playsLabel;
	@SuppressWarnings("unused") @InjectView(R.id.play_stats_root) View playStatsRoot;
	@SuppressWarnings("unused") @InjectView(R.id.colors_root) View colorsRoot;
	@SuppressWarnings("unused") @InjectView(R.id.game_colors_label) TextView colorsLabel;

	@SuppressWarnings("unused") @InjectView(R.id.game_comments_label) TextView commentsLabel;
	@SuppressWarnings("unused") @InjectView(R.id.game_ratings_label) TextView ratingsLabel;

	@SuppressWarnings("unused") @InjectView(R.id.game_stats_label) TextView statsLabel;
	@SuppressWarnings("unused") @InjectView(R.id.game_stats_content) View statsContent;
	@SuppressWarnings("unused") @InjectView(R.id.game_stats_rating_count) TextView ratingsCountView;
	@SuppressWarnings("unused") @InjectView(R.id.game_stats_average_bar) StatBar averageStatBar;
	@SuppressWarnings("unused") @InjectView(R.id.game_stats_bayes_bar) StatBar bayesAverageBar;
	@SuppressWarnings("unused") @InjectView(R.id.game_stats_median_bar) StatBar medianBar;
	@SuppressWarnings("unused") @InjectView(R.id.game_stats_stddev_bar) StatBar standardDeviationBar;
	@SuppressWarnings("unused") @InjectView(R.id.game_stats_weight_count) TextView weightCountView;
	@SuppressWarnings("unused") @InjectView(R.id.game_stats_weight_bar) StatBar weightBar;
	@SuppressWarnings("unused") @InjectView(R.id.game_stats_users_count) TextView userCountView;
	@SuppressWarnings("unused") @InjectView(R.id.game_stats_owning_bar) StatBar numberOwningBar;
	@SuppressWarnings("unused") @InjectView(R.id.game_stats_rating_bar) StatBar numberRatingBar;
	@SuppressWarnings("unused") @InjectView(R.id.game_stats_trading_bar) StatBar numberTradingBar;
	@SuppressWarnings("unused") @InjectView(R.id.game_stats_wanting_bar) StatBar numberWantingBar;
	@SuppressWarnings("unused") @InjectView(R.id.game_stats_wishing_bar) StatBar numberWishingBar;
	@SuppressWarnings("unused") @InjectView(R.id.game_stats_weighting_bar) StatBar numberWeightingBar;

	@SuppressWarnings("unused") @InjectView(R.id.game_links_label) TextView linksLabel;
	@SuppressWarnings("unused") @InjectView(R.id.game_links_content) View linksContent;

	@SuppressWarnings("unused") @InjectView(R.id.game_info_id) TextView idView;
	@SuppressWarnings("unused") @InjectView(R.id.game_info_last_updated) TextView updatedView;

	@SuppressWarnings("unused") @InjectViews({
		R.id.number_of_players,
		R.id.play_time,
		R.id.player_age
	}) List<TextView> colorizedTextViews;
	@SuppressWarnings("unused") @InjectViews({
		R.id.game_info_designers,
		R.id.game_info_artists,
		R.id.game_info_publishers,
		R.id.game_info_categories,
		R.id.game_info_mechanics,
		R.id.game_info_expansions,
		R.id.game_info_base_games
	}) List<GameDetailRow> colorizedRows;
	@SuppressWarnings("unused") @InjectViews({
		R.id.card_header_details,
		R.id.card_header_collection,
		R.id.card_header_plays,
		R.id.card_header_user_feedback
	}) List<TextView> colorizedHeaders;
	@SuppressWarnings("unused") @InjectViews({
		R.id.icon_plays,
		R.id.icon_play_stats,
		R.id.icon_colors,
		R.id.icon_forums,
		R.id.icon_comments,
		R.id.icon_ratings,
		R.id.icon_stats,
		R.id.icon_links
	}) List<ImageView> colorizedIcons;

	private boolean isDescriptionExpanded;
	private boolean isStatsExpanded;
	private boolean isLinksExpanded;
	private final NumberFormat numberFormat = NumberFormat.getInstance();
	private boolean mightNeedRefreshing;
	private Palette palette;

	private final ViewTreeObserver.OnGlobalLayoutListener globalLayoutListener
		= new ViewTreeObserver.OnGlobalLayoutListener() {
		@Override
		public void onGlobalLayout() {
			ImageUtils.resizeImagePerAspectRatio(imageView, scrollRoot.getHeight() / 2, heroContainer);
		}
	};

	@Override
	@DebugLog
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		timeHintUpdateHandler = new Handler();

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		gameUri = intent.getData();

		if (gameUri == null) {
			return;
		}

		if (savedInstanceState != null) {
			isDescriptionExpanded = savedInstanceState.getBoolean(KEY_DESCRIPTION_EXPANDED);
			isStatsExpanded = savedInstanceState.getBoolean(KEY_STATS_EXPANDED);
			isLinksExpanded = savedInstanceState.getBoolean(KEY_LINKS_EXPANDED);
		}

		HelpUtils.showHelpDialog(getActivity(), HelpUtils.HELP_GAME_KEY, HELP_VERSION, R.string.help_boardgame);
	}

	@Override
	@DebugLog
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_game, container, false);
		ButterKnife.inject(this, rootView);

		swipeRefreshLayout.setOnRefreshListener(this);
		swipeRefreshLayout.setColorSchemeResources(R.color.primary_dark, R.color.primary);

		colorize();
		openOrCloseDescription();
		openOrCloseStats();
		openOrCloseLinks();
		ScrimUtils.applyDefaultScrim(headerContainer);
		scrollRoot.addCallbacks(this);
		ViewTreeObserver vto = scrollRoot.getViewTreeObserver();
		if (vto.isAlive()) {
			vto.addOnGlobalLayoutListener(globalLayoutListener);
		}

		mightNeedRefreshing = true;
		LoaderManager lm = getLoaderManager();
		lm.restartLoader(GameQuery._TOKEN, null, this);
		lm.restartLoader(RankQuery._TOKEN, null, this);
		lm.restartLoader(CollectionQuery._TOKEN, null, this);
		if (shouldShowPlays()) {
			lm.restartLoader(PlaysQuery._TOKEN, null, this);
		}
		if (PreferencesUtils.showLogPlay(getActivity())) {
			lm.restartLoader(ColorQuery._TOKEN, null, this);
		}
		return rootView;
	}

	@DebugLog
	@Override
	public void onStart() {
		super.onStart();
		EventBus.getDefault().registerSticky(this);
	}

	@Override
	@DebugLog
	public void onResume() {
		super.onResume();
		if (timeHintUpdateRunnable != null) {
			timeHintUpdateHandler.postDelayed(timeHintUpdateRunnable, TIME_HINT_UPDATE_INTERVAL);
		}
	}

	@Override
	@DebugLog
	public void onPause() {
		super.onPause();
		if (timeHintUpdateRunnable != null) {
			timeHintUpdateHandler.removeCallbacks(timeHintUpdateRunnable);
		}
	}

	@Override
	public void onStop() {
		EventBus.getDefault().unregister(this);
		super.onStop();
	}

	@Override
	@DebugLog
	public void onDestroyView() {
		super.onDestroyView();
		ButterKnife.reset(this);
	}

	@Override
	@DebugLog
	public void onDestroy() {
		super.onDestroy();
		if (scrollRoot == null) {
			return;
		}

		ViewTreeObserver vto = scrollRoot.getViewTreeObserver();
		if (vto.isAlive()) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
				//noinspection deprecation
				vto.removeGlobalOnLayoutListener(globalLayoutListener);
			} else {
				vto.removeOnGlobalLayoutListener(globalLayoutListener);
			}
		}
	}

	@Override
	@DebugLog
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(KEY_DESCRIPTION_EXPANDED, isDescriptionExpanded);
		outState.putBoolean(KEY_STATS_EXPANDED, isStatsExpanded);
		outState.putBoolean(KEY_LINKS_EXPANDED, isLinksExpanded);
	}

	@Override
	public void onRefresh() {
		triggerRefresh();
	}

	@Override
	@DebugLog
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = null;
		int gameId = Games.getGameId(gameUri);
		switch (id) {
			case GameQuery._TOKEN:
				loader = new CursorLoader(getActivity(), gameUri, GameQuery.PROJECTION, null, null, null);
				break;
			case DesignerQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Games.buildDesignersUri(gameId), DesignerQuery.PROJECTION, null, null, null);
				break;
			case ArtistQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Games.buildArtistsUri(gameId), ArtistQuery.PROJECTION, null, null, null);
				break;
			case PublisherQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Games.buildPublishersUri(gameId), PublisherQuery.PROJECTION, null, null, null);
				break;
			case CategoryQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Games.buildCategoriesUri(gameId), CategoryQuery.PROJECTION, null, null, null);
				break;
			case MechanicQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Games.buildMechanicsUri(gameId), MechanicQuery.PROJECTION, null, null, null);
				break;
			case ExpansionQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Games.buildExpansionsUri(gameId), ExpansionQuery.PROJECTION, GamesExpansions.INBOUND + "=?", new String[] { "0" }, null);
				break;
			case BaseGameQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Games.buildExpansionsUri(gameId), BaseGameQuery.PROJECTION, GamesExpansions.INBOUND + "=?", new String[] { "1" }, null);
				break;
			case RankQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Games.buildRanksUri(gameId), RankQuery.PROJECTION, null, null, null);
				break;
			case CollectionQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Collection.CONTENT_URI, CollectionQuery.PROJECTION, "collection." + Collection.GAME_ID + "=?", new String[] { String.valueOf(gameId) }, null);
				break;
			case PlaysQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Plays.CONTENT_URI, PlaysQuery.PROJECTION,
					PlayItems.OBJECT_ID + "=?", new String[] { String.valueOf(gameId) }, null);
				break;
			case ColorQuery._TOKEN:
				loader = new CursorLoader(getActivity(), GameColorAdapter.createUri(gameId),
					GameColorAdapter.PROJECTION, null, null, null);
				break;
			default:
				Timber.w("Invalid query token=" + id);
				break;
		}
		return loader;
	}

	@Override
	@DebugLog
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		switch (loader.getId()) {
			case GameQuery._TOKEN:
				onGameQueryComplete(cursor);
				LoaderManager lm = getLoaderManager();
				lm.restartLoader(DesignerQuery._TOKEN, null, this);
				lm.restartLoader(ArtistQuery._TOKEN, null, this);
				lm.restartLoader(PublisherQuery._TOKEN, null, this);
				lm.restartLoader(CategoryQuery._TOKEN, null, this);
				lm.restartLoader(MechanicQuery._TOKEN, null, this);
				lm.restartLoader(ExpansionQuery._TOKEN, null, this);
				lm.restartLoader(BaseGameQuery._TOKEN, null, this);
				break;
			case DesignerQuery._TOKEN:
				onListQueryComplete(cursor, designersView, DesignerQuery.DESIGNER_NAME, DesignerQuery.DESIGNER_ID);
				break;
			case ArtistQuery._TOKEN:
				onListQueryComplete(cursor, artistsView, ArtistQuery.ARTIST_NAME, ArtistQuery.ARTIST_ID);
				break;
			case PublisherQuery._TOKEN:
				onListQueryComplete(cursor, publishersView, PublisherQuery.PUBLISHER_NAME, PublisherQuery.PUBLISHER_ID);
				break;
			case CategoryQuery._TOKEN:
				onListQueryComplete(cursor, categoriesView, CategoryQuery.CATEGORY_NAME, CategoryQuery.CATEGORY_ID);
				break;
			case MechanicQuery._TOKEN:
				onListQueryComplete(cursor, mechanicsView, MechanicQuery.MECHANIC_NAME, MechanicQuery.MECHANIC_ID);
				break;
			case ExpansionQuery._TOKEN:
				onListQueryComplete(cursor, expansionsView, ExpansionQuery.EXPANSION_NAME, ExpansionQuery.EXPANSION_ID);
				break;
			case BaseGameQuery._TOKEN:
				onListQueryComplete(cursor, baseGamesView, BaseGameQuery.EXPANSION_NAME, BaseGameQuery.EXPANSION_ID);
				break;
			case RankQuery._TOKEN:
				onRankQueryComplete(cursor);
				break;
			case CollectionQuery._TOKEN:
				onCollectionQueryComplete(cursor);
				break;
			case PlaysQuery._TOKEN:
				onPlaysQueryComplete(cursor);
				break;
			case ColorQuery._TOKEN:
				playsCard.setVisibility(View.VISIBLE);
				colorsRoot.setVisibility(View.VISIBLE);
				int count = cursor.getCount();
				colorsLabel.setText(getResources().getQuantityString(R.plurals.colors_suffix, count, count));
				break;
			default:
				cursor.close();
				break;
		}
	}

	@Override
	@DebugLog
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	@DebugLog
	public void onScrollChanged(int deltaX, int deltaY) {
		if (VersionUtils.hasHoneycomb()) {
			int scrollY = scrollRoot.getScrollY();
			imageView.setTranslationY(scrollY * 0.5f);
			headerContainer.setTranslationY(scrollY * 0.5f);
		}
	}

	@Override
	@DebugLog
	public void onPaletteGenerated(Palette palette) {
		this.palette = palette;
		colorize();
	}

	@SuppressWarnings("unused")
	@DebugLog
	public void onEventMainThread(UpdateEvent event) {
		isSyncing = event.getType() == UpdateService.SYNC_TYPE_GAME;
		updateRefreshStatus();
	}

	@SuppressWarnings("unused")
	@DebugLog
	public void onEventMainThread(UpdateCompleteEvent event) {
		isSyncing = false;
		updateRefreshStatus();
	}

	@DebugLog
	private void updateRefreshStatus() {
		if (swipeRefreshLayout != null) {
			swipeRefreshLayout.post(new Runnable() {
				@Override
				public void run() {
					if (swipeRefreshLayout != null) {
						swipeRefreshLayout.setRefreshing(isSyncing);
					}
				}
			});
		}
	}

	@DebugLog
	private void colorize() {
		if (palette == null || primaryInfoContainer == null) {
			return;
		}
		Palette.Swatch swatch = PaletteUtils.getInverseSwatch(palette, getResources().getColor(R.color.info_background));
		primaryInfoContainer.setBackgroundColor(swatch.getRgb());
		ButterKnife.apply(colorizedTextViews, PaletteUtils.colorTextViewOnBackgroundSetter, swatch);
		swatch = PaletteUtils.getIconSwatch(palette);
		ButterKnife.apply(colorizedRows, GameDetailRow.colorIconSetter, swatch);
		ButterKnife.apply(colorizedIcons, PaletteUtils.colorIconSetter, swatch);
		swatch = PaletteUtils.getHeaderSwatch(palette);
		ButterKnife.apply(colorizedHeaders, PaletteUtils.colorTextViewSetter, swatch);
	}

	@DebugLog
	private void onGameQueryComplete(Cursor cursor) {
		if (cursor == null || !cursor.moveToFirst()) {
			if (mightNeedRefreshing) {
				triggerRefresh();
			}
			return;
		}

		Game game = new Game(cursor);

		notifyChange(game);
		gameName = game.Name;
		imageUrl = game.ImageUrl;
		thumbnailUrl = game.ThumbnailUrl;
		arePlayersCustomSorted = game.CustomPlayerSort;

		ImageUtils.safelyLoadImage(imageView, game.ImageUrl, this);
		nameView.setText(game.Name);
		rankView.setText(game.getRankDescription());
		yearPublishedView.setText(game.getYearPublished());
		subtypeView.setText(game.getSubtype());
		ratingView.setText(game.getRatingDescription());
		ColorUtils.setViewBackground(ratingView, ColorUtils.getRatingColor(game.Rating));
		idView.setText(String.valueOf(game.Id));
		updatedView.setTag(game.Updated);
		UIUtils.setTextMaybeHtml(descriptionView, game.Description);
		numberOfPlayersView.setText(game.getPlayerRangeDescription());
		playTimeView.setText(game.getPlayingTimeDescription());
		playerAgeView.setText(game.getAgeDescription());
		commentsLabel.setText(getResources().getQuantityString(R.plurals.comments_suffix, game.UsersCommented, game.UsersCommented));
		ratingsLabel.setText(getResources().getQuantityString(R.plurals.ratings_suffix, game.UsersRated, game.UsersRated));

		ratingsCountView.setText(String.format(getResources().getString(R.string.rating_count), numberFormat.format(game.UsersRated)));
		averageStatBar.setBar(R.string.average_meter_text, game.Rating);
		bayesAverageBar.setBar(R.string.bayes_meter_text, game.BayesAverage);
		if (game.Median <= 0) {
			medianBar.setVisibility(View.GONE);
		} else {
			medianBar.setVisibility(View.VISIBLE);
			medianBar.setBar(R.string.median_meter_text, game.Median);
		}
		standardDeviationBar.setBar(R.string.stdDev_meter_text, game.StandardDeviation, 5.0);

		weightCountView.setText(String.format(getResources().getString(R.string.weight_count), numberFormat.format(game.NumberWeights)));
		weightBar.setBar(game.getWeightDescriptionResId(), game.AverageWeight, 5.0, 1.0);

		userCountView.setText(String.format(getResources().getString(R.string.user_total), numberFormat.format(game.getMaxUsers())));
		numberOwningBar.setBar(R.string.owning_meter_text, game.NumberOwned, game.getMaxUsers());
		numberRatingBar.setBar(R.string.rating_meter_text, game.UsersRated, game.getMaxUsers());
		numberTradingBar.setBar(R.string.trading_meter_text, game.NumberTrading, game.getMaxUsers());
		numberWantingBar.setBar(R.string.wanting_meter_text, game.NumberWanting, game.getMaxUsers());
		numberWishingBar.setBar(R.string.wishing_meter_text, game.NumberWishing, game.getMaxUsers());
		numberWeightingBar.setBar(R.string.weighting_meter_text, game.NumberWeights, game.getMaxUsers());

		if (shouldShowPlays()) {
			playsCard.setVisibility(View.VISIBLE);
			playStatsRoot.setVisibility(View.VISIBLE);
		}

		updateTimeBasedUi();
		if (timeHintUpdateRunnable != null) {
			timeHintUpdateHandler.removeCallbacks(timeHintUpdateRunnable);
		}
		timeHintUpdateRunnable = new Runnable() {
			@Override
			public void run() {
				updateTimeBasedUi();
				timeHintUpdateHandler.postDelayed(timeHintUpdateRunnable, TIME_HINT_UPDATE_INTERVAL);
			}
		};
		timeHintUpdateHandler.postDelayed(timeHintUpdateRunnable, TIME_HINT_UPDATE_INTERVAL);

		AnimationUtils.fadeOut(getActivity(), progressView, true);
		AnimationUtils.fadeIn(getActivity(), swipeRefreshLayout, true);

		if (mightNeedRefreshing
			&& (game.PollsCount == 0 || DateTimeUtils.howManyDaysOld(game.Updated) > AGE_IN_DAYS_TO_REFRESH)) {
			triggerRefresh();
		}
		mightNeedRefreshing = false;
	}

	@DebugLog
	private void updateTimeBasedUi() {
		if (!isAdded()) {
			return;
		}
		if (updatedView != null) {
			long updatedTime = (long) updatedView.getTag();
			updatedView.setText(PresentationUtils.describePastTimeSpan(updatedTime, getResources().getString(R.string.needs_updating)));
		}
	}

	@DebugLog
	private boolean shouldShowPlays() {
		return Authenticator.isSignedIn(getActivity()) && PreferencesUtils.getSyncPlays(getActivity());
	}

	@DebugLog
	private void notifyChange(Game game) {
		GameInfoChangedEvent event = new GameInfoChangedEvent(game.Name, game.Subtype, game.ImageUrl, game.ThumbnailUrl, game.CustomPlayerSort);
		EventBus.getDefault().post(event);
	}

	@DebugLog
	private void onListQueryComplete(Cursor cursor, GameDetailRow view, int nameColumnIndex, int idColumnIndex) {
		if (cursor == null || !cursor.moveToFirst()) {
			view.setVisibility(View.GONE);
			view.clear();
		} else {
			view.setVisibility(View.VISIBLE);
			view.bind(cursor, nameColumnIndex, idColumnIndex, Games.getGameId(gameUri), gameName);
		}
	}

	@DebugLog
	private void onRankQueryComplete(Cursor cursor) {
		if (subtypeContainer != null) {
			subtypeContainer.removeAllViews();
			if (cursor != null && cursor.getCount() > 0) {
				while (cursor.moveToNext()) {
					Rank rank = new Rank(cursor);
					if (!"subtype".equals(rank.Type)) {
						addRankRow(rank.Name, rank.Rank, rank.Rating);
					}
				}
			}
		}
	}

	@DebugLog
	private void addRankRow(String label, int rank, double rating) {
		LinearLayout layout = (LinearLayout) getLayoutInflater(null).inflate(R.layout.widget_rank_row, subtypeContainer, false);

		if (label.endsWith(" Rank")) {
			label = label.substring(0, label.length() - 5);
		}
		TextView tv = (TextView) layout.findViewById(R.id.rank_row_label);
		tv.setText(label);

		tv = (TextView) layout.findViewById(R.id.rank_row_rank);
		tv.setText(PresentationUtils.describeRank(rank));

		tv = (TextView) layout.findViewById(R.id.rank_row_rating);
		tv.setText(PresentationUtils.describeAverageRating(getActivity(), rating));
		ColorUtils.setViewBackground(tv, ColorUtils.getRatingColor(rating));

		subtypeContainer.addView(layout);
	}

	@DebugLog
	private void onCollectionQueryComplete(Cursor cursor) {
		if (cursor.moveToFirst()) {
			collectionCard.setVisibility(View.VISIBLE);
			collectionContainer.removeViews(1, collectionContainer.getChildCount() - 1);
			do {
				GameCollectionRow row = new GameCollectionRow(getActivity());

				final int gameId = Games.getGameId(gameUri);
				final int collectionId = cursor.getInt(CollectionQuery.COLLECTION_ID);
				final int yearPublished = cursor.getInt(CollectionQuery.YEAR_PUBLISHED);
				row.bind(gameId, gameName, collectionId, yearPublished);

				final String thumbnailUrl = cursor.getString(CollectionQuery.COLLECTION_THUMBNAIL);
				final String collectionName = cursor.getString(CollectionQuery.COLLECTION_NAME);
				final int collectionYearPublished = cursor.getInt(CollectionQuery.COLLECTION_YEAR);
				final int numberOfPlays = cursor.getInt(CollectionQuery.NUM_PLAYS);
				final String comment = cursor.getString(CollectionQuery.COMMENT);
				final double rating = cursor.getDouble(CollectionQuery.RATING);
				List<String> status = new ArrayList<>();
				for (int i = CollectionQuery.STATUS_1; i <= CollectionQuery.STATUS_N; i++) {
					if (cursor.getInt(i) == 1) {
						if (i == CollectionQuery.STATUS_WISHLIST) {
							status.add(PresentationUtils.describeWishlist(getActivity(),
								cursor.getInt(CollectionQuery.STATUS_WISHLIST_PRIORITY)));
						} else {
							int index = i - CollectionQuery.STATUS_1;
							status.add(getResources().getStringArray(R.array.collection_status_filter_entries)[index]);
						}
					}
				}

				row.setThumbnail(thumbnailUrl);
				row.setStatus(status, numberOfPlays, rating, comment);
				row.setDescription(collectionName, collectionYearPublished);
				row.setComment(comment);
				row.setRating(rating);

				collectionContainer.addView(row);
			} while (cursor.moveToNext());
		}
	}

	@DebugLog
	private void onPlaysQueryComplete(Cursor cursor) {
		if (cursor.moveToFirst()) {
			playsCard.setVisibility(View.VISIBLE);
			playsRoot.setVisibility(View.VISIBLE);
			int sum = cursor.getInt(PlaysQuery.SUM_QUANTITY);
			if (sum > 0) {
				String date = CursorUtils.getFormattedDate(cursor, getActivity(), PlaysQuery.MAX_DATE);
				playsLabel.setText(getResources().getQuantityString(R.plurals.plays_summary, sum, sum, date));
			} else {
				playsLabel.setText(getResources().getString(R.string.no_plays));
			}
		}
	}

	@SuppressWarnings("unused")
	@OnClick(R.id.image)
	@DebugLog
	public void onThumbnailClick(View v) {
		if (!TextUtils.isEmpty(imageUrl)) {
			final Intent intent = new Intent(getActivity(), ImageActivity.class);
			intent.putExtra(ActivityUtils.KEY_IMAGE_URL, imageUrl);
			startActivity(intent);
		}
	}

	@SuppressWarnings("unused")
	@OnClick(R.id.game_description)
	@DebugLog
	public void onDescriptionClick(View v) {
		isDescriptionExpanded = !isDescriptionExpanded;
		openOrCloseDescription();
	}

	@SuppressWarnings("unused")
	@OnClick(R.id.plays_root)
	@DebugLog
	public void onPlaysClick(View v) {
		Intent intent = new Intent(getActivity(), GamePlaysActivity.class);
		intent.setData(gameUri);
		intent.putExtra(ActivityUtils.KEY_GAME_NAME, gameName);
		intent.putExtra(ActivityUtils.KEY_IMAGE_URL, imageUrl);
		intent.putExtra(ActivityUtils.KEY_THUMBNAIL_URL, thumbnailUrl);
		intent.putExtra(ActivityUtils.KEY_CUSTOM_PLAYER_SORT, arePlayersCustomSorted);
		startActivity(intent);
	}

	@SuppressWarnings("unused")
	@OnClick(R.id.play_stats_root)
	@DebugLog
	public void onPlayStatsClick(View v) {
		Intent intent = new Intent(getActivity(), GamePlayStatsActivity.class);
		intent.setData(gameUri);
		intent.putExtra(ActivityUtils.KEY_GAME_NAME, gameName);
		startActivity(intent);
	}

	@SuppressWarnings("unused")
	@OnClick(R.id.colors_root)
	@DebugLog
	public void onColorsClick(View v) {
		Intent intent = new Intent(getActivity(), ColorsActivity.class);
		intent.setData(gameUri);
		intent.putExtra(ActivityUtils.KEY_GAME_NAME, gameName);
		startActivity(intent);
	}

	@SuppressWarnings("unused")
	@OnClick(R.id.forums_root)
	@DebugLog
	public void onForumsClick(View v) {
		Intent intent = new Intent(getActivity(), GameForumsActivity.class);
		intent.setData(gameUri);
		intent.putExtra(ActivityUtils.KEY_GAME_NAME, gameName);
		startActivity(intent);
	}

	@SuppressWarnings("unused")
	@OnClick(R.id.comments_root)
	@DebugLog
	public void onCommentsClick(@SuppressWarnings("UnusedParameters") View v) {
		Intent intent = new Intent(getActivity(), CommentsActivity.class);
		intent.setData(gameUri);
		intent.putExtra(ActivityUtils.KEY_GAME_NAME, gameName);
		startActivity(intent);
	}

	@SuppressWarnings("unused")
	@OnClick(R.id.ratings_root)
	public void onRatingsClick(@SuppressWarnings("UnusedParameters") View v) {
		Intent intent = new Intent(getActivity(), CommentsActivity.class);
		intent.setData(gameUri);
		intent.putExtra(ActivityUtils.KEY_GAME_NAME, gameName);
		intent.putExtra(ActivityUtils.KEY_SORT, CommentsActivity.SORT_RATING);
		startActivity(intent);
	}

	@SuppressWarnings("unused")
	@OnClick(R.id.game_info_stats_root)
	@DebugLog
	public void onStatsClick(@SuppressWarnings("UnusedParameters") View v) {
		isStatsExpanded = !isStatsExpanded;
		openOrCloseStats();
	}

	@SuppressWarnings("unused")
	@OnClick(R.id.game_info_links_root)
	@DebugLog
	public void onLinksClick(@SuppressWarnings("UnusedParameters") View v) {
		isLinksExpanded = !isLinksExpanded;
		openOrCloseLinks();
	}

	@DebugLog
	private void openOrCloseDescription() {
		descriptionView.setMaxLines(isDescriptionExpanded ? Integer.MAX_VALUE : 3);
		descriptionView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0,
			isDescriptionExpanded ? R.drawable.expander_close : R.drawable.expander_open);
	}

	@DebugLog
	private void openOrCloseStats() {
		statsContent.setVisibility(isStatsExpanded ? View.VISIBLE : View.GONE);
		statsLabel.setCompoundDrawablesWithIntrinsicBounds(0, 0, isStatsExpanded ? R.drawable.expander_close : R.drawable.expander_open, 0);
	}

	@DebugLog
	private void openOrCloseLinks() {
		linksContent.setVisibility(isLinksExpanded ? View.VISIBLE : View.GONE);
		linksLabel.setCompoundDrawablesWithIntrinsicBounds(0, 0, isLinksExpanded ? R.drawable.expander_close : R.drawable.expander_open, 0);
	}

	@SuppressWarnings("unused")
	@DebugLog
	@OnClick({ R.id.link_bgg, R.id.link_bg_prices, R.id.link_amazon, R.id.link_ebay })
	void onLinkClick(View v) {
		switch (v.getId()) {
			case R.id.link_bgg:
				ActivityUtils.linkBgg(getActivity(), Games.getGameId(gameUri));
				break;
			case R.id.link_bg_prices:
				ActivityUtils.linkBgPrices(getActivity(), gameName);
				break;
			case R.id.link_amazon:
				ActivityUtils.linkAmazon(getActivity(), gameName);
				break;
			case R.id.link_ebay:
				ActivityUtils.linkEbay(getActivity(), gameName);
				break;
		}
	}

	@SuppressWarnings("unused")
	@OnClick({ R.id.number_of_players, R.id.player_age })
	@DebugLog
	public void onPollClick(View v) {
		Bundle arguments = new Bundle(2);
		arguments.putInt(ActivityUtils.KEY_GAME_ID, Games.getGameId(gameUri));
		arguments.putString(ActivityUtils.KEY_TYPE, (String) v.getTag());
		DialogUtils.launchDialog(this, new PollFragment(), "poll-dialog", arguments);
	}

	@DebugLog
	private void triggerRefresh() {
		mightNeedRefreshing = false;
		int gameId = Games.getGameId(gameUri);
		UpdateService.start(getActivity(), UpdateService.SYNC_TYPE_GAME, gameId);
		UpdateService.start(getActivity(), UpdateService.SYNC_TYPE_GAME_COLLECTION, gameId);
		UpdateService.start(getActivity(), UpdateService.SYNC_TYPE_GAME_PLAYS, gameId);
	}

	private interface GameQuery {
		int _TOKEN = 0x11;

		String[] PROJECTION = { Games.GAME_ID, Games.STATS_AVERAGE, Games.YEAR_PUBLISHED, Games.MIN_PLAYERS,
			Games.MAX_PLAYERS, Games.PLAYING_TIME, Games.MINIMUM_AGE, Games.DESCRIPTION, Games.STATS_USERS_RATED,
			Games.UPDATED, Games.GAME_RANK, Games.GAME_NAME, Games.THUMBNAIL_URL, Games.STATS_BAYES_AVERAGE,
			Games.STATS_MEDIAN, Games.STATS_STANDARD_DEVIATION, Games.STATS_NUMBER_WEIGHTS, Games.STATS_AVERAGE_WEIGHT,
			Games.STATS_NUMBER_OWNED, Games.STATS_NUMBER_TRADING, Games.STATS_NUMBER_WANTING,
			Games.STATS_NUMBER_WISHING, Games.POLLS_COUNT, Games.IMAGE_URL, Games.SUBTYPE, Games.CUSTOM_PLAYER_SORT,
			Games.STATS_NUMBER_COMMENTS };

		int GAME_ID = 0;
		int STATS_AVERAGE = 1;
		int YEAR_PUBLISHED = 2;
		int MIN_PLAYERS = 3;
		int MAX_PLAYERS = 4;
		int PLAYING_TIME = 5;
		int MINIMUM_AGE = 6;
		int DESCRIPTION = 7;
		int STATS_USERS_RATED = 8;
		int UPDATED = 9;
		int GAME_RANK = 10;
		int GAME_NAME = 11;
		int THUMBNAIL_URL = 12;
		int STATS_BAYES_AVERAGE = 13;
		int STATS_MEDIAN = 14;
		int STATS_STANDARD_DEVIATION = 15;
		int STATS_NUMBER_WEIGHTS = 16;
		int STATS_AVERAGE_WEIGHT = 17;
		int STATS_NUMBER_OWNED = 18;
		int STATS_NUMBER_TRADING = 19;
		int STATS_NUMBER_WANTING = 20;
		int STATS_NUMBER_WISHING = 21;
		int POLLS_COUNT = 22;
		int IMAGE_URL = 23;
		int SUBTYPE = 24;
		int CUSTOM_PLAYER_SORT = 25;
		int STATS_NUMBER_COMMENTS = 26;
	}

	private interface DesignerQuery {
		int _TOKEN = 0x12;
		String[] PROJECTION = { Designers.DESIGNER_ID, Designers.DESIGNER_NAME, Designers._ID };
		int DESIGNER_ID = 0;
		int DESIGNER_NAME = 1;
	}

	private interface ArtistQuery {
		int _TOKEN = 0x13;
		String[] PROJECTION = { Artists.ARTIST_ID, Artists.ARTIST_NAME, Artists._ID };
		int ARTIST_ID = 0;
		int ARTIST_NAME = 1;
	}

	private interface PublisherQuery {
		int _TOKEN = 0x14;
		String[] PROJECTION = { Publishers.PUBLISHER_ID, Publishers.PUBLISHER_NAME, Publishers._ID };
		int PUBLISHER_ID = 0;
		int PUBLISHER_NAME = 1;
	}

	private interface CategoryQuery {
		int _TOKEN = 0x15;
		String[] PROJECTION = { Categories.CATEGORY_ID, Categories.CATEGORY_NAME, Categories._ID };
		int CATEGORY_ID = 0;
		int CATEGORY_NAME = 1;
	}

	private interface MechanicQuery {
		int _TOKEN = 0x16;
		String[] PROJECTION = { Mechanics.MECHANIC_ID, Mechanics.MECHANIC_NAME, Mechanics._ID };
		int MECHANIC_ID = 0;
		int MECHANIC_NAME = 1;
	}

	private interface ExpansionQuery {
		int _TOKEN = 0x17;
		String[] PROJECTION = { GamesExpansions.EXPANSION_ID, GamesExpansions.EXPANSION_NAME, GamesExpansions._ID };
		int EXPANSION_ID = 0;
		int EXPANSION_NAME = 1;
	}

	private interface BaseGameQuery {
		int _TOKEN = 0x18;
		String[] PROJECTION = { GamesExpansions.EXPANSION_ID, GamesExpansions.EXPANSION_NAME, GamesExpansions._ID };
		int EXPANSION_ID = 0;
		int EXPANSION_NAME = 1;
	}

	private interface RankQuery {
		int _TOKEN = 0x19;
		String[] PROJECTION = { GameRanks.GAME_RANK_FRIENDLY_NAME, GameRanks.GAME_RANK_VALUE, GameRanks.GAME_RANK_TYPE,
			GameRanks.GAME_RANK_BAYES_AVERAGE };
		int GAME_RANK_FRIENDLY_NAME = 0;
		int GAME_RANK_VALUE = 1;
		int GAME_RANK_TYPE = 2;
		int GAME_RANK_BAYES_AVERAGE = 3;
	}

	private interface CollectionQuery {
		String[] PROJECTION = { Collection._ID, Collection.COLLECTION_ID, Collection.COLLECTION_NAME,
			Collection.COLLECTION_YEAR_PUBLISHED, Collection.COLLECTION_THUMBNAIL_URL, Collection.STATUS_OWN,
			Collection.STATUS_PREVIOUSLY_OWNED, Collection.STATUS_FOR_TRADE, Collection.STATUS_WANT,
			Collection.STATUS_WANT_TO_BUY, Collection.STATUS_WISHLIST, Collection.STATUS_WANT_TO_PLAY,
			Collection.STATUS_PREORDERED, Collection.STATUS_WISHLIST_PRIORITY, Collection.NUM_PLAYS,
			Collection.COMMENT, Games.YEAR_PUBLISHED, Collection.RATING };
		int _TOKEN = 0x20;
		int COLLECTION_ID = 1;
		int COLLECTION_NAME = 2;
		int COLLECTION_YEAR = 3;
		int COLLECTION_THUMBNAIL = 4;
		int STATUS_1 = 5;
		int STATUS_N = 12;
		int STATUS_WISHLIST = 10;
		int STATUS_WISHLIST_PRIORITY = 13;
		int NUM_PLAYS = 14;
		int COMMENT = 15;
		int YEAR_PUBLISHED = 16;
		int RATING = 17;
	}

	private interface PlaysQuery {
		String[] PROJECTION = { Plays._ID, Plays.MAX_DATE, Plays.SUM_QUANTITY, Plays.MAX_DATE };
		int _TOKEN = 0x21;
		int MAX_DATE = 1;
		int SUM_QUANTITY = 2;
	}

	private interface ColorQuery {
		int _TOKEN = 0x22;
	}

	private class Game {
		final String Name;
		final String ThumbnailUrl;
		final String ImageUrl;
		final int Id;
		final double Rating;
		final int YearPublished;
		final int MinPlayers;
		final int MaxPlayers;
		final int PlayingTime;
		final int MinimumAge;
		final String Description;
		final int UsersRated;
		final int UsersCommented;
		final long Updated;
		final int Rank;
		final double BayesAverage;
		final double Median;
		final double StandardDeviation;
		final double AverageWeight;
		final int NumberWeights;
		final int NumberOwned;
		final int NumberTrading;
		final int NumberWanting;
		final int NumberWishing;
		final int PollsCount;
		final String Subtype;
		final boolean CustomPlayerSort;

		public Game(Cursor cursor) {
			Name = cursor.getString(GameQuery.GAME_NAME);
			ThumbnailUrl = cursor.getString(GameQuery.THUMBNAIL_URL);
			ImageUrl = cursor.getString(GameQuery.IMAGE_URL);
			Id = cursor.getInt(GameQuery.GAME_ID);
			Rating = cursor.getDouble(GameQuery.STATS_AVERAGE);
			YearPublished = cursor.getInt(GameQuery.YEAR_PUBLISHED);
			MinPlayers = cursor.getInt(GameQuery.MIN_PLAYERS);
			MaxPlayers = cursor.getInt(GameQuery.MAX_PLAYERS);
			PlayingTime = cursor.getInt(GameQuery.PLAYING_TIME);
			MinimumAge = cursor.getInt(GameQuery.MINIMUM_AGE);
			Description = cursor.getString(GameQuery.DESCRIPTION);
			UsersRated = cursor.getInt(GameQuery.STATS_USERS_RATED);
			UsersCommented = cursor.getInt(GameQuery.STATS_NUMBER_COMMENTS);
			Updated = cursor.getLong(GameQuery.UPDATED);
			Rank = cursor.getInt(GameQuery.GAME_RANK);
			BayesAverage = cursor.getDouble(GameQuery.STATS_BAYES_AVERAGE);
			Median = cursor.getDouble(GameQuery.STATS_MEDIAN);
			StandardDeviation = cursor.getDouble(GameQuery.STATS_STANDARD_DEVIATION);
			AverageWeight = cursor.getDouble(GameQuery.STATS_AVERAGE_WEIGHT);
			NumberWeights = cursor.getInt(GameQuery.STATS_NUMBER_WEIGHTS);
			NumberOwned = cursor.getInt(GameQuery.STATS_NUMBER_OWNED);
			NumberTrading = cursor.getInt(GameQuery.STATS_NUMBER_TRADING);
			NumberWanting = cursor.getInt(GameQuery.STATS_NUMBER_WANTING);
			NumberWishing = cursor.getInt(GameQuery.STATS_NUMBER_WISHING);
			PollsCount = cursor.getInt(GameQuery.POLLS_COUNT);
			Subtype = cursor.getString(GameQuery.SUBTYPE);
			CustomPlayerSort = (cursor.getInt(GameQuery.CUSTOM_PLAYER_SORT) == 1);
		}

		@DebugLog
		public String getAgeDescription() {
			if (MinimumAge > 0) {
				return MinimumAge + " " + getResources().getString(R.string.age_suffix);
			}
			return getResources().getString(R.string.text_unknown);
		}

		@DebugLog
		public int getMaxUsers() {
			int max = Math.max(UsersRated, NumberOwned);
			max = Math.max(max, NumberTrading);
			max = Math.max(max, NumberWanting);
			max = Math.max(max, NumberWeights);
			max = Math.max(max, NumberWishing);
			return max;
		}

		@DebugLog
		private String getPlayerRangeDescription() {
			if (MinPlayers == 0 && MaxPlayers == 0) {
				return getResources().getString(R.string.text_unknown);
			} else if (MinPlayers >= MaxPlayers) {
				return String.valueOf(MinPlayers);
			} else {
				return String.valueOf(MinPlayers) + " - " + String.valueOf(MaxPlayers);
			}
		}

		@DebugLog
		private String getPlayingTimeDescription() {
			if (PlayingTime > 0) {
				return PlayingTime + " " + getResources().getString(R.string.minutes_abbr);
			}
			return getResources().getString(R.string.text_unknown);
		}

		@DebugLog
		private String getRankDescription() {
			return PresentationUtils.describeRank(Rank);
		}

		@DebugLog
		public String getRatingDescription() {
			return PresentationUtils.describeAverageRating(getContext(), Rating);
		}

		@DebugLog
		public String getYearPublished() {
			return PresentationUtils.describeYear(getContext(), YearPublished);
		}

		@DebugLog
		public int getWeightDescriptionResId() {
			int resId = R.string.weight_1_text;
			if (AverageWeight >= 4.2) {
				resId = R.string.weight_5_text;
			} else if (AverageWeight >= 3.4) {
				resId = R.string.weight_4_text;
			} else if (AverageWeight >= 2.6) {
				resId = R.string.weight_3_text;
			} else if (AverageWeight >= 1.8) {
				resId = R.string.weight_2_text;
			}
			return resId;
		}

		@DebugLog
		public String getSubtype() {
			// TODO: improve these strings
			@StringRes int resId = R.string.title_game;
			switch (Subtype) {
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
			return getString(resId);
		}
	}

	private class Rank {
		final String Name;
		final int Rank;
		final double Rating;
		final String Type;

		Rank(Cursor cursor) {
			Name = cursor.getString(RankQuery.GAME_RANK_FRIENDLY_NAME);
			Rank = cursor.getInt(RankQuery.GAME_RANK_VALUE);
			Rating = cursor.getDouble(RankQuery.GAME_RANK_BAYES_AVERAGE);
			Type = cursor.getString(RankQuery.GAME_RANK_TYPE);
		}
	}
}