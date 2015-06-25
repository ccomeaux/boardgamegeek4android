package com.boardgamegeek.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.graphics.Palette;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.events.UpdateCompleteEvent;
import com.boardgamegeek.events.UpdateEvent;
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

import java.text.DecimalFormat;
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

	private Handler mHandler = new Handler();
	private Runnable mTimeHintUpdaterRunnable = null;
	private Uri mGameUri;
	private String mGameName;
	private String mImageUrl;
	private boolean mSyncing;

	@InjectView(R.id.swipe_refresh) SwipeRefreshLayout mSwipeRefreshLayout;
	@InjectView(R.id.scroll_root) ObservableScrollView mScrollRoot;
	@InjectView(R.id.progress) View mProgressView;
	@InjectView(R.id.hero_container) View mHeroContainer;
	@InjectView(R.id.image) ImageView mImageView;
	@InjectView(R.id.header_container) View mHeaderContainer;
	@InjectView(R.id.game_info_name) TextView mNameView;
	@InjectView(R.id.game_info_rating) TextView mRatingView;
	@InjectView(R.id.game_info_description) TextView mDescriptionView;
	@InjectView(R.id.game_info_rank) TextView mRankView;
	@InjectView(R.id.game_info_year) TextView mYearPublishedView;

	@InjectView(R.id.primary_info_container) View mPrimaryInfo;
	@InjectView(R.id.number_of_players) TextView mNumberOfPlayersView;
	@InjectView(R.id.play_time) TextView mPlayTimeView;
	@InjectView(R.id.player_age) TextView mPlayerAgeView;

	@InjectView(R.id.game_info_designers) GameDetailRow mDesignersView;
	@InjectView(R.id.game_info_artists) GameDetailRow mArtistsView;
	@InjectView(R.id.game_info_publishers) GameDetailRow mPublishersView;
	@InjectView(R.id.game_info_categories) GameDetailRow mCategoriesView;
	@InjectView(R.id.game_info_mechanics) GameDetailRow mMechanicsView;
	@InjectView(R.id.game_info_expansions) GameDetailRow mExpansionsView;
	@InjectView(R.id.game_info_base_games) GameDetailRow mBaseGamesView;

	@InjectView(R.id.collection_card) View mCollectionCard;
	@InjectView(R.id.collection_container) ViewGroup mCollectionContainer;

	@InjectView(R.id.plays_card) View mPlaysCard;
	@InjectView(R.id.plays_root) View mPlaysRoot;
	@InjectView(R.id.plays_label) TextView mPlaysLabel;
	@InjectView(R.id.play_stats_root) View mPlayStatsRoot;
	@InjectView(R.id.colors_root) View mColorsRoot;
	@InjectView(R.id.game_colors_label) TextView mColorsLabel;

	@InjectView(R.id.game_comments_label) TextView mCommentsLabel;
	@InjectView(R.id.game_ratings_label) TextView mRatingsLabel;

	@InjectView(R.id.game_stats_label) TextView mStatsLabel;
	@InjectView(R.id.game_stats_content) View mStatsContent;
	@InjectView(R.id.game_stats_rank_root) LinearLayout mRankRoot;
	@InjectView(R.id.game_stats_rating_count) TextView mRatingsCount;
	@InjectView(R.id.game_stats_average_bar) StatBar mAverageStatBar;
	@InjectView(R.id.game_stats_bayes_bar) StatBar mBayesAverageBar;
	@InjectView(R.id.game_stats_median_bar) StatBar mMedianBar;
	@InjectView(R.id.game_stats_stddev_bar) StatBar mStdDevBar;
	@InjectView(R.id.game_stats_weight_count) TextView mWeightCount;
	@InjectView(R.id.game_stats_weight_bar) StatBar mWeightBar;
	@InjectView(R.id.game_stats_users_count) TextView mUserCount;
	@InjectView(R.id.game_stats_owning_bar) StatBar mNumOwningBar;
	@InjectView(R.id.game_stats_rating_bar) StatBar mNumRatingBar;
	@InjectView(R.id.game_stats_trading_bar) StatBar mNumTradingBar;
	@InjectView(R.id.game_stats_wanting_bar) StatBar mNumWantingBar;
	@InjectView(R.id.game_stats_wishing_bar) StatBar mNumWishingBar;
	@InjectView(R.id.game_stats_weighting_bar) StatBar mNumWeightingBar;

	@InjectView(R.id.game_links_label) TextView mLinksLabel;
	@InjectView(R.id.game_links_content) View mLinksContent;

	@InjectView(R.id.game_info_id) TextView mIdView;
	@InjectView(R.id.game_info_last_updated) TextView mUpdatedView;

	@InjectViews({
		R.id.number_of_players,
		R.id.play_time,
		R.id.player_age
	}) List<TextView> mColorizedTextViews;
	@InjectViews({
		R.id.game_info_designers,
		R.id.game_info_artists,
		R.id.game_info_publishers,
		R.id.game_info_categories,
		R.id.game_info_mechanics,
		R.id.game_info_expansions,
		R.id.game_info_base_games
	}) List<GameDetailRow> mColorizedRows;
	@InjectViews({
		R.id.card_header_details,
		R.id.card_header_collection,
		R.id.card_header_plays,
		R.id.card_header_user_feedback
	}) List<TextView> mColorizedHeaders;
	@InjectViews({
		R.id.icon_plays,
		R.id.icon_play_stats,
		R.id.icon_colors,
		R.id.icon_forums,
		R.id.icon_comments,
		R.id.icon_ratings,
		R.id.icon_stats,
		R.id.icon_links
	}) List<ImageView> mColorizedIcons;

	private boolean mIsDescriptionExpanded;
	private boolean mIsStatsExpanded;
	private boolean mIsLinksExpanded;
	private final NumberFormat mFormat = NumberFormat.getInstance();
	private boolean mMightNeedRefreshing;
	private Palette mPalette;

	static class GameInfo {
		String gameName;
		String subtype;
		String thumbnailUrl;
		String imageUrl;
		boolean customPlayerSort;
	}

	public interface Callbacks {
		public void onGameInfoChanged(GameInfo gameInfo);
	}

	private static Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public void onGameInfoChanged(GameInfo gameInfo) {
		}
	};

	private Callbacks mCallbacks = sDummyCallbacks;

	private ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener
		= new ViewTreeObserver.OnGlobalLayoutListener() {
		@Override
		public void onGlobalLayout() {
			ImageUtils.resizeImagePerAspectRatio(mImageView, mScrollRoot.getHeight() / 2, mHeroContainer);
		}
	};

	@Override
	@DebugLog
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mHandler = new Handler();

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mGameUri = intent.getData();

		if (mGameUri == null) {
			return;
		}

		if (savedInstanceState != null) {
			mIsDescriptionExpanded = savedInstanceState.getBoolean(KEY_DESCRIPTION_EXPANDED);
			mIsStatsExpanded = savedInstanceState.getBoolean(KEY_STATS_EXPANDED);
			mIsLinksExpanded = savedInstanceState.getBoolean(KEY_LINKS_EXPANDED);
		}

		HelpUtils.showHelpDialog(getActivity(), HelpUtils.HELP_GAME_KEY, HELP_VERSION, R.string.help_boardgame);
	}

	@Override
	@DebugLog
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_game, container, false);
		ButterKnife.inject(this, rootView);

		mSwipeRefreshLayout.setOnRefreshListener(this);
		mSwipeRefreshLayout.setColorSchemeResources(R.color.primary_dark, R.color.primary);

		colorize();
		openOrCloseDescription();
		openOrCloseStats();
		openOrCloseLinks();
		ScrimUtils.applyDefaultScrim(mHeaderContainer);
		mScrollRoot.addCallbacks(this);
		ViewTreeObserver vto = mScrollRoot.getViewTreeObserver();
		if (vto.isAlive()) {
			vto.addOnGlobalLayoutListener(mGlobalLayoutListener);
		}

		mMightNeedRefreshing = true;
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
		if (mTimeHintUpdaterRunnable != null) {
			mHandler.postDelayed(mTimeHintUpdaterRunnable, TIME_HINT_UPDATE_INTERVAL);
		}
	}

	@Override
	@DebugLog
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		if (!(activity instanceof Callbacks)) {
			throw new ClassCastException("Activity must implement fragment's callbacks.");
		}

		mCallbacks = (Callbacks) activity;
	}

	@Override
	@DebugLog
	public void onPause() {
		super.onPause();
		if (mTimeHintUpdaterRunnable != null) {
			mHandler.removeCallbacks(mTimeHintUpdaterRunnable);
		}
	}

	@Override
	@DebugLog
	public void onDetach() {
		super.onDetach();
		mCallbacks = sDummyCallbacks;
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
		if (mScrollRoot == null) {
			return;
		}

		ViewTreeObserver vto = mScrollRoot.getViewTreeObserver();
		if (vto.isAlive()) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
				//noinspection deprecation
				vto.removeGlobalOnLayoutListener(mGlobalLayoutListener);
			} else {
				vto.removeOnGlobalLayoutListener(mGlobalLayoutListener);
			}
		}
	}

	@Override
	@DebugLog
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(KEY_DESCRIPTION_EXPANDED, mIsDescriptionExpanded);
		outState.putBoolean(KEY_STATS_EXPANDED, mIsStatsExpanded);
		outState.putBoolean(KEY_LINKS_EXPANDED, mIsLinksExpanded);
	}

	@Override
	public void onRefresh() {
		triggerRefresh();
	}

	@Override
	@DebugLog
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = null;
		int gameId = Games.getGameId(mGameUri);
		switch (id) {
			case GameQuery._TOKEN:
				loader = new CursorLoader(getActivity(), mGameUri, GameQuery.PROJECTION, null, null, null);
				break;
			case DesignerQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Games.buildDesignersUri(gameId),
					DesignerQuery.PROJECTION, null, null, null);
				break;
			case ArtistQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Games.buildArtistsUri(gameId),
					ArtistQuery.PROJECTION, null, null, null);
				break;
			case PublisherQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Games.buildPublishersUri(gameId),
					PublisherQuery.PROJECTION, null, null, null);
				break;
			case CategoryQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Games.buildCategoriesUri(gameId),
					CategoryQuery.PROJECTION, null, null, null);
				break;
			case MechanicQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Games.buildMechanicsUri(gameId),
					MechanicQuery.PROJECTION, null, null, null);
				break;
			case ExpansionQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Games.buildExpansionsUri(gameId),
					ExpansionQuery.PROJECTION, GamesExpansions.INBOUND + "=?", new String[] { "0" }, null);
				break;
			case BaseGameQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Games.buildExpansionsUri(gameId),
					BaseGameQuery.PROJECTION, GamesExpansions.INBOUND + "=?", new String[] { "1" }, null);
				break;
			case RankQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Games.buildRanksUri(gameId),
					RankQuery.PROJECTION, null, null, null);
				break;
			case CollectionQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Collection.CONTENT_URI, CollectionQuery.PROJECTION,
					"collection." + Collection.GAME_ID + "=?", new String[] { String.valueOf(gameId) }, null);
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
				onListQueryComplete(cursor, mDesignersView, DesignerQuery.DESIGNER_NAME);
				break;
			case ArtistQuery._TOKEN:
				onListQueryComplete(cursor, mArtistsView, ArtistQuery.ARTIST_NAME);
				break;
			case PublisherQuery._TOKEN:
				onListQueryComplete(cursor, mPublishersView, PublisherQuery.PUBLISHER_NAME);
				break;
			case CategoryQuery._TOKEN:
				onListQueryComplete(cursor, mCategoriesView, CategoryQuery.CATEGORY_NAME);
				break;
			case MechanicQuery._TOKEN:
				onListQueryComplete(cursor, mMechanicsView, MechanicQuery.MECHANIC_NAME);
				break;
			case ExpansionQuery._TOKEN:
				onListQueryComplete(cursor, mExpansionsView, ExpansionQuery.EXPANSION_NAME);
				break;
			case BaseGameQuery._TOKEN:
				onListQueryComplete(cursor, mBaseGamesView, BaseGameQuery.EXPANSION_NAME);
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
				mPlaysCard.setVisibility(View.VISIBLE);
				mColorsRoot.setVisibility(View.VISIBLE);
				int count = cursor.getCount();
				mColorsLabel.setText(getResources().getQuantityString(R.plurals.colors_suffix, count, count));
				break;
			default:
				cursor.close();
				break;
		}
	}

	@Override
	@DebugLog
	public void onLoaderReset(Loader<Cursor> arg0) {
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	@DebugLog
	public void onScrollChanged(int deltaX, int deltaY) {
		if (VersionUtils.hasHoneycomb()) {
			int scrollY = mScrollRoot.getScrollY();
			mImageView.setTranslationY(scrollY * 0.5f);
			mHeaderContainer.setTranslationY(scrollY * 0.5f);
		}
	}

	@Override
	@DebugLog
	public void onPaletteGenerated(Palette palette) {
		mPalette = palette;
		colorize();
	}

	@DebugLog
	public void onEventMainThread(UpdateEvent event) {
		mSyncing = event.type == UpdateService.SYNC_TYPE_GAME;
		updateRefreshStatus();
	}

	@DebugLog
	public void onEventMainThread(UpdateCompleteEvent event) {
		mSyncing = false;
		updateRefreshStatus();
	}

	@DebugLog
	private void updateRefreshStatus() {
		if (mSwipeRefreshLayout != null) {
			mSwipeRefreshLayout.post(new Runnable() {
				@Override
				public void run() {
					mSwipeRefreshLayout.setRefreshing(mSyncing);
				}
			});
		}
	}

	@DebugLog
	private void colorize() {
		if (mPalette == null || mPrimaryInfo == null) {
			return;
		}
		Palette.Swatch swatch = PaletteUtils.getInverseSwatch(mPalette);
		mPrimaryInfo.setBackgroundColor(swatch.getRgb());
		ButterKnife.apply(mColorizedTextViews, PaletteUtils.colorTextViewOnBackgroundSetter, swatch);
		swatch = PaletteUtils.getIconSwatch(mPalette);
		ButterKnife.apply(mColorizedRows, GameDetailRow.colorIconSetter, swatch);
		ButterKnife.apply(mColorizedIcons, PaletteUtils.colorIconSetter, swatch);
		swatch = PaletteUtils.getHeaderSwatch(mPalette);
		ButterKnife.apply(mColorizedHeaders, PaletteUtils.colorTextViewSetter, swatch);
	}

	@DebugLog
	private void onGameQueryComplete(Cursor cursor) {
		if (cursor == null || !cursor.moveToFirst()) {
			if (mMightNeedRefreshing) {
				triggerRefresh();
			}
			return;
		}

		Game game = new Game(cursor);

		notifyChange(game);
		mGameName = game.Name;
		mImageUrl = game.ImageUrl;

		ImageUtils.safelyLoadImage(mImageView, game.ImageUrl, this);
		mNameView.setText(game.Name);
		mRankView.setText(game.getRankDescription());
		mYearPublishedView.setText(game.getYearPublished());
		mRatingView.setText(game.getRatingDescription());
		ColorUtils.setViewBackground(mRatingView, ColorUtils.getRatingColor(game.Rating));
		mIdView.setText(String.valueOf(game.Id));
		mUpdatedView.setTag(game.Updated);
		UIUtils.setTextMaybeHtml(mDescriptionView, game.Description);
		mNumberOfPlayersView.setText(game.getPlayerRangeDescription());
		mPlayTimeView.setText(game.getPlayingTimeDescription());
		mPlayerAgeView.setText(game.getAgeDescription());
		mCommentsLabel.setText(getResources().getQuantityString(R.plurals.comments_suffix, game.UsersCommented, game.UsersCommented));
		mRatingsLabel.setText(getResources().getQuantityString(R.plurals.ratings_suffix, game.UsersRated, game.UsersRated));

		mRatingsCount.setText(String.format(getResources().getString(R.string.rating_count), mFormat.format(game.UsersRated)));
		mAverageStatBar.setBar(R.string.average_meter_text, game.Rating);
		mBayesAverageBar.setBar(R.string.bayes_meter_text, game.BayesAverage);
		if (game.Median <= 0) {
			mMedianBar.setVisibility(View.GONE);
		} else {
			mMedianBar.setVisibility(View.VISIBLE);
			mMedianBar.setBar(R.string.median_meter_text, game.Median);
		}
		mStdDevBar.setBar(R.string.stdDev_meter_text, game.StandardDeviation, 5.0);

		mWeightCount.setText(String.format(getResources().getString(R.string.weight_count), mFormat.format(game.NumberWeights)));
		mWeightBar.setBar(game.getWeightDescriptionResId(), game.AverageWeight, 5.0, 1.0);

		mUserCount.setText(String.format(getResources().getString(R.string.user_total), mFormat.format(game.getMaxUsers())));
		mNumOwningBar.setBar(R.string.owning_meter_text, game.NumberOwned, game.getMaxUsers());
		mNumRatingBar.setBar(R.string.rating_meter_text, game.UsersRated, game.getMaxUsers());
		mNumTradingBar.setBar(R.string.trading_meter_text, game.NumberTrading, game.getMaxUsers());
		mNumWantingBar.setBar(R.string.wanting_meter_text, game.NumberWanting, game.getMaxUsers());
		mNumWishingBar.setBar(R.string.wishing_meter_text, game.NumberWishing, game.getMaxUsers());
		mNumWeightingBar.setBar(R.string.weighting_meter_text, game.NumberWeights, game.getMaxUsers());

		if (shouldShowPlays()) {
			mPlaysCard.setVisibility(View.VISIBLE);
			mPlayStatsRoot.setVisibility(View.VISIBLE);
		}

		updateTimeBasedUi();
		if (mTimeHintUpdaterRunnable != null) {
			mHandler.removeCallbacks(mTimeHintUpdaterRunnable);
		}
		mTimeHintUpdaterRunnable = new Runnable() {
			@Override
			public void run() {
				updateTimeBasedUi();
				mHandler.postDelayed(mTimeHintUpdaterRunnable, TIME_HINT_UPDATE_INTERVAL);
			}
		};
		mHandler.postDelayed(mTimeHintUpdaterRunnable, TIME_HINT_UPDATE_INTERVAL);

		AnimationUtils.fadeOut(getActivity(), mProgressView, true);
		AnimationUtils.fadeIn(getActivity(), mSwipeRefreshLayout, true);

		if (mMightNeedRefreshing
			&& (game.PollsCount == 0 || DateTimeUtils.howManyDaysOld(game.Updated) > AGE_IN_DAYS_TO_REFRESH)) {
			triggerRefresh();
		}
		mMightNeedRefreshing = false;
	}

	@DebugLog
	private void updateTimeBasedUi() {
		if (!isAdded()) {
			return;
		}
		if (mUpdatedView != null) {
			long updatedTime = (long) mUpdatedView.getTag();
			mUpdatedView.setText(PresentationUtils.describePastTimeSpan(updatedTime, getResources().getString(R.string.needs_updating)));
		}
	}

	@DebugLog
	private boolean shouldShowPlays() {
		return Authenticator.isSignedIn(getActivity()) && PreferencesUtils.getSyncPlays(getActivity());
	}

	@DebugLog
	private void notifyChange(Game game) {
		GameInfo gameInfo = new GameInfo();
		gameInfo.gameName = game.Name;
		gameInfo.subtype = game.Subtype;
		gameInfo.thumbnailUrl = game.ThumbnailUrl;
		gameInfo.imageUrl = game.ImageUrl;
		gameInfo.customPlayerSort = game.CustomPlayerSort;
		mCallbacks.onGameInfoChanged(gameInfo);
	}

	@DebugLog
	private void onListQueryComplete(Cursor cursor, GameDetailRow view, int nameColumnIndex) {
		if (cursor == null || !cursor.moveToFirst()) {
			view.setVisibility(View.GONE);
			view.clear();
		} else {
			view.setVisibility(View.VISIBLE);
			view.bind(cursor, nameColumnIndex, Games.getGameId(mGameUri), mGameName);
		}
	}

	@DebugLog
	private void onRankQueryComplete(Cursor cursor) {
		mRankRoot.removeAllViews();
		if (cursor == null || cursor.getCount() == 0) {
			mRankRoot.setVisibility(View.GONE);
		} else {
			mRankRoot.setVisibility(View.VISIBLE);

			while (cursor.moveToNext()) {
				Rank rank = new Rank(cursor);
				addRankRow(rank.Name, rank.Rank, "subtype".equals(rank.Type), rank.Rating);
			}
		}
	}

	@DebugLog
	private void addRankRow(String label, int rank, boolean bold, double rating) {
		LinearLayout layout = (LinearLayout) getLayoutInflater(null)
			.inflate(R.layout.widget_rank_row, mRankRoot, false);

		TextView tv = (TextView) layout.findViewById(R.id.rank_row_label);
		setText(tv, label, bold);

		tv = (TextView) layout.findViewById(R.id.rank_row_rank);
		String rankText = (rank == 0 || rank == Integer.MAX_VALUE) ? getResources().getString(
			R.string.text_not_available) : String.valueOf(rank);
		setText(tv, rankText, bold);

		StatBar sb = new StatBar(getActivity());
		sb.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
			LinearLayout.LayoutParams.MATCH_PARENT));
		sb.setBar(R.string.average_meter_text, rating);

		mRankRoot.addView(layout);
		mRankRoot.addView(sb);
	}

	@DebugLog
	private void onCollectionQueryComplete(Cursor cursor) {
		if (cursor.moveToFirst()) {
			mCollectionCard.setVisibility(View.VISIBLE);
			mCollectionContainer.removeViews(1, mCollectionContainer.getChildCount() - 1);
			do {
				GameCollectionRow row = new GameCollectionRow(getActivity());
				row.bind(Games.getGameId(mGameUri), mGameName, cursor.getInt(CollectionQuery.COLLECTION_ID));
				row.setThumbnail(cursor.getString(CollectionQuery.COLLECTION_THUMBNAIL));
				row.setName(cursor.getString(CollectionQuery.COLLECTION_NAME));
				row.setYear(cursor.getInt(CollectionQuery.COLLECTION_YEAR));
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
				row.setStatus(status, cursor.getInt(CollectionQuery.NUM_PLAYS));
				mCollectionContainer.addView(row);
			} while (cursor.moveToNext());
		}
	}

	@DebugLog
	private void onPlaysQueryComplete(Cursor cursor) {
		if (cursor.moveToFirst()) {
			mPlaysCard.setVisibility(View.VISIBLE);
			mPlaysRoot.setVisibility(View.VISIBLE);
			int sum = cursor.getInt(PlaysQuery.SUM_QUANTITY);
			if (sum > 0) {
				String date = CursorUtils.getFormattedDate(cursor, getActivity(), PlaysQuery.MAX_DATE);
				mPlaysLabel.setText(getResources().getQuantityString(R.plurals.plays_summary, sum, sum, date));
			} else {
				mPlaysLabel.setText(getResources().getString(R.string.no_plays));
			}
		}
	}

	@DebugLog
	private void setText(TextView tv, String text, boolean bold) {
		if (bold) {
			SpannableString ss = new SpannableString(text);
			ss.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			tv.setText(ss);
		} else {
			tv.setText(text);
		}
	}

	@OnClick(R.id.image)
	@DebugLog
	public void onThumbnailClick(View v) {
		if (!TextUtils.isEmpty(mImageUrl)) {
			final Intent intent = new Intent(getActivity(), ImageActivity.class);
			intent.putExtra(ActivityUtils.KEY_IMAGE_URL, mImageUrl);
			startActivity(intent);
		}
	}

	@OnClick(R.id.game_info_description)
	@DebugLog
	public void onDescriptionClick(View v) {
		mIsDescriptionExpanded = !mIsDescriptionExpanded;
		openOrCloseDescription();
	}

	@OnClick(R.id.plays_root)
	@DebugLog
	public void onPlaysClick(View v) {
		Intent intent = new Intent(getActivity(), GamePlaysActivity.class);
		intent.setData(mGameUri);
		intent.putExtra(ActivityUtils.KEY_GAME_NAME, mGameName);
		startActivity(intent);
	}

	@OnClick(R.id.play_stats_root)
	@DebugLog
	public void onPlayStatsClick(View v) {
		Intent intent = new Intent(getActivity(), GamePlayStatsActivity.class);
		intent.setData(mGameUri);
		intent.putExtra(ActivityUtils.KEY_GAME_NAME, mGameName);
		startActivity(intent);
	}

	@OnClick(R.id.colors_root)
	@DebugLog
	public void onColorsClick(View v) {
		Intent intent = new Intent(getActivity(), ColorsActivity.class);
		intent.setData(mGameUri);
		intent.putExtra(ActivityUtils.KEY_GAME_NAME, mGameName);
		startActivity(intent);
	}

	@OnClick(R.id.forums_root)
	@DebugLog
	public void onForumsClick(View v) {
		Intent intent = new Intent(getActivity(), GameForumsActivity.class);
		intent.setData(mGameUri);
		intent.putExtra(ActivityUtils.KEY_GAME_NAME, mGameName);
		startActivity(intent);
	}

	@OnClick(R.id.comments_root)
	@DebugLog
	public void onCommentsClick(View v) {
		Intent intent = new Intent(getActivity(), CommentsActivity.class);
		intent.setData(mGameUri);
		intent.putExtra(ActivityUtils.KEY_GAME_NAME, mGameName);
		startActivity(intent);
	}

	@OnClick(R.id.ratings_root)
	public void onRatingsClick(View v) {
		Intent intent = new Intent(getActivity(), CommentsActivity.class);
		intent.setData(mGameUri);
		intent.putExtra(ActivityUtils.KEY_GAME_NAME, mGameName);
		intent.putExtra(ActivityUtils.KEY_SORT, CommentsActivity.SORT_RATING);
		startActivity(intent);
	}

	@OnClick(R.id.game_info_stats_root)
	@DebugLog
	public void onStatsClick(View v) {
		mIsStatsExpanded = !mIsStatsExpanded;
		openOrCloseStats();
	}

	@OnClick(R.id.game_info_links_root)
	@DebugLog
	public void onLinksClick(View v) {
		mIsLinksExpanded = !mIsLinksExpanded;
		openOrCloseLinks();
	}

	@DebugLog
	private void openOrCloseDescription() {
		mDescriptionView.setMaxLines(mIsDescriptionExpanded ? Integer.MAX_VALUE : 3);
		mDescriptionView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0,
			mIsDescriptionExpanded ? R.drawable.expander_close : R.drawable.expander_open);
	}

	@DebugLog
	private void openOrCloseStats() {
		mStatsContent.setVisibility(mIsStatsExpanded ? View.VISIBLE : View.GONE);
		mStatsLabel.setCompoundDrawablesWithIntrinsicBounds(0, 0, mIsStatsExpanded ? R.drawable.expander_close : R.drawable.expander_open, 0);
	}

	@DebugLog
	private void openOrCloseLinks() {
		mLinksContent.setVisibility(mIsLinksExpanded ? View.VISIBLE : View.GONE);
		mLinksLabel.setCompoundDrawablesWithIntrinsicBounds(0, 0, mIsLinksExpanded ? R.drawable.expander_close : R.drawable.expander_open, 0);
	}

	@DebugLog
	@OnClick({ R.id.link_bgg, R.id.link_bg_prices, R.id.link_amazon, R.id.link_ebay })
	void onLinkClick(View v) {
		switch (v.getId()) {
			case R.id.link_bgg:
				ActivityUtils.linkBgg(getActivity(), Games.getGameId(mGameUri));
				break;
			case R.id.link_bg_prices:
				ActivityUtils.linkBgPrices(getActivity(), mGameName);
				break;
			case R.id.link_amazon:
				ActivityUtils.linkAmazon(getActivity(), mGameName);
				break;
			case R.id.link_ebay:
				ActivityUtils.linkEbay(getActivity(), mGameName);
				break;
		}
	}

	@OnClick({ R.id.number_of_players, R.id.player_age })
	@DebugLog
	public void onPollClick(View v) {
		Bundle arguments = new Bundle(2);
		arguments.putInt(ActivityUtils.KEY_GAME_ID, Games.getGameId(mGameUri));
		arguments.putString(ActivityUtils.KEY_TYPE, (String) v.getTag());
		DialogUtils.launchDialog(this, new PollFragment(), "poll-dialog", arguments);
	}

	@DebugLog
	private void triggerRefresh() {
		mMightNeedRefreshing = false;
		UpdateService.start(getActivity(), UpdateService.SYNC_TYPE_GAME, Games.getGameId(mGameUri));
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
		int DESIGNER_NAME = 1;
	}

	private interface ArtistQuery {
		int _TOKEN = 0x13;
		String[] PROJECTION = { Artists.ARTIST_ID, Artists.ARTIST_NAME, Artists._ID };
		int ARTIST_NAME = 1;
	}

	private interface PublisherQuery {
		int _TOKEN = 0x14;
		String[] PROJECTION = { Publishers.PUBLISHER_ID, Publishers.PUBLISHER_NAME, Publishers._ID };
		int PUBLISHER_NAME = 1;
	}

	private interface CategoryQuery {
		int _TOKEN = 0x15;
		String[] PROJECTION = { Categories.CATEGORY_ID, Categories.CATEGORY_NAME, Categories._ID };
		int CATEGORY_NAME = 1;
	}

	private interface MechanicQuery {
		int _TOKEN = 0x16;
		String[] PROJECTION = { Mechanics.MECHANIC_ID, Mechanics.MECHANIC_NAME, Mechanics._ID };
		int MECHANIC_NAME = 1;
	}

	private interface ExpansionQuery {
		int _TOKEN = 0x17;
		String[] PROJECTION = { GamesExpansions.EXPANSION_ID, GamesExpansions.EXPANSION_NAME, GamesExpansions._ID };
		int EXPANSION_NAME = 1;
	}

	private interface BaseGameQuery {
		int _TOKEN = 0x18;
		String[] PROJECTION = { GamesExpansions.EXPANSION_ID, GamesExpansions.EXPANSION_NAME, GamesExpansions._ID };
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
			Collection.COLLECTION_YEAR_PUBLISHED, Collection.COLLECTION_IMAGE_URL, Collection.STATUS_OWN,
			Collection.STATUS_PREVIOUSLY_OWNED, Collection.STATUS_FOR_TRADE, Collection.STATUS_WANT,
			Collection.STATUS_WANT_TO_BUY, Collection.STATUS_WISHLIST, Collection.STATUS_WANT_TO_PLAY,
			Collection.STATUS_PREORDERED, Collection.STATUS_WISHLIST_PRIORITY, Collection.NUM_PLAYS };
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
	}

	private interface PlaysQuery {
		String[] PROJECTION = { Plays._ID, Plays.MAX_DATE, Plays.SUM_QUANTITY, Plays.MAX_DATE };
		int _TOKEN = 0x21;
		int _ID = 0;
		int MAX_DATE = 1;
		int SUM_QUANTITY = 2;
	}

	private interface ColorQuery {
		int _TOKEN = 0x22;
	}

	private class Game {
		String Name;
		String ThumbnailUrl;
		String ImageUrl;
		int Id;
		float Rating;
		int YearPublished;
		int MinPlayers;
		int MaxPlayers;
		int PlayingTime;
		int MinimumAge;
		String Description;
		int UsersRated;
		int UsersCommented;
		long Updated;
		int Rank;
		double BayesAverage;
		double Median;
		double StandardDeviation;
		double AverageWeight;
		int NumberWeights;
		int NumberOwned;
		int NumberTrading;
		int NumberWanting;
		int NumberWishing;
		int PollsCount;
		String Subtype;
		boolean CustomPlayerSort;

		public Game(Cursor cursor) {
			Name = cursor.getString(GameQuery.GAME_NAME);
			ThumbnailUrl = cursor.getString(GameQuery.THUMBNAIL_URL);
			ImageUrl = cursor.getString(GameQuery.IMAGE_URL);
			Id = cursor.getInt(GameQuery.GAME_ID);
			Rating = (float) cursor.getDouble(GameQuery.STATS_AVERAGE);
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
			if (Rank == 0 || Rank == Integer.MAX_VALUE) {
				return "";
			} else {
				return "#" + Rank;
			}
		}

		@DebugLog
		public String getRatingDescription() {
			return new DecimalFormat("#0.00").format(Rating);
		}

		@DebugLog
		public String getYearPublished() {
			return PresentationUtils.describeYear(getActivity(), YearPublished);
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
	}

	private class Rank {
		String Name;
		int Rank;
		double Rating;
		String Type;

		Rank(Cursor cursor) {
			Name = cursor.getString(RankQuery.GAME_RANK_FRIENDLY_NAME);
			Rank = cursor.getInt(RankQuery.GAME_RANK_VALUE);
			Rating = cursor.getDouble(RankQuery.GAME_RANK_BAYES_AVERAGE);
			Type = cursor.getString(RankQuery.GAME_RANK_TYPE);
		}
	}
}