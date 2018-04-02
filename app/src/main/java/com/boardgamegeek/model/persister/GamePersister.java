package com.boardgamegeek.model.persister;

import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.boardgamegeek.entities.GameEntity;
import com.boardgamegeek.mappers.GameMapper;
import com.boardgamegeek.model.Game;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggContract.Categories;
import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.GamePollResults;
import com.boardgamegeek.provider.BggContract.GamePollResultsResult;
import com.boardgamegeek.provider.BggContract.GamePolls;
import com.boardgamegeek.provider.BggContract.GameRanks;
import com.boardgamegeek.provider.BggContract.GameSuggestedPlayerCountPollPollResults;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.GamesExpansions;
import com.boardgamegeek.provider.BggContract.Mechanics;
import com.boardgamegeek.provider.BggContract.Publishers;
import com.boardgamegeek.provider.BggDatabase.GamesArtists;
import com.boardgamegeek.provider.BggDatabase.GamesCategories;
import com.boardgamegeek.provider.BggDatabase.GamesDesigners;
import com.boardgamegeek.provider.BggDatabase.GamesMechanics;
import com.boardgamegeek.provider.BggDatabase.GamesPublishers;
import com.boardgamegeek.util.CursorUtils;
import com.boardgamegeek.util.DataUtils;
import com.boardgamegeek.util.NotificationUtils;
import com.boardgamegeek.util.PlayerCountRecommendation;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.ResolverUtils;

import java.util.ArrayList;
import java.util.List;

import kotlin.Pair;
import kotlin.Triple;
import timber.log.Timber;

public class GamePersister {
	private final Context context;
	private final ContentResolver resolver;
	private final long updateTime;
	private final List<Integer> gameIds;

	public GamePersister(Context context) {
		this.context = context;
		resolver = context.getContentResolver();
		updateTime = System.currentTimeMillis();
		gameIds = new ArrayList<>();
	}

	public int save(List<Game> games, String debugMessage) {
		boolean avoidBatching = PreferencesUtils.getAvoidBatching(context);
		int recordCount = 0;
		ArrayList<ContentProviderOperation> batch = new ArrayList<>();
		if (games != null && games.size() > 0) {

			DesignerPersister designerPersister = new DesignerPersister();
			ArtistPersister artistPersister = new ArtistPersister();
			PublisherPersister publisherPersister = new PublisherPersister();
			CategoryPersister categoryPersister = new CategoryPersister();
			MechanicPersister mechanicPersister = new MechanicPersister();
			ExpansionPersister expansionPersister = new ExpansionPersister();

			for (Game g : games) {
				GameEntity game = new GameMapper().map(g, updateTime);

				if (TextUtils.isEmpty(game.getName())) {
					Timber.w("Missing name from game ID=%s", game.getId());
					continue;
				}

				if (gameIds.contains(game.getId())) {
					Timber.i("Skipping duplicate game ID=%s", game.getId());
					continue;
				}
				gameIds.add(game.getId());

				Timber.i("Saving game %s (%s)", game.getName(), game.getId());

				Builder cpoBuilder;
				ContentValues values = toValues(game, updateTime);
				if (ResolverUtils.rowExists(resolver, Games.buildGameUri(game.getId()))) {
					values.remove(Games.GAME_ID);
					if (shouldClearHeroImageUrl(game)) {
						values.put(Games.HERO_IMAGE_URL, "");
					}
					cpoBuilder = ContentProviderOperation.newUpdate(Games.buildGameUri(game.getId()));
				} else {
					cpoBuilder = ContentProviderOperation.newInsert(Games.CONTENT_URI);
				}

				ArrayList<ContentProviderOperation> rankOperations = createRanksBatch(game);
				ArrayList<ContentProviderOperation> pollOperations = createPollsBatch(game, values);

				batch.add(cpoBuilder.withValues(values).withYieldAllowed(true).build());
				batch.addAll(rankOperations);
				batch.addAll(pollOperations);
				batch.addAll(designerPersister.createBatch(game.getId(), resolver, game.getDesigners()));
				batch.addAll(artistPersister.createBatch(game.getId(), resolver, game.getArtists()));
				batch.addAll(publisherPersister.createBatch(game.getId(), resolver, game.getPublishers()));
				batch.addAll(categoryPersister.createBatch(game.getId(), resolver, game.getCategories()));
				batch.addAll(mechanicPersister.createBatch(game.getId(), resolver, game.getMechanics()));
				batch.addAll(expansionPersister.createBatch(game.getId(), resolver, game.getExpansions()));

				if (avoidBatching) {
					try {
						int count = ResolverUtils.applyBatch(context, batch, debugMessage).length;
						Timber.i("Saved game ID '%s' in %,d records", game.getId(), count);
						recordCount += count;
					} catch (Exception e) {
						NotificationUtils.showPersistErrorNotification(context, e);
					} finally {
						batch.clear();
					}
				}
			}
			if (avoidBatching) {
				return recordCount;
			} else {
				try {
					int count = ResolverUtils.applyBatch(context, batch, debugMessage).length;
					Timber.i("Saved a list of games in %,d records", count);
					return count;
				} catch (Exception e) {
					NotificationUtils.showPersistErrorNotification(context, e);
				}
			}
		}
		return 0;
	}

