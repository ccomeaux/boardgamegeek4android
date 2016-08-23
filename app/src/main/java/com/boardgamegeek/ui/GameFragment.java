package com.boardgamegeek.ui;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.graphics.Palette;
import android.support.v7.graphics.Palette.Swatch;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.events.CollectionItemUpdatedEvent;
import com.boardgamegeek.events.GameInfoChangedEvent;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.Forum;
import com.boardgamegeek.model.ForumListResponse;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggContract.Categories;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.GamePollResultsResult;
import com.boardgamegeek.provider.BggContract.GamePolls;
import com.boardgamegeek.provider.BggContract.GameRanks;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.GamesExpansions;
import com.boardgamegeek.provider.BggContract.Mechanics;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.provider.BggContract.Publishers;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.service.UpdateService;
import com.boardgamegeek.tasks.AddCollectionItemTask;
import com.boardgamegeek.ui.adapter.GameColorAdapter;
import com.boardgamegeek.ui.dialog.CollectionStatusDialogFragment;
import com.boardgamegeek.ui.dialog.CollectionStatusDialogFragment.CollectionStatusDialogListener;
import com.boardgamegeek.ui.widget.GameCollectionRow;
import com.boardgamegeek.ui.widget.GameDetailRow;
import com.boardgamegeek.ui.widget.SafeViewTarget;
import com.boardgamegeek.ui.widget.StatBar;
import com.boardgamegeek.ui.widget.TimestampView;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.CursorUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.DialogUtils;
import com.boardgamegeek.util.HelpUtils;
import com.boardgamegeek.util.PaletteUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.PresentationUtils;
import com.boardgamegeek.util.ShowcaseViewWizard;
import com.boardgamegeek.util.TaskUtils;
import com.boardgamegeek.util.UIUtils;
import com.github.amlcurran.showcaseview.targets.Target;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

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

public class GameFragment extends Fragment implements LoaderCallbacks<Cursor> {
	private static final int HELP_VERSION = 2;
	private static final int AGE_IN_DAYS_TO_REFRESH = 7;
	private static final String POLL_TYPE_LANGUAGE_DEPENDENCE = "language_dependence";

	private Uri gameUri;
	private String gameName;
	private String imageUrl;
	private String thumbnailUrl;
	private boolean arePlayersCustomSorted;

	private Unbinder unbinder;

	@BindView(R.id.game_rating) TextView ratingView;
	@BindView(R.id.game_description) TextView descriptionView;
	@BindView(R.id.game_rank) TextView rankView;
	@BindView(R.id.game_year_published) TextView yearPublishedView;

	@BindView(R.id.primary_info_container) View primaryInfoContainer;
	@BindView(R.id.number_of_players) TextView numberOfPlayersView;
	@BindView(R.id.play_time) TextView playTimeView;
	@BindView(R.id.player_age) TextView playerAgeView;

	@BindView(R.id.game_subtype) TextView subtypeView;
	@BindView(R.id.game_subtype_container) ViewGroup subtypeContainer;
	@BindView(R.id.subtype_expander) ImageView subtypeExpander;

	@BindView(R.id.game_info_designers) GameDetailRow designersView;
	@BindView(R.id.game_info_artists) GameDetailRow artistsView;
	@BindView(R.id.game_info_publishers) GameDetailRow publishersView;
	@BindView(R.id.game_info_categories) GameDetailRow categoriesView;
	@BindView(R.id.game_info_mechanics) GameDetailRow mechanicsView;
	@BindView(R.id.game_info_expansions) GameDetailRow expansionsView;
	@BindView(R.id.game_info_base_games) GameDetailRow baseGamesView;

	@BindView(R.id.collection_card) View collectionCard;
	@BindView(R.id.collection_container) ViewGroup collectionContainer;
	@BindView(R.id.collection_add_button) TextView collectionAddButton;

	@BindView(R.id.plays_card) View playsCard;
	@BindView(R.id.plays_root) View playsRoot;
	@BindView(R.id.plays_label) TextView playsLabel;
	@BindView(R.id.plays_last_play) TextView lastPlayView;
	@BindView(R.id.play_stats_root) View playStatsRoot;
	@BindView(R.id.colors_root) View colorsRoot;
	@BindView(R.id.game_colors_label) TextView colorsLabel;

	@BindView(R.id.game_ratings_label) TextView ratingsLabel;
	@BindView(R.id.game_ratings_votes) TextView ratingsVotes;
	@BindView(R.id.game_ratings_standard_deviation) TextView ratingsStandardDeviation;

