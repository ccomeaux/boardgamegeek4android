package com.boardgamegeek.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.graphics.Palette;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggContract.Categories;
import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.GameRanks;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.GamesExpansions;
import com.boardgamegeek.provider.BggContract.Mechanics;
import com.boardgamegeek.provider.BggContract.Publishers;
import com.boardgamegeek.service.UpdateService;
import com.boardgamegeek.ui.widget.GameDetailRow;
import com.boardgamegeek.ui.widget.StatBar;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.AnimationUtils;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.DetachableResultReceiver;
import com.boardgamegeek.util.ForumsUtils;
import com.boardgamegeek.util.ScrimUtil;
import com.boardgamegeek.util.UIUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.InjectViews;
import butterknife.OnClick;
import timber.log.Timber;

public class GameInfoFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
	ActivityUtils.ImageCallback {
	private static final int HELP_VERSION = 1;
	private static final int AGE_IN_DAYS_TO_REFRESH = 7;
	private static final String KEY_DESCRIPTION_EXPANDED = "DESCRIPTION_EXPANDED";
	private static final String KEY_STATS_EXPANDED = "STATS_EXPANDED";

	private Uri mGameUri;
	private String mGameName;
	private String mImageUrl;

	@InjectView(R.id.game_info_scroll_root) View mScrollRoot;
	@InjectView(R.id.game_info_progress) View mProgressView;
	@InjectView(R.id.hero_container) View mHeroContainer;
	@InjectView(R.id.game_info_image) ImageView mImageView;
	@InjectView(R.id.game_info_name) TextView mNameView;
	@InjectView(R.id.game_info_rating) TextView mRatingView;
	@InjectView(R.id.game_info_rating_count) TextView mNumberRatingView;
	@InjectView(R.id.game_info_id) TextView mIdView;
	@InjectView(R.id.game_info_last_updated) TextView mUpdatedView;
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
		R.id.icon_forums,
		R.id.icon_stats
	}) List<ImageView> mColorizedIcons;

	boolean mIsDescriptionExpanded;
	boolean mIsStatsExpanded;
	private NumberFormat mFormat = NumberFormat.getInstance();
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

		public DetachableResultReceiver getReceiver();
	}

	private static Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public void onGameInfoChanged(GameInfo gameInfo) {
		}

		@Override
		public DetachableResultReceiver getReceiver() {
			return null;
		}
	};

	private Callbacks mCallbacks = sDummyCallbacks;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		mGameUri = intent.getData();

		if (mGameUri == null) {
			return;
		}

		if (savedInstanceState != null) {
			mIsDescriptionExpanded = savedInstanceState.getBoolean(KEY_DESCRIPTION_EXPANDED);
			mIsStatsExpanded = savedInstanceState.getBoolean(KEY_STATS_EXPANDED);
		}

		UIUtils.showHelpDialog(getActivity(), UIUtils.HELP_GAME_KEY, HELP_VERSION, R.string.help_boardgame);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_game_info, container, false);
		ButterKnife.inject(this, rootView);
		colorize(mPalette);
		openOrCloseDescription();
		openOrCloseStats();

		mHeroContainer.setBackground(ScrimUtil.makeDefaultScrimDrawable(getActivity()));

		mMightNeedRefreshing = true;
		LoaderManager lm = getLoaderManager();
		lm.restartLoader(GameQuery._TOKEN, null, this);
		lm.restartLoader(RankQuery._TOKEN, null, this);

		return rootView;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		if (!(activity instanceof Callbacks)) {
			throw new ClassCastException("Activity must implement fragment's callbacks.");
		}

		mCallbacks = (Callbacks) activity;
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mCallbacks = sDummyCallbacks;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		ButterKnife.reset(this);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(KEY_DESCRIPTION_EXPANDED, mIsDescriptionExpanded);
		outState.putBoolean(KEY_STATS_EXPANDED, mIsStatsExpanded);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_refresh) {
			triggerRefresh();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = null;
		switch (id) {
			case GameQuery._TOKEN:
				loader = new CursorLoader(getActivity(), mGameUri, GameQuery.PROJECTION, null, null, null);
				break;
			case DesignerQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Games.buildDesignersUri(Games.getGameId(mGameUri)),
					DesignerQuery.PROJECTION, null, null, null);
				break;
			case ArtistQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Games.buildArtistsUri(Games.getGameId(mGameUri)),
					ArtistQuery.PROJECTION, null, null, null);
				break;
			case PublisherQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Games.buildPublishersUri(Games.getGameId(mGameUri)),
					PublisherQuery.PROJECTION, null, null, null);
				break;
			case CategoryQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Games.buildCategoriesUri(Games.getGameId(mGameUri)),
					CategoryQuery.PROJECTION, null, null, null);
				break;
			case MechanicQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Games.buildMechanicsUri(Games.getGameId(mGameUri)),
					MechanicQuery.PROJECTION, null, null, null);
				break;
			case ExpansionQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Games.buildExpansionsUri(Games.getGameId(mGameUri)),
					ExpansionQuery.PROJECTION, GamesExpansions.INBOUND + "=?", new String[] { "0" }, null);
				break;
			case BaseGameQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Games.buildExpansionsUri(Games.getGameId(mGameUri)),
					BaseGameQuery.PROJECTION, GamesExpansions.INBOUND + "=?", new String[] { "1" }, null);
				break;
			case RankQuery._TOKEN:
				loader = new CursorLoader(getActivity(), Games.buildRanksUri(Games.getGameId(mGameUri)),
					RankQuery.PROJECTION, null, null, null);
				break;
			default:
				Timber.w("Invalid query token=" + id);
				break;
		}
		return loader;
	}

	@Override
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
			default:
				cursor.close();
				break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
	}

	@Override
	public void onPaletteGenerated(Palette palette) {
		mPalette = palette;
		colorize(palette);
	}

	private void colorize(Palette palette) {
		if (palette == null || mPrimaryInfo == null) {
			return;
		}
		Palette.Swatch swatch = ColorUtils.getInverseSwatch(palette);
		mPrimaryInfo.setBackgroundColor(swatch.getRgb());
		ButterKnife.apply(mColorizedTextViews, ColorUtils.colorTextViewSetter, swatch);
		swatch = ColorUtils.getIconSwatch(palette);
		ButterKnife.apply(mColorizedRows, GameDetailRow.colorIconSetter, swatch);
		ButterKnife.apply(mColorizedIcons, ColorUtils.colorIconSetter, swatch);
	}

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

		ActivityUtils.safelyLoadImage(mImageView, game.ImageUrl, this);
		mNameView.setText(game.Name);
		mRankView.setText(game.getRankDescription());
		mYearPublishedView.setText(game.getYearPublished());
		mRatingView.setText(game.getRatingDescription());
		ColorUtils.setTextViewBackground(mRatingView, ColorUtils.getRatingColor(game.Rating));
		mNumberRatingView.setText(mFormat.format(game.UsersRated));
		mIdView.setText(String.valueOf(game.Id));
		mUpdatedView.setText(game.getUpdatedDescription());
		UIUtils.setTextMaybeHtml(mDescriptionView, game.Description);
		mNumberOfPlayersView.setText(game.getPlayerRangeDescription());
		mPlayTimeView.setText(game.getPlayingTimeDescription());
		mPlayerAgeView.setText(game.getAgeDescription());

		mRatingsCount.setText(String.format(getResources().getString(R.string.rating_count),
			mFormat.format(game.UsersRated)));
		mAverageStatBar.setBar(R.string.average_meter_text, game.Rating);
		mBayesAverageBar.setBar(R.string.bayes_meter_text, game.BayesAverage);
		if (game.Median <= 0) {
			mMedianBar.setVisibility(View.GONE);
		} else {
			mMedianBar.setVisibility(View.VISIBLE);
			mMedianBar.setBar(R.string.median_meter_text, game.Median);
		}
		mStdDevBar.setBar(R.string.stdDev_meter_text, game.StandardDeviation, 5.0);

		mWeightCount.setText(String.format(getResources().getString(R.string.weight_count),
			mFormat.format(game.NumberWeights)));
		mWeightBar.setBar(game.getWeightDescriptionResId(), game.AverageWeight, 5.0, 1.0);

		mUserCount.setText(String.format(getResources().getString(R.string.user_total),
			mFormat.format(game.getMaxUsers())));
		mNumOwningBar.setBar(R.string.owning_meter_text, game.NumberOwned, game.getMaxUsers());
		mNumRatingBar.setBar(R.string.rating_meter_text, game.UsersRated, game.getMaxUsers());
		mNumTradingBar.setBar(R.string.trading_meter_text, game.NumberTrading, game.getMaxUsers());
		mNumWantingBar.setBar(R.string.wanting_meter_text, game.NumberWanting, game.getMaxUsers());
		mNumWishingBar.setBar(R.string.wishing_meter_text, game.NumberWishing, game.getMaxUsers());
		mNumWeightingBar.setBar(R.string.weighting_meter_text, game.NumberWeights, game.getMaxUsers());

		AnimationUtils.fadeOut(getActivity(), mProgressView, true);
		AnimationUtils.fadeIn(getActivity(), mScrollRoot, true);

		if (mMightNeedRefreshing
			&& (game.PollsCount == 0 || DateTimeUtils.howManyDaysOld(game.Updated) > AGE_IN_DAYS_TO_REFRESH)) {
			triggerRefresh();
		}
		mMightNeedRefreshing = false;
	}

	private void notifyChange(Game game) {
		GameInfo gameInfo = new GameInfo();
		gameInfo.gameName = game.Name;
		gameInfo.subtype = game.Subtype;
		gameInfo.thumbnailUrl = game.ThumbnailUrl;
		gameInfo.imageUrl = game.ImageUrl;
		gameInfo.customPlayerSort = game.CustomPlayerSort;
		mCallbacks.onGameInfoChanged(gameInfo);
	}

	private void onListQueryComplete(Cursor cursor, GameDetailRow view, int nameColumnIndex) {
		if (cursor == null || !cursor.moveToFirst()) {
			view.setVisibility(View.GONE);
			view.clear();
		} else {
			view.setVisibility(View.VISIBLE);
			view.bind(cursor, nameColumnIndex, Games.getGameId(mGameUri), mGameName);
		}
	}

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

	private void setText(TextView tv, String text, boolean bold) {
		if (bold) {
			SpannableString ss = new SpannableString(text);
			ss.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			tv.setText(ss);
		} else {
			tv.setText(text);
		}
	}

	@OnClick(R.id.game_info_image)
	public void onThumbnailClick(View v) {
		if (!TextUtils.isEmpty(mImageUrl)) {
			final Intent intent = new Intent(getActivity(), ImageActivity.class);
			intent.putExtra(ImageActivity.KEY_IMAGE_URL, mImageUrl);
			startActivity(intent);
		}
	}

	@OnClick(R.id.game_info_description)
	public void onDescriptionClick(View v) {
		mIsDescriptionExpanded = !mIsDescriptionExpanded;
		openOrCloseDescription();
	}

	@OnClick(R.id.forums_root)
	public void onForumsClick(View v) {
		Intent intent = new Intent(getActivity(), GameForumsActivity.class);
		intent.setData(mGameUri);
		intent.putExtra(ForumsUtils.KEY_GAME_NAME, mGameName);
		startActivity(intent);
	}

	@OnClick(R.id.game_info_stats_root)
	public void onStatsClick(View v) {
		mIsStatsExpanded = !mIsStatsExpanded;
		openOrCloseStats();
	}

	private void openOrCloseDescription() {
		mDescriptionView.setMaxLines(mIsDescriptionExpanded ? Integer.MAX_VALUE : 3);
		mDescriptionView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0,
			mIsDescriptionExpanded ? R.drawable.expander_close : R.drawable.expander_open);
	}

	private void openOrCloseStats() {
		mStatsContent.setVisibility(mIsStatsExpanded ? View.VISIBLE : View.GONE);
		mStatsLabel.setCompoundDrawablesWithIntrinsicBounds(0, 0, mIsStatsExpanded ? R.drawable.expander_close
			: R.drawable.expander_open, 0);
	}

	@OnClick({ R.id.number_of_players, R.id.player_age })
	public void onPollClick(View v) {
		Bundle arguments = new Bundle(2);
		arguments.putInt(PollFragment.KEY_GAME_ID, Games.getGameId(mGameUri));
		arguments.putString(PollFragment.KEY_TYPE, (String) v.getTag());
		ActivityUtils.launchDialog(this, new PollFragment(), "poll-dialog", arguments);
	}

	private void triggerRefresh() {
		mMightNeedRefreshing = false;
		UpdateService.start(getActivity(), UpdateService.SYNC_TYPE_GAME, Games.getGameId(mGameUri), null);
	}

	private interface GameQuery {
		int _TOKEN = 0x11;

		String[] PROJECTION = { Games.GAME_ID, Games.STATS_AVERAGE, Games.YEAR_PUBLISHED, Games.MIN_PLAYERS,
			Games.MAX_PLAYERS, Games.PLAYING_TIME, Games.MINIMUM_AGE, Games.DESCRIPTION, Games.STATS_USERS_RATED,
			Games.UPDATED, Games.GAME_RANK, Games.GAME_NAME, Games.THUMBNAIL_URL, Games.STATS_BAYES_AVERAGE,
			Games.STATS_MEDIAN, Games.STATS_STANDARD_DEVIATION, Games.STATS_NUMBER_WEIGHTS, Games.STATS_AVERAGE_WEIGHT,
			Games.STATS_NUMBER_OWNED, Games.STATS_NUMBER_TRADING, Games.STATS_NUMBER_WANTING,
			Games.STATS_NUMBER_WISHING, Games.POLLS_COUNT, Games.IMAGE_URL, Games.SUBTYPE, Games.CUSTOM_PLAYER_SORT };

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

		public String getAgeDescription() {
			if (MinimumAge > 0) {
				return MinimumAge + " " + getResources().getString(R.string.age_suffix);
			}
			return getResources().getString(R.string.text_unknown);
		}

		public int getMaxUsers() {
			int max = Math.max(UsersRated, NumberOwned);
			max = Math.max(max, NumberTrading);
			max = Math.max(max, NumberWanting);
			max = Math.max(max, NumberWeights);
			max = Math.max(max, NumberWishing);
			return max;
		}

		private String getPlayerRangeDescription() {
			if (MinPlayers == 0 && MaxPlayers == 0) {
				return getResources().getString(R.string.text_unknown);
			} else if (MinPlayers >= MaxPlayers) {
				return String.valueOf(MinPlayers);
			} else {
				return String.valueOf(MinPlayers) + " - " + String.valueOf(MaxPlayers);
			}
		}

		private String getPlayingTimeDescription() {
			if (PlayingTime > 0) {
				return PlayingTime + " " + getResources().getString(R.string.minutes_abbr);
			}
			return getResources().getString(R.string.text_unknown);
		}

		private String getRankDescription() {
			if (Rank == 0 || Rank == Integer.MAX_VALUE) {
				return "";
			} else {
				return "#" + Rank;
			}
		}

		public String getRatingDescription() {
			return new DecimalFormat("#0.00").format(Rating);
		}

		public String getYearPublished() {
			if (YearPublished > 0) {
				return getString(R.string.year_positive, YearPublished);
			} else if (YearPublished == 0) {
				return getString(R.string.year_zero, YearPublished);
			} else {
				return getString(R.string.year_negative, -YearPublished);
			}
		}

		public CharSequence getUpdatedDescription() {
			if (Updated == 0) {
				return getResources().getString(R.string.needs_updating);
			}
			return DateUtils.getRelativeTimeSpanString(Updated);
		}

		public int getWeightDescriptionResId() {
			int resId = R.string.weight_1_text;
			if (AverageWeight >= 4.5) {
				resId = R.string.weight_5_text;
			} else if (AverageWeight >= 3.5) {
				resId = R.string.weight_4_text;
			} else if (AverageWeight >= 2.5) {
				resId = R.string.weight_3_text;
			} else if (AverageWeight >= 1.5) {
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