	private boolean shouldClearHeroImageUrl(GameEntity game) {
		Cursor cursor = resolver.query(Games.buildGameUri(game.getId()), new String[]{Games.IMAGE_URL, Games.THUMBNAIL_URL}, null, null, null);
		try {
			if (cursor != null && cursor.moveToFirst()) {
				String imageUrl = CursorUtils.getString(cursor, 0);
				String thumbnailUrl = cursor.getString(1);
				if (!imageUrl.equals(game.getImageUrl()) ||
					!thumbnailUrl.equals(game.getThumbnailUrl())) {
					return true;
				}
			}
		} finally {
			if (cursor != null) cursor.close();
		}
		return false;
	}

	private static ContentValues toValues(GameEntity game, long updateTime) {
		ContentValues values = new ContentValues();
		values.put(Games.UPDATED, updateTime);
		values.put(Games.UPDATED_LIST, updateTime);
		values.put(Games.GAME_ID, game.getId());
		values.put(Games.GAME_NAME, game.getName());
		values.put(Games.GAME_SORT_NAME, game.getSortName());
		values.put(Games.THUMBNAIL_URL, game.getThumbnailUrl());
		values.put(Games.IMAGE_URL, game.getImageUrl());
		values.put(Games.DESCRIPTION, game.getDescription());
		values.put(Games.SUBTYPE, game.getSubtype());
		values.put(Games.YEAR_PUBLISHED, game.getYearPublished());
		values.put(Games.MIN_PLAYERS, game.getMinPlayers());
		values.put(Games.MAX_PLAYERS, game.getMaxPlayers());
		values.put(Games.PLAYING_TIME, game.getPlayingTime());
		values.put(Games.MIN_PLAYING_TIME, game.getMinPlayingTime());
		values.put(Games.MAX_PLAYING_TIME, game.getMaxPlayingTime());
		values.put(Games.MINIMUM_AGE, game.getMinAge());
		if (game.getHasStatistics()) {
			values.put(Games.STATS_AVERAGE, game.getAverage());
			values.put(Games.STATS_BAYES_AVERAGE, game.getBayesAverage());
			values.put(Games.STATS_STANDARD_DEVIATION, game.getStandardDeviation());
			values.put(Games.STATS_MEDIAN, game.getMedian());
			values.put(Games.STATS_USERS_RATED, game.getNumberOfRatings());
			values.put(Games.STATS_NUMBER_OWNED, game.getNumberOfUsersOwned());
			values.put(Games.STATS_NUMBER_TRADING, game.getNumberOfUsersTrading());
			values.put(Games.STATS_NUMBER_WANTING, game.getNumberOfUsersWanting());
			values.put(Games.STATS_NUMBER_WISHING, game.getNumberOfUsersWishListing());
			values.put(Games.STATS_NUMBER_COMMENTS, game.getNumberOfComments());
			values.put(Games.STATS_NUMBER_WEIGHTS, game.getNumberOfUsersWeighting());
			values.put(Games.STATS_AVERAGE_WEIGHT, game.getAverageWeight());
		}
		values.put(Games.GAME_RANK, game.getOverallRank());
		return values;
	}