	@BindView(R.id.game_comments_label) TextView commentsLabel;

	@BindView(R.id.forums_last_post_date) TimestampView forumsLastPostDateView;

	@BindView(R.id.game_weight) TextView weightView;
	@BindView(R.id.game_weight_votes) TextView weightVotes;

	@BindView(R.id.language_dependence_details) TextView languageDependenceDetails;

	@BindView(R.id.users_count) TextView userCountView;
	@BindView(R.id.users_owning_bar) StatBar numberOwningBar;
	@BindView(R.id.users_trading_bar) StatBar numberTradingBar;
	@BindView(R.id.users_wanting_bar) StatBar numberWantingBar;
	@BindView(R.id.users_wishing_bar) StatBar numberWishingBar;

	@BindView(R.id.game_info_id) TextView idView;
	@BindView(R.id.game_info_last_updated) TimestampView updatedView;

	@BindViews({
		R.id.game_year_published,
		R.id.number_of_players,
		R.id.play_time,
		R.id.player_age
	}) List<TextView> colorizedTextViews;
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
		R.id.card_header_details,
		R.id.card_header_collection,
		R.id.card_header_plays,
		R.id.card_header_user_feedback,
		R.id.card_header_links
	}) List<TextView> colorizedHeaders;
	@BindViews({
		R.id.icon_plays,
		R.id.icon_play_stats,
		R.id.icon_colors,
		R.id.icon_forums,
		R.id.icon_comments,
		R.id.icon_ratings,
		R.id.icon_weight,
		R.id.icon_language_dependence,
		R.id.icon_users,
		R.id.icon_link_bgg,
		R.id.icon_link_bg_prices,
		R.id.icon_link_amazon,
		R.id.icon_link_ebay
	}) List<ImageView> colorizedIcons;
	@BindViews({
		R.id.users_owning_bar,
		R.id.users_trading_bar,
		R.id.users_wanting_bar,
		R.id.users_wishing_bar,
	}) List<StatBar> statBars;

	@State boolean isRanksExpanded;
	@State boolean isDescriptionExpanded;
	private boolean mightNeedRefreshing;
	private Palette palette;
	private ShowcaseViewWizard showcaseViewWizard;

	@Override
	@DebugLog
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Icepick.restoreInstanceState(this, savedInstanceState);
		setHasOptionsMenu(true);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		gameUri = intent.getData();
	}

	@DebugLog
	@Override
	public void onStart() {
		super.onStart();
		EventBus.getDefault().register(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		EventBus.getDefault().unregister(this);
	}

	@Override
	@DebugLog
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_game, container, false);
		unbinder = ButterKnife.bind(this, rootView);

		colorize();
		openOrCloseDescription();

		mightNeedRefreshing = true;
		LoaderManager lm = getLoaderManager();
		lm.restartLoader(GameQuery._TOKEN, null, this);
		lm.restartLoader(RankQuery._TOKEN, null, this);
		if (shouldShowPlays()) {
			lm.restartLoader(PlaysQuery._TOKEN, null, this);
		}
		if (PreferencesUtils.showLogPlay(getActivity())) {
			lm.restartLoader(ColorQuery._TOKEN, null, this);
		}
		lm.restartLoader(LanguagePollQuery._TOKEN, null, this);

		showcaseViewWizard = setUpShowcaseViewWizard();
		showcaseViewWizard.maybeShowHelp();
		return rootView;
	}

	@Override
	@DebugLog
	public void onDestroyView() {
		super.onDestroyView();
		unbinder.unbind();
	}

	@Override
	@DebugLog
	public void onSaveInstanceState(Bundle outState) {
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
		wizard.addTarget(R.string.help_game_log_play, new SafeViewTarget(R.id.fab, getActivity()));
		wizard.addTarget(R.string.help_game_poll, new SafeViewTarget(R.id.number_of_players, getActivity()));
		wizard.addTarget(-1, new SafeViewTarget(R.id.player_age, getActivity()));
		return wizard;
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
					PlayItems.OBJECT_ID + "=? AND " + Plays.SYNC_STATUS + " !=?",
					new String[] { String.valueOf(gameId), String.valueOf(Play.SYNC_STATUS_PENDING_DELETE) }, null);
				break;
			case ColorQuery._TOKEN:
				loader = new CursorLoader(getActivity(), GameColorAdapter.createUri(gameId), GameColorAdapter.PROJECTION, null, null, null);
				break;
			case LanguagePollQuery._TOKEN:
				loader = new CursorLoader(getActivity(),
					Games.buildPollResultsResultUri(gameId, POLL_TYPE_LANGUAGE_DEPENDENCE),
					LanguagePollQuery.PROJECTION,
					null,
					null,
					LanguagePollQuery.SORT);
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
				lm.restartLoader(CollectionQuery._TOKEN, null, this);
				lm.restartLoader(DesignerQuery._TOKEN, null, this);
				lm.restartLoader(ArtistQuery._TOKEN, null, this);
				lm.restartLoader(PublisherQuery._TOKEN, null, this);
				lm.restartLoader(CategoryQuery._TOKEN, null, this);
				lm.restartLoader(MechanicQuery._TOKEN, null, this);
				lm.restartLoader(ExpansionQuery._TOKEN, null, this);
				lm.restartLoader(BaseGameQuery._TOKEN, null, this);
				fetchForumInfo();
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
				colorsLabel.setText(PresentationUtils.getQuantityText(getActivity(), R.plurals.colors_suffix, count, count));
				break;
			case LanguagePollQuery._TOKEN:
				onLanguagePollQueryComplete(cursor);
				break;
			default:
				cursor.close();
				break;
		}
	}

	private void fetchForumInfo() {
		if (forumsLastPostDateView.getVisibility() == View.VISIBLE) {
			return;
		}
		BggService bggService = Adapter.createForXml();
		Call<ForumListResponse> call = bggService.forumList(BggService.FORUM_TYPE_THING, Games.getGameId(gameUri));
		call.enqueue(new Callback<ForumListResponse>() {
			@Override
			public void onResponse(Call<ForumListResponse> call, Response<ForumListResponse> response) {
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
			public void onFailure(Call<ForumListResponse> call, Throwable t) {
				Timber.w("Failed fetching forum for game %s: %s", Games.getGameId(gameUri), t.getMessage());
			}
		});
	}

	@Override
	@DebugLog
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	@DebugLog
	public void onPaletteGenerated(Palette palette) {
		this.palette = palette;
		colorize();
	}

	@DebugLog
	private void colorize() {
		if (palette == null || primaryInfoContainer == null || !isAdded()) {
			return;
		}
		Palette.Swatch swatch = PaletteUtils.getInverseSwatch(palette, getResources().getColor(R.color.info_background));
		primaryInfoContainer.setBackgroundColor(swatch.getRgb());
		ButterKnife.apply(colorizedTextViews, PaletteUtils.colorTextViewOnBackgroundSetter, swatch);

		swatch = PaletteUtils.getIconSwatch(palette);
		ButterKnife.apply(colorizedRows, GameDetailRow.colorIconSetter, swatch);
		ButterKnife.apply(colorizedIcons, PaletteUtils.colorIconSetter, swatch);

		ButterKnife.apply(colorizedHeaders, PaletteUtils.colorTextViewSetter, PaletteUtils.getHeaderSwatch(palette));
		ButterKnife.apply(statBars, StatBar.colorSetter, PaletteUtils.getDarkSwatch(palette));
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

		rankView.setText(game.getRankDescription());
		yearPublishedView.setText(game.getYearPublished());
		subtypeView.setText(game.getSubtype());
		ratingView.setText(game.getRatingDescription());
		ColorUtils.setViewBackground(ratingView, ColorUtils.getRatingColor(game.Rating));
		idView.setText(String.valueOf(game.Id));
		updatedView.setTimestamp(game.Updated);
		UIUtils.setTextMaybeHtml(descriptionView, game.Description);
		numberOfPlayersView.setText(game.getPlayerRangeDescription());
		playTimeView.setText(game.getPlayingTimeDescription());
		playerAgeView.setText(game.getAgeDescription());
		commentsLabel.setText(PresentationUtils.getQuantityText(getActivity(), R.plurals.comments_suffix, game.UsersCommented, game.UsersCommented));

		ratingsLabel.setText(PresentationUtils.getText(getActivity(),
			R.string.average_rating_prefix, PresentationUtils.describeAverageRating(getActivity(), game.BayesAverage)));
		ratingsVotes.setText(PresentationUtils.getText(getActivity(), R.string.votes_suffix, game.UsersRated));
		ratingsStandardDeviation.setText(getString(R.string.standard_deviation_prefix, PresentationUtils.describeAverageRating(getActivity(), game.StandardDeviation)));

		weightView.setText(PresentationUtils.describeWeight(getActivity(), game.AverageWeight));
		weightVotes.setText(PresentationUtils.getText(getActivity(), R.string.votes_suffix, game.NumberWeights));

		final int maxUsers = game.getMaxUsers();
		userCountView.setText(PresentationUtils.getQuantityText(getActivity(), R.plurals.users_suffix, maxUsers, maxUsers));
		numberOwningBar.setBar(R.string.owning_meter_text, game.NumberOwned, maxUsers);
		numberTradingBar.setBar(R.string.trading_meter_text, game.NumberTrading, maxUsers);
		numberWantingBar.setBar(R.string.wanting_meter_text, game.NumberWanting, maxUsers);
		numberWishingBar.setBar(R.string.wishing_meter_text, game.NumberWishing, maxUsers);

		if (shouldShowPlays()) {
			playsCard.setVisibility(View.VISIBLE);
			playStatsRoot.setVisibility(View.VISIBLE);
		}

		if (mightNeedRefreshing
			&& (game.PollsCount == 0 || DateTimeUtils.howManyDaysOld(game.Updated) > AGE_IN_DAYS_TO_REFRESH)) {
			triggerRefresh();
		}
		mightNeedRefreshing = false;
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

		TextView tv = (TextView) layout.findViewById(R.id.rank_row_label);
		tv.setText(PresentationUtils.describeRankName(getActivity(), "family", label));

		tv = (TextView) layout.findViewById(R.id.rank_row_rank);
		tv.setText(PresentationUtils.describeRank(rank));

		tv = (TextView) layout.findViewById(R.id.rank_row_rating);
		tv.setText(PresentationUtils.describeAverageRating(getActivity(), rating));

		subtypeContainer.addView(layout);
	}

	@DebugLog
	private void onCollectionQueryComplete(Cursor cursor) {
		collectionCard.setVisibility(View.VISIBLE);
		collectionContainer.removeViews(2, collectionContainer.getChildCount() - 2);
		if (cursor.moveToFirst()) {
			collectionAddButton.setVisibility(View.GONE);
			do {
				GameCollectionRow row = new GameCollectionRow(getActivity());

				final long internalId = cursor.getLong(CollectionQuery._ID);
				final int gameId = Games.getGameId(gameUri);
				final int collectionId = cursor.getInt(CollectionQuery.COLLECTION_ID);
				final int yearPublished = cursor.getInt(CollectionQuery.YEAR_PUBLISHED);
				final String imageUrl = cursor.getString(CollectionQuery.COLLECTION_IMAGE_URL);
				row.bind(internalId, gameId, gameName, collectionId, yearPublished, imageUrl);

				final String thumbnailUrl = cursor.getString(CollectionQuery.COLLECTION_THUMBNAIL_URL);
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
		} else {
			collectionAddButton.setVisibility(View.VISIBLE);
		}
	}

	@OnClick(R.id.collection_add_button)
	void onAddToCollectionClick() {
		CollectionStatusDialogFragment statusDialogFragment = CollectionStatusDialogFragment.newInstance(
			collectionContainer,
			new CollectionStatusDialogListener() {
				@Override
				public void onSelectStatuses(List<String> selectedStatuses, int wishlistPriority) {
					int gameId = Games.getGameId(gameUri);
					AddCollectionItemTask task = new AddCollectionItemTask(getActivity(), gameId, selectedStatuses, wishlistPriority);
					TaskUtils.executeAsyncTask(task);
				}
			}
		);
		statusDialogFragment.setTitle(R.string.title_add_a_copy);
		DialogUtils.showFragment(getActivity(), statusDialogFragment, "status_dialog");
	}

	@DebugLog
	private void onPlaysQueryComplete(Cursor cursor) {
		if (cursor.moveToFirst()) {
			playsCard.setVisibility(View.VISIBLE);
			playsRoot.setVisibility(View.VISIBLE);

			int sum = cursor.getInt(PlaysQuery.SUM_QUANTITY);
			long date = CursorUtils.getDateInMillis(cursor, PlaysQuery.MAX_DATE);

			if (sum > 0) {
				String description = PresentationUtils.describePlayCount(getActivity(), sum);
				if (!TextUtils.isEmpty(description)) {
					description = " (" + description + ")";
				}
				playsLabel.setText(PresentationUtils.getQuantityText(getActivity(), R.plurals.plays_prefix, sum, sum, description));
			} else {
				playsLabel.setText(getResources().getString(R.string.no_plays));
			}

			if (date > 0) {
				lastPlayView.setText(PresentationUtils.getText(getActivity(), R.string.last_played_prefix, PresentationUtils.describePastDaySpan(date)));
				lastPlayView.setVisibility(View.VISIBLE);
			} else {
				lastPlayView.setVisibility(View.GONE);
			}
		}
	}

	@DebugLog
	private void onLanguagePollQueryComplete(Cursor cursor) {
		int totalLevel = 0;
		int totalVotes = 0;
		if (cursor != null) {
			while (cursor.moveToNext()) {
				totalVotes = cursor.getInt(LanguagePollQuery.POLL_TOTAL_VOTES);
				int level = cursor.getInt(LanguagePollQuery.POLL_RESULTS_RESULT_LEVEL) % 5;
				int votes = cursor.getInt(LanguagePollQuery.POLL_RESULTS_RESULT_VOTES);
				totalLevel += votes * level;
			}
		}
		languageDependenceDetails.setText(PresentationUtils.describeLanguageDependence(getActivity(), (double) totalLevel / totalVotes));
		languageDependenceDetails.setVisibility(View.VISIBLE);
	}

	@OnClick(R.id.rank_root)
	@DebugLog
	public void onRankClick() {
		isRanksExpanded = !isRanksExpanded;
		openOrCloseRanks();
	}

	@OnClick(R.id.game_description)
	@DebugLog
	public void onDescriptionClick() {
		isDescriptionExpanded = !isDescriptionExpanded;
		openOrCloseDescription();
	}

	@OnClick(R.id.plays_root)
	@DebugLog
	public void onPlaysClick() {
		Intent intent = new Intent(getActivity(), GamePlaysActivity.class);
		intent.setData(gameUri);
		intent.putExtra(ActivityUtils.KEY_GAME_NAME, gameName);
		intent.putExtra(ActivityUtils.KEY_IMAGE_URL, imageUrl);
		intent.putExtra(ActivityUtils.KEY_THUMBNAIL_URL, thumbnailUrl);
		intent.putExtra(ActivityUtils.KEY_CUSTOM_PLAYER_SORT, arePlayersCustomSorted);
		startActivity(intent);
	}

	@OnClick(R.id.play_stats_root)
	@DebugLog
	public void onPlayStatsClick() {
		Intent intent = new Intent(getActivity(), GamePlayStatsActivity.class);
		intent.setData(gameUri);
		intent.putExtra(ActivityUtils.KEY_GAME_NAME, gameName);
		if (palette != null) {
			final Swatch swatch = PaletteUtils.getHeaderSwatch(palette);
			intent.putExtra(ActivityUtils.KEY_HEADER_COLOR, swatch.getRgb());
		}
		startActivity(intent);
	}

	@OnClick(R.id.colors_root)
	@DebugLog
	public void onColorsClick() {
		Intent intent = new Intent(getActivity(), ColorsActivity.class);
		intent.setData(gameUri);
		intent.putExtra(ActivityUtils.KEY_GAME_NAME, gameName);
		startActivity(intent);
	}

	@OnClick(R.id.forums_root)
	@DebugLog
	public void onForumsClick() {
		Intent intent = new Intent(getActivity(), GameForumsActivity.class);
		intent.setData(gameUri);
		intent.putExtra(ActivityUtils.KEY_GAME_NAME, gameName);
		startActivity(intent);
	}

	@OnClick(R.id.language_dependence_root)
	@DebugLog
	public void onLanguageDependenceClick() {
		Bundle arguments = new Bundle(2);
		arguments.putInt(ActivityUtils.KEY_GAME_ID, Games.getGameId(gameUri));
		arguments.putString(ActivityUtils.KEY_TYPE, POLL_TYPE_LANGUAGE_DEPENDENCE);
		DialogUtils.launchDialog(this, new PollFragment(), "poll-dialog", arguments);
	}

	@OnClick(R.id.comments_root)
	@DebugLog
	public void onCommentsClick() {
		Intent intent = new Intent(getActivity(), CommentsActivity.class);
		intent.setData(gameUri);
		intent.putExtra(ActivityUtils.KEY_GAME_NAME, gameName);
		startActivity(intent);
	}

	@DebugLog
	@OnClick(R.id.ratings_root)
	public void onRatingsClick() {
		Intent intent = new Intent(getActivity(), CommentsActivity.class);
		intent.setData(gameUri);
		intent.putExtra(ActivityUtils.KEY_GAME_NAME, gameName);
		intent.putExtra(ActivityUtils.KEY_SORT, CommentsActivity.SORT_RATING);
		startActivity(intent);
	}

	@DebugLog
	private void openOrCloseRanks() {
		subtypeContainer.setVisibility(isRanksExpanded ? View.VISIBLE : View.GONE);
		subtypeExpander.setImageResource(isRanksExpanded ? R.drawable.expander_close : R.drawable.expander_open);
	}

	@DebugLog
	private void openOrCloseDescription() {
		descriptionView.setMaxLines(isDescriptionExpanded ? Integer.MAX_VALUE : 3);
		descriptionView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0,
			isDescriptionExpanded ? R.drawable.expander_close : R.drawable.expander_open);
	}

	@Subscribe
	public void onEvent(CollectionItemUpdatedEvent event) {
		SyncService.sync(getActivity(), SyncService.FLAG_SYNC_COLLECTION_UPLOAD);
	}

	@SuppressWarnings("unused")
	@DebugLog
	@OnClick({ R.id.link_bgg, R.id.link_bg_prices, R.id.link_amazon, R.id.link_ebay })
	void onLinkClick(View view) {
		switch (view.getId()) {
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

	@OnClick({ R.id.number_of_players, R.id.player_age })
	@DebugLog
	public void onPollClick(View view) {
		Bundle arguments = new Bundle(2);
		arguments.putInt(ActivityUtils.KEY_GAME_ID, Games.getGameId(gameUri));
		arguments.putString(ActivityUtils.KEY_TYPE, (String) view.getTag());
		DialogUtils.launchDialog(this, new PollFragment(), "poll-dialog", arguments);
	}

	@DebugLog
	public void triggerRefresh() {
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
		String[] PROJECTION = { GameRanks.GAME_RANK_NAME, GameRanks.GAME_RANK_VALUE, GameRanks.GAME_RANK_TYPE,
			GameRanks.GAME_RANK_BAYES_AVERAGE };
		int GAME_RANK_NAME = 0;
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
			Collection.COMMENT, Games.YEAR_PUBLISHED, Collection.RATING, Collection.IMAGE_URL };
		int _TOKEN = 0x20;
		int _ID = 0;
		int COLLECTION_ID = 1;
		int COLLECTION_NAME = 2;
		int COLLECTION_YEAR = 3;
		int COLLECTION_THUMBNAIL_URL = 4;
		int STATUS_1 = 5;
		int STATUS_N = 12;
		int STATUS_WISHLIST = 10;
		int STATUS_WISHLIST_PRIORITY = 13;
		int NUM_PLAYS = 14;
		int COMMENT = 15;
		int YEAR_PUBLISHED = 16;
		int RATING = 17;
		int COLLECTION_IMAGE_URL = 18;
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

	private interface LanguagePollQuery {
		String[] PROJECTION = {
			GamePollResultsResult.POLL_RESULTS_RESULT_VOTES,
			GamePollResultsResult.POLL_RESULTS_RESULT_LEVEL,
			GamePolls.POLL_TOTAL_VOTES
		};
		int _TOKEN = 0x23;
		int POLL_RESULTS_RESULT_VOTES = 0;
		int POLL_RESULTS_RESULT_LEVEL = 1;
		int POLL_TOTAL_VOTES = 2;
		String SORT = GamePollResultsResult.POLL_RESULTS_SORT_INDEX + " ASC, " + GamePollResultsResult.POLL_RESULTS_RESULT_SORT_INDEX;
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
			int max = Math.max(UsersRated, UsersCommented);
			max = Math.max(max, NumberOwned);
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
			return PresentationUtils.describeRating(getContext(), Rating);
		}

		@DebugLog
		public String getYearPublished() {
			return PresentationUtils.describeYear(getContext(), YearPublished);
		}

		@DebugLog
		public String getSubtype() {
			return PresentationUtils.describeRankName(getActivity(), "subtype", Subtype);
		}
	}

	private class Rank {
		final String Name;
		final int Rank;
		final double Rating;
		final String Type;

		Rank(Cursor cursor) {
			Name = cursor.getString(RankQuery.GAME_RANK_NAME);
			Rank = cursor.getInt(RankQuery.GAME_RANK_VALUE);
			Rating = cursor.getDouble(RankQuery.GAME_RANK_BAYES_AVERAGE);
			Type = cursor.getString(RankQuery.GAME_RANK_TYPE);
		}
	}
}