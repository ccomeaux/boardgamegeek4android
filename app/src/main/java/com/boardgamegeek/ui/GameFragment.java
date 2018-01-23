package com.boardgamegeek.ui;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.app.AlertDialog.Builder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.events.CollectionItemAddedEvent;
import com.boardgamegeek.events.GameInfoChangedEvent;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Forum;
import com.boardgamegeek.model.ForumListResponse;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.tasks.FavoriteGameTask;
import com.boardgamegeek.tasks.sync.SyncGameTask;
import com.boardgamegeek.ui.GameActivity.ColorEvent;
import com.boardgamegeek.ui.dialog.GameUsersDialogFragment;
import com.boardgamegeek.ui.dialog.RanksFragment;
import com.boardgamegeek.ui.model.Game;
import com.boardgamegeek.ui.model.GameArtist;
import com.boardgamegeek.ui.model.GameBaseGame;
import com.boardgamegeek.ui.model.GameCategory;
import com.boardgamegeek.ui.model.GameDesigner;
import com.boardgamegeek.ui.model.GameExpansion;
import com.boardgamegeek.ui.model.GameList;
import com.boardgamegeek.ui.model.GameMechanic;
import com.boardgamegeek.ui.model.GamePublisher;
import com.boardgamegeek.ui.model.GameRank;
import com.boardgamegeek.ui.model.GameSuggestedAge;
import com.boardgamegeek.ui.model.GameSuggestedLanguage;
import com.boardgamegeek.ui.model.GameSuggestedPlayerCount;
import com.boardgamegeek.ui.widget.ContentLoadingProgressBar;
import com.boardgamegeek.ui.widget.GameDetailRow;
import com.boardgamegeek.ui.widget.SafeViewTarget;
import com.boardgamegeek.ui.widget.TimestampView;
import com.boardgamegeek.util.AnimationUtils;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.HelpUtils;
import com.boardgamegeek.util.PaletteUtils;
import com.boardgamegeek.util.PlayerCountRecommendation;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.ScrimUtils;
import com.boardgamegeek.util.ShowcaseViewWizard;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.TaskUtils;
import com.boardgamegeek.util.UIUtils;
import com.github.amlcurran.showcaseview.targets.Target;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import hugo.weaving.DebugLog;
import icepick.Icepick;
import icepick.State;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class GameFragment extends Fragment implements LoaderCallbacks<Cursor>, OnRefreshListener {
	private static final String KEY_GAME_ID = "GAME_ID";
	private static final String KEY_GAME_NAME = "GAME_NAME";
	private static final String KEY_ICON_COLOR = "ICON_COLOR";
	private static final String KEY_DARK_COLOR = "DARK_COLOR";

	private static final int HELP_VERSION = 2;
	private static final int AGE_IN_DAYS_TO_REFRESH = 7;

	private static final int GAME_TOKEN = 0x11;
	private static final int DESIGNER_TOKEN = 0x12;
	private static final int ARTIST_TOKEN = 0x13;
	private static final int PUBLISHER_TOKEN = 0x14;
	private static final int CATEGORY_TOKEN = 0x15;
	private static final int MECHANIC_TOKEN = 0x16;
	private static final int EXPANSION_TOKEN = 0x17;
	private static final int BASE_GAME_TOKEN = 0x18;
	private static final int RANK_TOKEN = 0x19;
	private static final int SUGGESTED_LANGUAGE_TOKEN = 0x23;
	private static final int SUGGESTED_AGE_TOKEN = 0x24;
	private static final int SUGGESTED_PLAYER_COUNT_TOKEN = 0x25;

	private Unbinder unbinder;

	@BindView(R.id.swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
	@BindView(R.id.progress) ContentLoadingProgressBar progressBar;
	@BindView(R.id.empty) TextView emptyView;
	@BindView(R.id.game_info_root) View rootContainer;
	@BindView(R.id.game_rating) TextView ratingView;
	@BindView(R.id.game_description) TextView descriptionView;
	@BindView(R.id.game_year_published) TextView yearPublishedView;

	@BindView(R.id.number_of_players) TextView numberOfPlayersView;
	@BindView(R.id.number_of_players_community) TextView numberOfPlayersCommunity;
	@BindView(R.id.number_of_players_votes) TextView numberOfPlayersVotes;

	@BindView(R.id.play_time) TextView playTimeView;

	@BindView(R.id.player_age_message) TextView playerAgeMessage;
	@BindView(R.id.player_age_poll) TextView playerAgePoll;
	@BindView(R.id.player_age_votes) TextView playerAgeVotes;

	@BindView(R.id.game_rank) TextView rankView;
	@BindView(R.id.game_types) TextView typesView;
	@BindView(R.id.icon_favorite) ImageView favoriteView;

	@BindView(R.id.game_info_designers) GameDetailRow designersView;
	@BindView(R.id.game_info_artists) GameDetailRow artistsView;
	@BindView(R.id.game_info_publishers) GameDetailRow publishersView;
	@BindView(R.id.game_info_categories) GameDetailRow categoriesView;
	@BindView(R.id.game_info_mechanics) GameDetailRow mechanicsView;
	@BindView(R.id.game_info_expansions) GameDetailRow expansionsView;
	@BindView(R.id.game_info_base_games) GameDetailRow baseGamesView;

	@BindView(R.id.game_ratings_votes) TextView ratingsVotes;

	@BindView(R.id.forums_last_post_date) TimestampView forumsLastPostDateView;

	@BindView(R.id.game_weight_message) TextView weightMessage;
	@BindView(R.id.game_weight_votes) TextView weightVotes;

	@BindView(R.id.language_dependence_message) TextView languageDependenceMessage;
	@BindView(R.id.language_dependence_votes) TextView languageDependenceVotes;

	@BindView(R.id.users_count) TextView userCountView;

	@BindView(R.id.game_info_id) TextView idView;
	@BindView(R.id.game_info_last_updated) TimestampView updatedView;

	@BindViews({
		R.id.game_info_designers,
		R.id.game_info_artists,
		R.id.game_info_publishers,
		R.id.game_info_categories,
		R.id.game_info_mechanics,
		R.id.game_info_expansions,
		R.id.game_info_base_games
	}) List<GameDetailRow> colorizedRows;
	@BindViews({
		R.id.icon_favorite,
		R.id.icon_rating,
		R.id.icon_game_year_published,
		R.id.icon_play_time,
		R.id.icon_number_of_players,
		R.id.icon_player_age,
		R.id.icon_forums,
		R.id.icon_weight,
		R.id.icon_language_dependence,
		R.id.icon_users,
	}) List<ImageView> colorizedIcons;

	private int gameId;
	private String gameName;
	@ColorInt private int iconColor = Color.TRANSPARENT;
	@ColorInt private int darkColor = Color.TRANSPARENT;
	private ShowcaseViewWizard showcaseViewWizard;
	private boolean isRefreshing;
	@State boolean mightNeedRefreshing = true;
	private Call<ForumListResponse> call;

	public static GameFragment newInstance(int gameId, String gameName, @ColorInt int iconColor, @ColorInt int darkColor) {
		Bundle args = new Bundle();
		args.putInt(KEY_GAME_ID, gameId);
		args.putString(KEY_GAME_NAME, gameName);
		args.putInt(KEY_ICON_COLOR, iconColor);
		args.putInt(KEY_DARK_COLOR, darkColor);
		GameFragment fragment = new GameFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	@DebugLog
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		EventBus.getDefault().register(this);
		Icepick.restoreInstanceState(this, savedInstanceState);
		setHasOptionsMenu(true);
		readBundle(getArguments());
	}

	private void readBundle(@Nullable Bundle bundle) {
		if (bundle == null) return;
		gameId = bundle.getInt(KEY_GAME_ID, BggContract.INVALID_ID);
		gameName = bundle.getString(KEY_GAME_NAME);
		iconColor = bundle.getInt(KEY_ICON_COLOR, Color.TRANSPARENT);
		darkColor = bundle.getInt(KEY_DARK_COLOR, Color.TRANSPARENT);
	}

	@Override
	@DebugLog
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_game, container, false);
		unbinder = ButterKnife.bind(this, rootView);

		colorize();

		swipeRefreshLayout.setOnRefreshListener(this);
		swipeRefreshLayout.setColorSchemeResources(PresentationUtils.getColorSchemeResources());

		idView.setText(String.valueOf(gameId));
		updatedView.setTimestamp(0);

		LoaderManager lm = getLoaderManager();
		lm.restartLoader(GAME_TOKEN, null, this);
		lm.restartLoader(RANK_TOKEN, null, this);
		lm.restartLoader(SUGGESTED_LANGUAGE_TOKEN, null, this);
		lm.restartLoader(SUGGESTED_AGE_TOKEN, null, this);
		lm.restartLoader(SUGGESTED_PLAYER_COUNT_TOKEN, null, this);

		showcaseViewWizard = setUpShowcaseViewWizard();
		showcaseViewWizard.maybeShowHelp();
		return rootView;
	}

	@Override
	public void onStop() {
		super.onStop();
		if (call != null) call.cancel();
	}

	@Override
	@DebugLog
	public void onDestroyView() {
		super.onDestroyView();
		unbinder.unbind();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		EventBus.getDefault().unregister(this);
	}

	@Override
	@DebugLog
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		Icepick.saveInstanceState(this, outState);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.help, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_help) {
			showcaseViewWizard.showHelp();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@NonNull
	private ShowcaseViewWizard setUpShowcaseViewWizard() {
		ShowcaseViewWizard wizard = new ShowcaseViewWizard(getActivity(), HelpUtils.HELP_GAME_KEY, HELP_VERSION);
		wizard.addTarget(R.string.help_game_menu, Target.NONE);
		wizard.addTarget(R.string.help_game_poll, new SafeViewTarget(R.id.number_of_players, getActivity()));
		wizard.addTarget(-1, new SafeViewTarget(R.id.player_age_root, getActivity()));
		return wizard;
	}

	@Override
	@DebugLog
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = null;
		switch (id) {
			case GAME_TOKEN:
				loader = new CursorLoader(getActivity(), Games.buildGameUri(gameId), Game.getProjection(), null, null, null);
				break;
			case DESIGNER_TOKEN:
				loader = new CursorLoader(getActivity(), GameDesigner.buildUri(gameId), GameDesigner.PROJECTION, null, null, null);
				break;
			case ARTIST_TOKEN:
				loader = new CursorLoader(getActivity(), GameArtist.buildUri(gameId), GameArtist.PROJECTION, null, null, null);
				break;
			case PUBLISHER_TOKEN:
				loader = new CursorLoader(getActivity(), GamePublisher.buildUri(gameId), GamePublisher.PROJECTION, null, null, null);
				break;
			case CATEGORY_TOKEN:
				loader = new CursorLoader(getActivity(), GameCategory.buildUri(gameId), GameCategory.PROJECTION, null, null, null);
				break;
			case MECHANIC_TOKEN:
				loader = new CursorLoader(getActivity(), GameMechanic.buildUri(gameId), GameMechanic.PROJECTION, null, null, null);
				break;
			case EXPANSION_TOKEN:
				loader = new CursorLoader(getActivity(), GameExpansion.buildUri(gameId), GameExpansion.PROJECTION, GameExpansion.getSelection(), GameExpansion.getSelectionArgs(), null);
				break;
			case BASE_GAME_TOKEN:
				loader = new CursorLoader(getActivity(), GameBaseGame.buildUri(gameId), GameBaseGame.PROJECTION, GameBaseGame.getSelection(), GameBaseGame.getSelectionArgs(), null);
				break;
			case RANK_TOKEN:
				loader = new CursorLoader(getActivity(), GameRank.buildUri(gameId), GameRank.PROJECTION, null, null, null);
				break;
			case SUGGESTED_LANGUAGE_TOKEN:
				loader = new CursorLoader(getActivity(), GameSuggestedLanguage.buildUri(gameId), GameSuggestedLanguage.PROJECTION, null, null, GameSuggestedLanguage.SORT);
				break;
			case SUGGESTED_AGE_TOKEN:
				loader = new CursorLoader(getActivity(), GameSuggestedAge.buildUri(gameId), GameSuggestedAge.PROJECTION, null, null, GameSuggestedAge.SORT);
				break;
			case SUGGESTED_PLAYER_COUNT_TOKEN:
				loader = new CursorLoader(getActivity(), GameSuggestedPlayerCount.buildUri(gameId), GameSuggestedPlayerCount.PROJECTION, null, null, null);
				break;
			default:
				Timber.w("Invalid query token=%s", id);
				break;
		}
		return loader;
	}

	@Override
	@DebugLog
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) return;

		switch (loader.getId()) {
			case GAME_TOKEN:
				onGameQueryComplete(cursor);
				LoaderManager lm = getLoaderManager();
				lm.restartLoader(DESIGNER_TOKEN, null, this);
				lm.restartLoader(ARTIST_TOKEN, null, this);
				lm.restartLoader(PUBLISHER_TOKEN, null, this);
				lm.restartLoader(CATEGORY_TOKEN, null, this);
				lm.restartLoader(MECHANIC_TOKEN, null, this);
				lm.restartLoader(EXPANSION_TOKEN, null, this);
				lm.restartLoader(BASE_GAME_TOKEN, null, this);
				fetchForumInfo();
				break;
			case DESIGNER_TOKEN:
				onListQueryComplete(cursor, designersView);
				break;
			case ARTIST_TOKEN:
				onListQueryComplete(cursor, artistsView);
				break;
			case PUBLISHER_TOKEN:
				onListQueryComplete(cursor, publishersView);
				break;
			case CATEGORY_TOKEN:
				onListQueryComplete(cursor, categoriesView);
				break;
			case MECHANIC_TOKEN:
				onListQueryComplete(cursor, mechanicsView);
				break;
			case EXPANSION_TOKEN:
				onListQueryComplete(cursor, expansionsView);
				break;
			case BASE_GAME_TOKEN:
				onListQueryComplete(cursor, baseGamesView);
				break;
			case RANK_TOKEN:
				onRankQueryComplete(cursor);
				break;
			case SUGGESTED_LANGUAGE_TOKEN:
				onLanguagePollQueryComplete(cursor);
				break;
			case SUGGESTED_AGE_TOKEN:
				onAgePollQueryComplete(cursor);
				break;
			case SUGGESTED_PLAYER_COUNT_TOKEN:
				onPlayerCountQueryComplete(cursor);
				break;
			default:
				cursor.close();
				break;
		}
	}

	private void fetchForumInfo() {
		if (forumsLastPostDateView.getVisibility() == VISIBLE) return;

		BggService bggService = Adapter.createForXml();
		call = bggService.forumList(BggService.FORUM_TYPE_THING, gameId);
		call.enqueue(new Callback<ForumListResponse>() {
			@Override
			public void onResponse(@NonNull Call<ForumListResponse> call, @NonNull Response<ForumListResponse> response) {
				if (response.isSuccessful() && forumsLastPostDateView != null) {
					long lastPostDate = 0;
					String title = "";
					for (Forum forum : response.body().getForums()) {
						if (forum.lastPostDate() > lastPostDate) {
							lastPostDate = forum.lastPostDate();
							title = forum.title;
						}
					}
					forumsLastPostDateView.setFormatArg(title);
					forumsLastPostDateView.setTimestamp(lastPostDate);
				}
			}

			@Override
			public void onFailure(@NonNull Call<ForumListResponse> call, @NonNull Throwable t) {
				if (call.isCanceled()) return;
				Timber.w("Failed fetching forum for game %s: %s", gameId, t.getMessage());
			}
		});
	}

	@Override
	@DebugLog
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	@Override
	public void onRefresh() {
		requestRefresh();
	}

	@DebugLog
	private void requestRefresh() {
		mightNeedRefreshing = false;
		if (!isRefreshing) {
			updateRefreshStatus(true);
			TaskUtils.executeAsyncTask(new SyncGameTask(getContext(), gameId));
		} else {
			updateRefreshStatus(false);
		}
	}

	@SuppressWarnings("unused")
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(SyncGameTask.CompletedEvent event) {
		if (event.getGameId() == gameId) {
			updateRefreshStatus(false);
		}
	}

	protected void updateRefreshStatus(boolean refreshing) {
		this.isRefreshing = refreshing;
		if (swipeRefreshLayout != null) {
			swipeRefreshLayout.post(new Runnable() {
				@Override
				public void run() {
					if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(isRefreshing);
				}
			});
		}
	}

	@DebugLog
	private void colorize() {
		if (!isAdded()) return;

		if (iconColor != Color.TRANSPARENT) {
			if (colorizedRows != null) ButterKnife.apply(colorizedRows, GameDetailRow.rgbIconSetter, iconColor);
			if (colorizedIcons != null) ButterKnife.apply(colorizedIcons, PaletteUtils.rgbIconSetter, iconColor);
		}
	}

	@DebugLog
	private void onGameQueryComplete(Cursor cursor) {
		if (cursor == null || !cursor.moveToFirst()) {
			if (mightNeedRefreshing) {
				mightNeedRefreshing = false;
				requestRefresh();
			}
			progressBar.hide();
			AnimationUtils.fadeOut(rootContainer);
			AnimationUtils.fadeIn(emptyView);
		} else {
			Game game = Game.fromCursor(cursor);

			notifyChange(game);
			gameName = game.getName();

			yearPublishedView.setText(PresentationUtils.describeYear(getContext(), game.getYearPublished()));

			rankView.setText(PresentationUtils.describeRank(getContext(), game.getRank(), BggService.RANK_TYPE_SUBTYPE, game.getSubtype()));
			favoriteView.setImageResource(game.isFavorite() ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
			favoriteView.setTag(R.id.favorite, game.isFavorite());

			ratingView.setText(PresentationUtils.describeRating(getContext(), game.getRating()));
			final CharSequence ratings = PresentationUtils.getQuantityText(getActivity(), R.plurals.ratings_suffix, game.getUsersRated(), game.getUsersRated());
			final CharSequence comments = PresentationUtils.getQuantityText(getActivity(), R.plurals.comments_suffix, game.getUsersCommented(), game.getUsersCommented());
			ratingsVotes.setText(TextUtils.concat(ratings, " & ", comments));
			ColorUtils.setTextViewBackground(ratingView, ColorUtils.getRatingColor(game.getRating()));

			idView.setText(String.valueOf(game.getId()));
			updatedView.setTimestamp(game.getUpdated());
			UIUtils.setTextMaybeHtml(descriptionView, game.getDescription());
			ScrimUtils.applyWhiteScrim(descriptionView);
			numberOfPlayersView.setText(PresentationUtils.describePlayerRange(getContext(), game.getMinPlayers(), game.getMaxPlayers()));

			playTimeView.setText(PresentationUtils.describeMinuteRange(getContext(), game.getMinPlayingTime(), game.getMaxPlayingTime(), game.getPlayingTime()));

			playerAgeMessage.setText(PresentationUtils.describePlayerAge(getContext(), game.getMinimumAge()));

			weightMessage.setText(PresentationUtils.describeWeight(getContext(), game.getAverageWeight()));
			ColorUtils.setTextViewBackground(weightMessage, ColorUtils.getFiveStageColor(game.getAverageWeight()));
			PresentationUtils.setTextOrHide(weightVotes, PresentationUtils.getQuantityText(getActivity(), R.plurals.votes_suffix, game.getNumberWeights(), game.getNumberWeights()));

			final int maxUsers = game.getMaxUsers();
			userCountView.setText(PresentationUtils.getQuantityText(getActivity(), R.plurals.users_suffix, maxUsers, maxUsers));

			if (mightNeedRefreshing) {
				mightNeedRefreshing = false;
				if (DateTimeUtils.howManyDaysOld(game.getUpdated()) > AGE_IN_DAYS_TO_REFRESH ||
					game.getPollsVoteCount() == 0)
					requestRefresh();
			}
			progressBar.hide();
			AnimationUtils.fadeOut(emptyView);
			AnimationUtils.fadeIn(rootContainer);
		}
	}

	@DebugLog
	private void notifyChange(Game game) {
		GameInfoChangedEvent event = new GameInfoChangedEvent(game.getName(), game.getSubtype(), game.getImageUrl(), game.getThumbnailUrl(), game.getHeroImageUrl(), game.getCustomPlayerSort(), game.isFavorite());
		EventBus.getDefault().post(event);
	}

	@DebugLog
	private void onListQueryComplete(Cursor cursor, GameDetailRow view) {
		if (cursor == null || !cursor.moveToFirst()) {
			view.setVisibility(GONE);
			view.clear();
		} else {
			view.setVisibility(VISIBLE);
			view.bind(cursor, GameList.NAME_COLUMN_INDEX, GameList.ID_COLUMN_INDEX, gameId, gameName);
		}
	}

	@DebugLog
	private void onRankQueryComplete(Cursor cursor) {
		if (typesView != null) {
			if (cursor != null && cursor.getCount() > 0) {
				CharSequence cs = null;
				while (cursor.moveToNext()) {
					GameRank rank = GameRank.fromCursor(cursor);
					if (rank.isFamilyType()) {
						if (cs != null) {
							cs = PresentationUtils.getText(getContext(), R.string.rank_div, cs,
								PresentationUtils.describeRank(getContext(), rank.getRank(), rank.getType(), rank.getName()));
						} else {
							cs = PresentationUtils.describeRank(getContext(), rank.getRank(), rank.getType(), rank.getName());
						}
					}
				}
				if (TextUtils.isEmpty(cs)) {
					typesView.setVisibility(GONE);
				} else {
					typesView.setText(cs);
					typesView.setVisibility(VISIBLE);
				}
			} else {
				typesView.setVisibility(GONE);
			}
		}
	}

	@DebugLog
	private void onLanguagePollQueryComplete(Cursor cursor) {
		int totalVotes = 0;
		int totalLevel = 0;
		if (cursor != null) {
			while (cursor.moveToNext()) {
				GameSuggestedLanguage gsl = GameSuggestedLanguage.fromCursor(cursor);
				totalVotes = Math.max(totalVotes, gsl.getTotalVotes());
				totalLevel += gsl.getVotes() * gsl.getLevel();
			}
		}
		double score = (double) totalLevel / totalVotes;
		languageDependenceMessage.setText(PresentationUtils.describeLanguageDependence(getContext(), score));
		ColorUtils.setTextViewBackground(languageDependenceMessage, ColorUtils.getFiveStageColor(score));
		PresentationUtils.setTextOrHide(languageDependenceVotes,
			PresentationUtils.getQuantityText(getContext(), R.plurals.votes_suffix, totalVotes, totalVotes));
	}

	@DebugLog
	private void onAgePollQueryComplete(Cursor cursor) {
		String currentValue = "";
		int maxVotes = 0;
		int totalVotes = 0;
		if (cursor != null && cursor.moveToFirst()) {
			do {
				GameSuggestedAge gsa = GameSuggestedAge.fromCursor(cursor);
				totalVotes = Math.max(totalVotes, gsa.getTotalVotes());
				if (gsa.getVotes() > maxVotes) {
					maxVotes = gsa.getVotes();
					currentValue = gsa.getValue();
				}
			} while (cursor.moveToNext());
		}

		if (!TextUtils.isEmpty(currentValue))
			PresentationUtils.setTextOrHide(playerAgePoll, PresentationUtils.describePlayerAge(getContext(), currentValue));

		PresentationUtils.setTextOrHide(playerAgeVotes,
			PresentationUtils.getQuantityText(getActivity(), R.plurals.votes_suffix, totalVotes, totalVotes));
	}

	@DebugLog
	private void onPlayerCountQueryComplete(Cursor cursor) {
		int totalVotes = 0;
		if (cursor != null && cursor.moveToFirst()) {
			List<Integer> bestCounts = new ArrayList<>();
			List<Integer> recommendedCounts = new ArrayList<>();
			do {
				GameSuggestedPlayerCount suggestedPlayerCount = GameSuggestedPlayerCount.fromCursor(cursor);
				totalVotes = Math.max(totalVotes, suggestedPlayerCount.getTotalVotes());
				if (suggestedPlayerCount.getRecommendation() == PlayerCountRecommendation.BEST) {
					bestCounts.add(suggestedPlayerCount.getPlayerCount());
					recommendedCounts.add(suggestedPlayerCount.getPlayerCount());
				} else if (suggestedPlayerCount.getRecommendation() == PlayerCountRecommendation.RECOMMENDED) {
					recommendedCounts.add(suggestedPlayerCount.getPlayerCount());
				}
			} while (cursor.moveToNext());

			CharSequence communityText = "";
			if (bestCounts.size() > 0) {
				communityText = PresentationUtils.getText(getContext(), R.string.best_prefix, StringUtils.formatRange(bestCounts));
				if (recommendedCounts.size() > 0 && !bestCounts.equals(recommendedCounts)) {
					final CharSequence good = PresentationUtils.getText(getContext(), R.string.recommended_prefix, StringUtils.formatRange(recommendedCounts));
					communityText = TextUtils.concat(communityText, " & ", good);
				}
			} else if (recommendedCounts.size() > 0) {
				communityText = PresentationUtils.getText(getContext(), R.string.recommended_prefix, StringUtils.formatRange(recommendedCounts));
			}
			PresentationUtils.setTextOrHide(numberOfPlayersCommunity, communityText);
		} else {
			numberOfPlayersCommunity.setVisibility(GONE);
		}
		PresentationUtils.setTextOrHide(numberOfPlayersVotes,
			PresentationUtils.getQuantityText(getContext(), R.plurals.votes_suffix, totalVotes, totalVotes));
	}

	@OnClick(R.id.icon_favorite)
	@DebugLog
	public void onFavoriteClick() {
		boolean isFavorite = (boolean) favoriteView.getTag(R.id.favorite);
		TaskUtils.executeAsyncTask(new FavoriteGameTask(getContext(), gameId, !isFavorite));
	}

	@OnClick(R.id.game_rank_root)
	@DebugLog
	public void onRankClick() {
		RanksFragment.launch(this, gameId);
	}

	@SuppressLint("InflateParams")
	@OnClick(R.id.game_description)
	@DebugLog
	public void onDescriptionClick() {
		View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_text, null);
		((TextView) v.findViewById(R.id.text)).setText(descriptionView.getText());
		new Builder(getContext(), R.style.Theme_bgglight_Dialog_Alert)
			.setView(v)
			.show();
	}

	@OnClick(R.id.forums_root)
	@DebugLog
	public void onForumsClick() {
		GameForumsActivity.start(getContext(), Games.buildGameUri(gameId), gameName);
	}

	@OnClick(R.id.language_dependence_root)
	@DebugLog
	public void onLanguageDependenceClick() {
		PollFragment.launchLanguageDependence(this, gameId);
	}

	@OnClick(R.id.users_count_root)
	@DebugLog
	public void onUsersClick() {
		GameUsersDialogFragment.launch(this, gameId, darkColor);
	}

	@DebugLog
	@OnClick(R.id.ratings_root)
	public void onRatingsClick() {
		CommentsActivity.startRating(getContext(), Games.buildGameUri(gameId), gameName);
	}

	@SuppressWarnings("unused")
	@Subscribe
	public void onEvent(ColorEvent event) {
		if (event.getGameId() == gameId) {
			iconColor = event.getIconColor();
			darkColor = event.getDarkColor();
			colorize();
		}
	}

	@SuppressWarnings("unused")
	@Subscribe
	public void onEvent(CollectionItemAddedEvent event) {
		if (event.getGameId() == gameId) {
			SyncService.sync(getActivity(), SyncService.FLAG_SYNC_COLLECTION_UPLOAD);
		}
	}

	@OnClick({ R.id.player_age_root })
	@DebugLog
	public void onPollClick() {
		PollFragment.launchSuggestedPlayerAge(this, gameId);
	}

	@OnClick({ R.id.number_of_players_root })
	@DebugLog
	public void onSuggestedPlayerCountPollClick() {
		SuggestedPlayerCountPollFragment.launch(this, gameId);
	}
}