	private ArrayList<ContentProviderOperation> createPollsBatch(GameEntity game, ContentValues gameContentValues) {
		ArrayList<ContentProviderOperation> batch = new ArrayList<>();
		List<String> existingPollNames = ResolverUtils.queryStrings(resolver, Games.buildPollsUri(game.getId()), GamePolls.POLL_NAME);
		for (GameEntity.Poll poll : game.getPolls()) {
			if ("suggested_numplayers".equals(poll.getName())) {
				gameContentValues.put(Games.SUGGESTED_PLAYER_COUNT_POLL_VOTE_TOTAL, poll.getTotalVotes());
				int sortIndex = 0;
				List<String> existingResults = ResolverUtils.queryStrings(resolver,
					Games.buildSuggestedPlayerCountPollResultsUri(game.getId()), GameSuggestedPlayerCountPollPollResults.PLAYER_COUNT);
				for (GameEntity.Results results : poll.getResults()) {
					ContentValues values = new ContentValues(6);
					PlayerCountRecommendation.Builder builder = new PlayerCountRecommendation.Builder();
					values.put(GameSuggestedPlayerCountPollPollResults.SORT_INDEX, ++sortIndex);
					for (GameEntity.Result result : results.getResult()) {
						if ("Best".equals(result.getValue())) {
							values.put(GameSuggestedPlayerCountPollPollResults.BEST_VOTE_COUNT, result.getNumberOfVotes());
							builder.bestVoteCount(result.getNumberOfVotes());
						} else if ("Recommended".equals(result.getValue())) {
							values.put(GameSuggestedPlayerCountPollPollResults.RECOMMENDED_VOTE_COUNT, result.getNumberOfVotes());
							builder.recommendedVoteCount(result.getNumberOfVotes());
						} else if ("Not Recommended".equals(result.getValue())) {
							values.put(GameSuggestedPlayerCountPollPollResults.NOT_RECOMMENDED_VOTE_COUNT, result.getNumberOfVotes());
							builder.notRecommendVoteCount(result.getNumberOfVotes());
						} else {
							Timber.i("Unexpected suggested player count result of '%s'", result.getValue());
						}
					}
					values.put(GameSuggestedPlayerCountPollPollResults.RECOMMENDATION, builder.build().calculate());
					if (existingResults.remove(results.getKey())) {
						Uri uri = Games.buildSuggestedPlayerCountPollResultsUri(game.getId(), results.getKey());
						batch.add(ContentProviderOperation.newUpdate(uri).withValues(values).build());
					} else {
						values.put(GameSuggestedPlayerCountPollPollResults.PLAYER_COUNT, results.getKey());
						Uri uri = Games.buildSuggestedPlayerCountPollResultsUri(game.getId());
						batch.add(ContentProviderOperation.newInsert(uri).withValues(values).build());
					}
				}
				for (String result : existingResults) {
					Uri uri = Games.buildSuggestedPlayerCountPollResultsUri(game.getId(), result);
					batch.add(ContentProviderOperation.newDelete(uri).build());
				}
			} else {
				ContentValues values = new ContentValues();
				values.put(GamePolls.POLL_TITLE, poll.getTitle());
				values.put(GamePolls.POLL_TOTAL_VOTES, poll.getTotalVotes());

				List<String> existingResultKeys = new ArrayList<>();
				if (existingPollNames.remove(poll.getName())) {
					batch.add(ContentProviderOperation.newUpdate(Games.buildPollsUri(game.getId(), poll.getName())).withValues(values).build());
					existingResultKeys = ResolverUtils.queryStrings(resolver, Games.buildPollResultsUri(game.getId(), poll.getName()), GamePollResults.POLL_RESULTS_PLAYERS);
				} else {
					values.put(GamePolls.POLL_NAME, poll.getName());
					batch.add(ContentProviderOperation.newInsert(Games.buildPollsUri(game.getId())).withValues(values).build());
				}

				int resultsIndex = 0;
				for (GameEntity.Results results : poll.getResults()) {
					values.clear();
					values.put(GamePollResults.POLL_RESULTS_SORT_INDEX, ++resultsIndex);

					List<String> existingValues = new ArrayList<>();
					if (existingResultKeys.remove(results.getKey())) {
						batch.add(ContentProviderOperation
							.newUpdate(Games.buildPollResultsUri(game.getId(), poll.getName(), results.getKey()))
							.withValues(values).build());
						existingValues = ResolverUtils.queryStrings(resolver,
							Games.buildPollResultsResultUri(game.getId(), poll.getName(), results.getKey()),
							GamePollResultsResult.POLL_RESULTS_RESULT_KEY);
					} else {
						values.put(GamePollResults.POLL_RESULTS_PLAYERS, results.getKey());
						batch.add(ContentProviderOperation.newInsert(Games.buildPollResultsUri(game.getId(), poll.getName())).withValues(values).build());
					}

					int resultSortIndex = 0;
					for (GameEntity.Result result : results.getResult()) {
						values.clear();
						if (result.getLevel() > 0)
							values.put(GamePollResultsResult.POLL_RESULTS_RESULT_LEVEL, result.getLevel());
						values.put(GamePollResultsResult.POLL_RESULTS_RESULT_VALUE, result.getValue());
						values.put(GamePollResultsResult.POLL_RESULTS_RESULT_VOTES, result.getNumberOfVotes());
						values.put(GamePollResultsResult.POLL_RESULTS_RESULT_SORT_INDEX, ++resultSortIndex);

						String key = DataUtils.generatePollResultsKey(result.getLevel(), result.getValue());
						if (existingValues.remove(key)) {
							batch.add(ContentProviderOperation.newUpdate(Games.buildPollResultsResultUri(game.getId(), poll.getName(), results.getKey(), key))
								.withValues(values)
								.build());
						} else {
							batch.add(ContentProviderOperation
								.newInsert(Games.buildPollResultsResultUri(game.getId(), poll.getName(), results.getKey()))
								.withValues(values)
								.build());
						}
					}

					for (String value : existingValues) {
						batch.add(ContentProviderOperation.newDelete(Games.buildPollResultsResultUri(game.getId(), poll.getName(), results.getKey(), value)).build());
					}
				}

				for (String player : existingResultKeys) {
					batch.add(ContentProviderOperation.newDelete(Games.buildPollResultsUri(game.getId(), poll.getName(), player)).build());
				}
			}
		}
		for (String pollName : existingPollNames) {
			batch.add(ContentProviderOperation.newDelete(Games.buildPollsUri(game.getId(), pollName)).build());
		}
		return batch;
	}

