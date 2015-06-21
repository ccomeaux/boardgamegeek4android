package com.boardgamegeek.model.persister;

import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Game;
import com.boardgamegeek.model.Game.Poll;
import com.boardgamegeek.model.Game.Rank;
import com.boardgamegeek.model.Game.Result;
import com.boardgamegeek.model.Game.Results;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggContract.Categories;
import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.GamePollResults;
import com.boardgamegeek.provider.BggContract.GamePollResultsResult;
import com.boardgamegeek.provider.BggContract.GamePolls;
import com.boardgamegeek.provider.BggContract.GameRanks;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.GamesExpansions;
import com.boardgamegeek.provider.BggContract.Mechanics;
import com.boardgamegeek.provider.BggContract.Publishers;
import com.boardgamegeek.provider.BggDatabase.GamesArtists;
import com.boardgamegeek.provider.BggDatabase.GamesCategories;
import com.boardgamegeek.provider.BggDatabase.GamesDesigners;
import com.boardgamegeek.provider.BggDatabase.GamesMechanics;
import com.boardgamegeek.provider.BggDatabase.GamesPublishers;
import com.boardgamegeek.util.DataUtils;
import com.boardgamegeek.util.NotificationUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.ResolverUtils;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class GamePersister {
	private Context mContext;
	private ContentResolver mResolver;
	private long mUpdateTime;
	private List<Integer> mGameIds;

	public GamePersister(Context context) {
		mContext = context;
		mResolver = context.getContentResolver();
		mUpdateTime = System.currentTimeMillis();
		mGameIds = new ArrayList<>();
	}

	public int save(Game game) {
		return save(game, null);
	}

	public int save(Game game, String debugMessage) {
		List<Game> games = new ArrayList<>(1);
		games.add(game);
		return save(games, debugMessage);
	}

	public int save(List<Game> games) {
		return save(games, null);
	}

	public int save(List<Game> games, String debugMessage) {
		boolean debug = PreferencesUtils.getAvoidBatching(mContext);
		int length = 0;
		ArrayList<ContentProviderOperation> batch = new ArrayList<>();
		if (games != null) {

			DesignerPersister designerPersister = new DesignerPersister();
			ArtistPersister artistPersister = new ArtistPersister();
			PublisherPersister publisherPersister = new PublisherPersister();
			CategoryPersister categoryPersister = new CategoryPersister();
			MechanicPersister mechanicPersister = new MechanicPersister();
			ExpansionPersister expansionPersister = new ExpansionPersister();

			for (Game game : games) {
				if (mGameIds.contains(game.id)) {
					continue;
				}
				mGameIds.add(game.id);
				Builder cpo;
				ContentValues values = toValues(game, mUpdateTime);
				if (ResolverUtils.rowExists(mResolver, Games.buildGameUri(game.id))) {
					values.remove(Games.GAME_ID);
					cpo = ContentProviderOperation.newUpdate(Games.buildGameUri(game.id));
				} else {
					cpo = ContentProviderOperation.newInsert(Games.CONTENT_URI);
				}
				batch.add(cpo.withValues(values).build());
				batch.addAll(ranks(game));
				batch.addAll(polls(game));
				batch.addAll(designerPersister.insertAndCreateAssociations(game.id, mResolver, game.getDesigners()));
				batch.addAll(artistPersister.insertAndCreateAssociations(game.id, mResolver, game.getArtists()));
				batch.addAll(publisherPersister.insertAndCreateAssociations(game.id, mResolver, game.getPublishers()));
				batch.addAll(categoryPersister.insertAndCreateAssociations(game.id, mResolver, game.getCategories()));
				batch.addAll(mechanicPersister.insertAndCreateAssociations(game.id, mResolver, game.getMechanics()));
				batch.addAll(expansionPersister.insertAndCreateAssociations(game.id, mResolver, game.getExpansions()));
				// make sure the last operation has a yield allowed
				batch.add(ContentProviderOperation.newUpdate(Games.buildGameUri(game.id))
					.withValue(Games.UPDATED, mUpdateTime).withYieldAllowed(true).build());
				if (debug) {
					try {
						length += ResolverUtils.applyBatch(mContext, batch, debugMessage).length;
						Timber.i("Saved game ID=" + game.id);
					} catch (Exception e) {
						NotificationCompat.Builder builder = NotificationUtils
							.createNotificationBuilder(mContext, R.string.sync_notification_title)
							.setContentText(e.getMessage())
							.setCategory(NotificationCompat.CATEGORY_ERROR)
							.setStyle(
								new NotificationCompat.BigTextStyle().bigText(e.toString()).setSummaryText(
									e.getMessage()));
						NotificationUtils.notify(mContext, NotificationUtils.ID_PERSIST_ERROR, builder);
					} finally {
						batch.clear();
					}
				}
			}
			if (debug) {
				return length;
			} else {
				ContentProviderResult[] result = ResolverUtils.applyBatch(mContext, batch, debugMessage);
				return result.length;
			}
		}
		return 0;
	}

	private static ContentValues toValues(Game game, long updateTime) {
		ContentValues values = new ContentValues();
		values.put(Games.UPDATED, updateTime);
		values.put(Games.UPDATED_LIST, updateTime);
		values.put(Games.GAME_ID, game.id);
		values.put(Games.GAME_NAME, game.getName());
		values.put(Games.GAME_SORT_NAME, game.getSortName());
		values.put(Games.THUMBNAIL_URL, game.thumbnail);
		values.put(Games.IMAGE_URL, game.image);
		values.put(Games.DESCRIPTION, game.getDescription());
		values.put(Games.SUBTYPE, game.subtype());
		values.put(Games.YEAR_PUBLISHED, game.getYearPublished());
		values.put(Games.MIN_PLAYERS, game.getMinPlayers());
		values.put(Games.MAX_PLAYERS, game.getMaxPlayers());
		values.put(Games.PLAYING_TIME, game.getPlayingTime());
		values.put(Games.MINIMUM_AGE, game.getMinAge());
		values.put(Games.STATS_USERS_RATED, game.statistics.usersRated());
		values.put(Games.STATS_AVERAGE, game.statistics.average());
		values.put(Games.STATS_BAYES_AVERAGE, game.statistics.bayesAverage());
		values.put(Games.STATS_STANDARD_DEVIATION, game.statistics.standardDeviation());
		values.put(Games.STATS_MEDIAN, game.statistics.median());
		values.put(Games.STATS_NUMBER_OWNED, game.statistics.owned());
		values.put(Games.STATS_NUMBER_TRADING, game.statistics.trading());
		values.put(Games.STATS_NUMBER_WANTING, game.statistics.wanting());
		values.put(Games.STATS_NUMBER_WISHING, game.statistics.wishing());
		values.put(Games.STATS_NUMBER_COMMENTS, game.statistics.commenting());
		values.put(Games.STATS_NUMBER_WEIGHTS, game.statistics.weighting());
		values.put(Games.STATS_AVERAGE_WEIGHT, game.statistics.averageWeight());
		values.put(Games.GAME_RANK, game.getRank());
		return values;
	}

	private ArrayList<ContentProviderOperation> polls(Game game) {
		ArrayList<ContentProviderOperation> batch = new ArrayList<>();
		List<String> existingPollNames = ResolverUtils.queryStrings(mResolver, Games.buildPollsUri(game.id),
			GamePolls.POLL_NAME);
		if (game.polls != null) {
			for (Poll poll : game.polls) {
				ContentValues values = new ContentValues();
				values.put(GamePolls.POLL_TITLE, poll.title);
				values.put(GamePolls.POLL_TOTAL_VOTES, poll.totalvotes);

				List<String> existingResultKeys = new ArrayList<>();
				if (existingPollNames.remove(poll.name)) {
					batch.add(ContentProviderOperation.newUpdate(Games.buildPollsUri(game.id, poll.name))
						.withValues(values).build());
					existingResultKeys = ResolverUtils.queryStrings(mResolver,
						Games.buildPollResultsUri(game.id, poll.name), GamePollResults.POLL_RESULTS_PLAYERS);
				} else {
					values.put(GamePolls.POLL_NAME, poll.name);
					batch.add(ContentProviderOperation.newInsert(Games.buildPollsUri(game.id)).withValues(values)
						.build());
				}

				int resultsIndex = 0;
				for (Results results : poll.results) {
					values.clear();
					values.put(GamePollResults.POLL_RESULTS_SORT_INDEX, ++resultsIndex);

					List<String> existingValues = new ArrayList<>();
					if (existingResultKeys.remove(results.getKey())) {
						batch.add(ContentProviderOperation
							.newUpdate(Games.buildPollResultsUri(game.id, poll.name, results.getKey()))
							.withValues(values).build());
						existingValues = ResolverUtils.queryStrings(mResolver,
							Games.buildPollResultsResultUri(game.id, poll.name, results.getKey()),
							GamePollResultsResult.POLL_RESULTS_RESULT_KEY);
					} else {
						values.put(GamePollResults.POLL_RESULTS_PLAYERS, results.getKey());
						batch.add(ContentProviderOperation.newInsert(Games.buildPollResultsUri(game.id, poll.name))
							.withValues(values).build());
					}

					int resultSortIndex = 0;
					for (Result result : results.result) {
						values.clear();
						int level = result.level;
						if (level > 0) {
							values.put(GamePollResultsResult.POLL_RESULTS_RESULT_LEVEL, level);
						}
						values.put(GamePollResultsResult.POLL_RESULTS_RESULT_VALUE, result.value);
						values.put(GamePollResultsResult.POLL_RESULTS_RESULT_VOTES, result.numvotes);
						values.put(GamePollResultsResult.POLL_RESULTS_RESULT_SORT_INDEX, ++resultSortIndex);

						String key = DataUtils.generatePollResultsKey(level, result.value);
						if (existingValues.remove(key)) {
							batch.add(ContentProviderOperation.newUpdate(
								Games.buildPollResultsResultUri(game.id, poll.name, results.getKey(), key)).withValues(values).build());
						} else {
							batch.add(ContentProviderOperation
								.newInsert(Games.buildPollResultsResultUri(game.id, poll.name, results.getKey()))
								.withValues(values).build());
						}
					}

					for (String value : existingValues) {
						batch.add(ContentProviderOperation.newDelete(
							Games.buildPollResultsResultUri(game.id, poll.name, results.getKey(), value)).build());
					}
				}

				for (String player : existingResultKeys) {
					batch.add(ContentProviderOperation.newDelete(Games.buildPollResultsUri(game.id, poll.name, player))
						.build());
				}
			}
		}
		for (String pollName : existingPollNames) {
			batch.add(ContentProviderOperation.newDelete(Games.buildPollsUri(game.id, pollName)).build());
		}
		return batch;
	}

	private ArrayList<ContentProviderOperation> ranks(Game game) {
		ArrayList<ContentProviderOperation> batch = new ArrayList<>();
		List<Integer> rankIds = ResolverUtils.queryInts(mResolver, GameRanks.CONTENT_URI, GameRanks.GAME_RANK_ID,
			GameRanks.GAME_ID + "=?", new String[] { String.valueOf(game.id) });

		ContentValues values = new ContentValues();
		for (Rank rank : game.statistics.ranks) {
			values.clear();
			values.put(GameRanks.GAME_RANK_TYPE, rank.type);
			values.put(GameRanks.GAME_RANK_NAME, rank.name);
			values.put(GameRanks.GAME_RANK_FRIENDLY_NAME, rank.friendlyName);
			values.put(GameRanks.GAME_RANK_VALUE, rank.getValue());
			values.put(GameRanks.GAME_RANK_BAYES_AVERAGE, rank.getBayesAverage());

			Integer rankId = rank.id;
			if (rankIds.remove(rankId)) {
				batch.add(ContentProviderOperation.newUpdate(Games.buildRanksUri(game.id, rankId)).withValues(values)
					.build());
			} else {
				values.put(GameRanks.GAME_RANK_ID, rank.id);
				batch.add(ContentProviderOperation.newInsert(Games.buildRanksUri(game.id)).withValues(values).build());
			}
		}
		for (Integer rankId : rankIds) {
			batch.add(ContentProviderOperation.newDelete(GameRanks.buildGameRankUri(rankId)).build());
		}
		return batch;
	}

	static class DesignerPersister extends LinkPersister {
		@Override
		protected Uri getContentUri() {
			return Designers.CONTENT_URI;
		}

		@Override
		protected String getUriPath() {
			return BggContract.PATH_DESIGNERS;
		}

		@Override
		protected String getReferenceIdColumnName() {
			return Designers.DESIGNER_ID;
		}

		@Override
		protected String getReferenceNameColumnName() {
			return Designers.DESIGNER_NAME;
		}

		@Override
		protected String getAssociationIdColumnName() {
			return GamesDesigners.DESIGNER_ID;
		}

		@Override
		protected String getAssociationNameColumnName() {
			return null;
		}

		@Override
		protected String getInboundColumnName() {
			return null;
		}
	}

	static class ArtistPersister extends LinkPersister {
		@Override
		protected Uri getContentUri() {
			return Artists.CONTENT_URI;
		}

		@Override
		protected String getUriPath() {
			return BggContract.PATH_ARTISTS;
		}

		@Override
		protected String getReferenceIdColumnName() {
			return Artists.ARTIST_ID;
		}

		@Override
		protected String getReferenceNameColumnName() {
			return Artists.ARTIST_NAME;
		}

		@Override
		protected String getAssociationIdColumnName() {
			return GamesArtists.ARTIST_ID;
		}

		@Override
		protected String getAssociationNameColumnName() {
			return null;
		}

		@Override
		protected String getInboundColumnName() {
			return null;
		}
	}

	static class PublisherPersister extends LinkPersister {
		@Override
		protected Uri getContentUri() {
			return Publishers.CONTENT_URI;
		}

		@Override
		protected String getUriPath() {
			return BggContract.PATH_PUBLISHERS;
		}

		@Override
		protected String getReferenceIdColumnName() {
			return Publishers.PUBLISHER_ID;
		}

		@Override
		protected String getReferenceNameColumnName() {
			return Publishers.PUBLISHER_NAME;
		}

		@Override
		protected String getAssociationIdColumnName() {
			return GamesPublishers.PUBLISHER_ID;
		}

		@Override
		protected String getAssociationNameColumnName() {
			return null;
		}

		@Override
		protected String getInboundColumnName() {
			return null;
		}
	}

	static class CategoryPersister extends LinkPersister {
		@Override
		protected Uri getContentUri() {
			return Categories.CONTENT_URI;
		}

		@Override
		protected String getUriPath() {
			return BggContract.PATH_CATEGORIES;
		}

		@Override
		protected String getReferenceIdColumnName() {
			return Categories.CATEGORY_ID;
		}

		@Override
		protected String getReferenceNameColumnName() {
			return Categories.CATEGORY_NAME;
		}

		@Override
		protected String getAssociationIdColumnName() {
			return GamesCategories.CATEGORY_ID;
		}

		@Override
		protected String getAssociationNameColumnName() {
			return null;
		}

		@Override
		protected String getInboundColumnName() {
			return null;
		}
	}

	static class MechanicPersister extends LinkPersister {
		@Override
		protected Uri getContentUri() {
			return Mechanics.CONTENT_URI;
		}

		@Override
		protected String getUriPath() {
			return BggContract.PATH_MECHANICS;
		}

		@Override
		protected String getReferenceIdColumnName() {
			return Mechanics.MECHANIC_ID;
		}

		@Override
		protected String getReferenceNameColumnName() {
			return Mechanics.MECHANIC_NAME;
		}

		@Override
		protected String getAssociationIdColumnName() {
			return GamesMechanics.MECHANIC_ID;
		}

		@Override
		protected String getAssociationNameColumnName() {
			return null;
		}

		@Override
		protected String getInboundColumnName() {
			return null;
		}
	}

	static class ExpansionPersister extends LinkPersister {
		@Override
		protected Uri getContentUri() {
			return Games.CONTENT_URI;
		}

		@Override
		protected String getUriPath() {
			return BggContract.PATH_EXPANSIONS;
		}

		@Override
		protected String getReferenceIdColumnName() {
			return null;
		}

		@Override
		protected String getReferenceNameColumnName() {
			return null;
		}

		@Override
		protected String getAssociationIdColumnName() {
			return GamesExpansions.EXPANSION_ID;
		}

		@Override
		protected String getAssociationNameColumnName() {
			return GamesExpansions.EXPANSION_NAME;
		}

		@Override
		protected String getInboundColumnName() {
			return GamesExpansions.INBOUND;
		}
	}

	static abstract class LinkPersister {
		protected abstract Uri getContentUri();

		protected abstract String getUriPath();

		protected abstract String getReferenceIdColumnName();

		protected abstract String getReferenceNameColumnName();

		protected abstract String getAssociationIdColumnName();

		protected abstract String getAssociationNameColumnName();

		protected abstract String getInboundColumnName();

		ArrayList<ContentProviderOperation> insertAndCreateAssociations(int gameId, ContentResolver resolver,
																		List<Game.Link> newLinks) {
			ArrayList<ContentProviderOperation> batch = new ArrayList<>();
			Uri gameUri = Games.buildPathUri(gameId, getUriPath());
			List<Integer> existingIds = ResolverUtils.queryInts(resolver, gameUri, getAssociationIdColumnName());

			for (Game.Link newLink : newLinks) {
				if (!existingIds.remove(Integer.valueOf(newLink.id))) {
					// insert reference row, if missing
					if (!TextUtils.isEmpty(getReferenceIdColumnName())
						&& !ResolverUtils.rowExists(resolver,
						getContentUri().buildUpon().appendPath(String.valueOf(newLink.id)).build())) {
						// XXX think about delaying inserts in a separate batch
						ContentValues cv = new ContentValues();
						cv.put(getReferenceIdColumnName(), newLink.id);
						cv.put(getReferenceNameColumnName(), newLink.value);
						resolver.insert(getContentUri(), cv);
						// TODO else update?
					}
					// insert association row
					Builder cpo = ContentProviderOperation.newInsert(gameUri).withValue(getAssociationIdColumnName(),
						newLink.id);
					if (!TextUtils.isEmpty(getAssociationNameColumnName())) {
						cpo.withValue(getAssociationNameColumnName(), newLink.value);
					}
					if (!TextUtils.isEmpty(getInboundColumnName())) {
						cpo.withValue(getInboundColumnName(), newLink.getInbound());
					}
					batch.add(cpo.build());
				}
			}
			// remove unused associations
			for (Integer existingId : existingIds) {
				Uri uri = Games.buildPathUri(gameId, getUriPath(), existingId);
				batch.add(ContentProviderOperation.newDelete(uri).build());
			}
			return batch;
		}
	}
}