	private ArrayList<ContentProviderOperation> createRanksBatch(GameEntity game) {
		ArrayList<ContentProviderOperation> batch = new ArrayList<>();
		List<Integer> existingRankIds = ResolverUtils.queryInts(resolver,
			GameRanks.CONTENT_URI,
			GameRanks.GAME_RANK_ID,
			GameRanks.GAME_ID + "=?",
			new String[]{String.valueOf(game.getId())});

		ContentValues values = new ContentValues();
		for (GameEntity.Rank rank : game.getRanks()) {
			values.clear();
			values.put(GameRanks.GAME_RANK_TYPE, rank.getType());
			values.put(GameRanks.GAME_RANK_NAME, rank.getName());
			values.put(GameRanks.GAME_RANK_FRIENDLY_NAME, rank.getFriendlyName());
			values.put(GameRanks.GAME_RANK_VALUE, rank.getValue());
			values.put(GameRanks.GAME_RANK_BAYES_AVERAGE, rank.getBayesAverage());

			if (existingRankIds.remove((Integer) rank.getId())) {
				batch.add(ContentProviderOperation.newUpdate(Games.buildRanksUri(game.getId(), rank.getId())).withValues(values).build());
			} else {
				values.put(GameRanks.GAME_RANK_ID, rank.getId());
				batch.add(ContentProviderOperation.newInsert(Games.buildRanksUri(game.getId())).withValues(values).build());
			}
		}
		for (int rankId : existingRankIds) {
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
	}

	static class ExpansionPersister {
		protected Uri getContentUri() {
			return Games.CONTENT_URI;
		}

		protected String getUriPath() {
			return BggContract.PATH_EXPANSIONS;
		}

		protected String getReferenceIdColumnName() {
			return null;
		}

		protected String getReferenceNameColumnName() {
			return null;
		}

		protected String getAssociationIdColumnName() {
			return GamesExpansions.EXPANSION_ID;
		}

		protected String getAssociationNameColumnName() {
			return GamesExpansions.EXPANSION_NAME;
		}

		protected String getInboundColumnName() {
			return GamesExpansions.INBOUND;
		}

		ArrayList<ContentProviderOperation> createBatch(int gameId, ContentResolver resolver, List<Triple<Integer, String, Boolean>> newLinks) {
			ArrayList<ContentProviderOperation> batch = new ArrayList<>();
			Uri pathUri = Games.buildPathUri(gameId, getUriPath());
			List<Integer> existingIds = ResolverUtils.queryInts(resolver, pathUri, getAssociationIdColumnName());

			for (Triple<Integer, String, Boolean> newLink : newLinks) {
				if (!existingIds.remove(newLink.getFirst())) {
					if (shouldInsertReferenceRow(resolver, newLink)) {
						ContentValues cv = new ContentValues(2);
						cv.put(getReferenceIdColumnName(), newLink.getFirst());
						cv.put(getReferenceNameColumnName(), newLink.getSecond());
						resolver.insert(getContentUri(), cv);
					} else if (shouldUpdateReferenceRow(resolver, newLink)) {
						ContentValues cv = new ContentValues(1);
						cv.put(getReferenceNameColumnName(), newLink.getSecond());
						resolver.update(buildLinkUri(newLink), cv, null, null);
					}
					// insert association row
					Builder cpoBuilder = ContentProviderOperation.newInsert(pathUri)
						.withValue(getAssociationIdColumnName(), newLink.getFirst());
					if (!TextUtils.isEmpty(getAssociationNameColumnName())) {
						cpoBuilder.withValue(getAssociationNameColumnName(), newLink.getSecond());
					}
					if (!TextUtils.isEmpty(getInboundColumnName())) {
						cpoBuilder.withValue(getInboundColumnName(), newLink.getThird());
					}
					batch.add(cpoBuilder.build());
				}
			}
			// remove unused associations
			for (Integer existingId : existingIds) {
				Uri uri = Games.buildPathUri(gameId, getUriPath(), existingId);
				batch.add(ContentProviderOperation.newDelete(uri).build());
			}
			return batch;
		}

		private boolean shouldInsertReferenceRow(ContentResolver resolver, Triple<Integer, String, Boolean> newLink) {
			return !TextUtils.isEmpty(getReferenceIdColumnName()) &&
				!ResolverUtils.rowExists(resolver, buildLinkUri(newLink));
		}


		private boolean shouldUpdateReferenceRow(ContentResolver resolver, Triple<Integer, String, Boolean> newLink) {
			return !TextUtils.isEmpty(getReferenceIdColumnName()) &&
				ResolverUtils.rowExists(resolver, buildLinkUri(newLink));
		}

		private Uri buildLinkUri(Triple<Integer, String, Boolean> newLink) {
			return getContentUri().buildUpon().appendPath(String.valueOf(newLink.getFirst())).build();
		}
	}

	static abstract class LinkPersister {
		protected abstract Uri getContentUri();

		protected abstract String getUriPath();

		protected abstract String getReferenceIdColumnName();

		protected abstract String getReferenceNameColumnName();

		protected abstract String getAssociationIdColumnName();

		protected abstract String getAssociationNameColumnName();

		ArrayList<ContentProviderOperation> createBatch(int gameId, ContentResolver resolver, List<Pair<Integer, String>> newLinks) {
			ArrayList<ContentProviderOperation> batch = new ArrayList<>();
			Uri pathUri = Games.buildPathUri(gameId, getUriPath());
			List<Integer> existingIds = ResolverUtils.queryInts(resolver, pathUri, getAssociationIdColumnName());

			for (Pair<Integer, String> newLink : newLinks) {
				if (!existingIds.remove(newLink.getFirst())) {
					if (shouldInsertReferenceRow(resolver, newLink)) {
						ContentValues cv = new ContentValues(2);
						cv.put(getReferenceIdColumnName(), newLink.getFirst());
						cv.put(getReferenceNameColumnName(), newLink.getSecond());
						resolver.insert(getContentUri(), cv);
					} else if (shouldUpdateReferenceRow(resolver, newLink)) {
						ContentValues cv = new ContentValues(1);
						cv.put(getReferenceNameColumnName(), newLink.getSecond());
						resolver.update(buildLinkUri(newLink), cv, null, null);
					}
					// insert association row
					Builder cpoBuilder = ContentProviderOperation.newInsert(pathUri)
						.withValue(getAssociationIdColumnName(), newLink.getFirst());
					if (!TextUtils.isEmpty(getAssociationNameColumnName())) {
						cpoBuilder.withValue(getAssociationNameColumnName(), newLink.getSecond());
					}
					batch.add(cpoBuilder.build());
				}
			}
			// remove unused associations
			for (Integer existingId : existingIds) {
				Uri uri = Games.buildPathUri(gameId, getUriPath(), existingId);
				batch.add(ContentProviderOperation.newDelete(uri).build());
			}
			return batch;
		}

		private boolean shouldInsertReferenceRow(ContentResolver resolver, Pair<Integer, String> newLink) {
			return !TextUtils.isEmpty(getReferenceIdColumnName()) &&
				!ResolverUtils.rowExists(resolver, buildLinkUri(newLink));
		}


		private boolean shouldUpdateReferenceRow(ContentResolver resolver, Pair<Integer, String> newLink) {
			return !TextUtils.isEmpty(getReferenceIdColumnName()) &&
				ResolverUtils.rowExists(resolver, buildLinkUri(newLink));
		}

		private Uri buildLinkUri(Pair<Integer, String> newLink) {
			return getContentUri().buildUpon().appendPath(String.valueOf(newLink.getFirst())).build();
		}
	}
}
