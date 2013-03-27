package com.boardgamegeek.io;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;
import static org.xmlpull.v1.XmlPullParser.TEXT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentValues;
import android.net.Uri;

import com.boardgamegeek.database.ResolverUtils;
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
import com.boardgamegeek.util.StringUtils;

public class RemoteGameHandler extends RemoteBggHandler {
	private int mGameId;
	private List<Integer> mGameDesignerIds;
	private List<Integer> mGameArtistIds;
	private List<Integer> mGamePublisherIds;
	private List<Integer> mGameMechanicIds;
	private List<Integer> mGameCategoryIds;
	private List<Integer> mGameExpansionIds;
	private List<String> mPollNames;
	private boolean mParsePolls;
	private int mCount;

	public RemoteGameHandler() {
		super();
	}

	public void setParsePolls() {
		mParsePolls = true;
	}

	@Override
	public int getCount() {
		return mCount;
	}

	@Override
	protected String getRootNodeName() {
		return Tags.BOARDGAMES;
	}

	@Override
	protected void parseItems() throws XmlPullParserException, IOException {
		int type;
		while ((type = mParser.next()) != END_DOCUMENT) {
			if (type == START_TAG && Tags.BOARDGAME.equals(mParser.getName())) {
				mGameId = parseIntegerAttribute(Tags.ID);
				saveChildRecords();
				ContentValues values = parseGame();
				Uri uri = Games.buildGameUri(mGameId);
				if (!ResolverUtils.rowExists(mResolver, uri)) {
					values.put(Games.GAME_ID, mGameId);
					values.put(Games.UPDATED_LIST, System.currentTimeMillis());
					addInsertToTop(Games.CONTENT_URI, values);
				} else {
					addUpdateToTop(uri, values);
				}
				mCount++;
				deleteOldChildRecords();
			}
			processBatch();
		}
	}

	private void saveChildRecords() {
		mGameDesignerIds = ResolverUtils.queryInts(mResolver, Games.buildDesignersUri(mGameId), Designers.DESIGNER_ID);
		mGameArtistIds = ResolverUtils.queryInts(mResolver, Games.buildArtistsUri(mGameId), Artists.ARTIST_ID);
		mGamePublisherIds = ResolverUtils.queryInts(mResolver, Games.buildPublishersUri(mGameId),
			Publishers.PUBLISHER_ID);
		mGameMechanicIds = ResolverUtils.queryInts(mResolver, Games.buildMechanicsUri(mGameId), Mechanics.MECHANIC_ID);
		mGameCategoryIds = ResolverUtils
			.queryInts(mResolver, Games.buildCategoriesUri(mGameId), Categories.CATEGORY_ID);
		mGameExpansionIds = ResolverUtils.queryInts(mResolver, Games.buildExpansionsUri(mGameId),
			GamesExpansions.EXPANSION_ID);
		mPollNames = ResolverUtils.queryStrings(mResolver, Games.buildPollsUri(mGameId), GamePolls.POLL_NAME);
	}

	private void deleteOldChildRecords() {
		for (Integer designerId : mGameDesignerIds) {
			addDelete(Games.buildDesignersUri(mGameId, designerId));
		}
		for (Integer artistId : mGameArtistIds) {
			addDelete(Games.buildArtistsUri(mGameId, artistId));
		}
		for (Integer publisherId : mGamePublisherIds) {
			addDelete(Games.buildPublishersUri(mGameId, publisherId));
		}
		for (Integer mechanicId : mGameMechanicIds) {
			addDelete(Games.buildMechanicsUri(mGameId, mechanicId));
		}
		for (Integer categoryId : mGameCategoryIds) {
			addDelete(Games.buildCategoriesUri(mGameId, categoryId));
		}
		for (Integer expansionId : mGameExpansionIds) {
			addDelete(Games.buildExpansionsUri(mGameId, expansionId));
		}
		for (String pollName : mPollNames) {
			addDelete(Games.buildPollsUri(mGameId, pollName));
		}
	}

	private ContentValues parseGame() throws XmlPullParserException, IOException {

		ContentValues values = new ContentValues();
		String tag = null;
		int sortIndex = 1;

		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == START_TAG) {
				tag = mParser.getName();

				if (Tags.NAME.equals(tag)) {
					sortIndex = parseIntegerAttribute(Tags.SORT_INDEX, 1);
					String primary = parseStringAttribute(Tags.PRIMARY);
					if (!Tags.TRUE.equals(primary)) {
						tag = null;
					}
				} else if (Tags.DESIGNER.equals(tag)) {
					parseDesigner();
					tag = null;
				} else if (Tags.ARTIST.equals(tag)) {
					parseArtist();
					tag = null;
				} else if (Tags.PUBLISHER.equals(tag)) {
					parsePublisher();
					tag = null;
				} else if (Tags.MECHANIC.equals(tag)) {
					parseMechanic();
					tag = null;
				} else if (Tags.CATEGORY.equals(tag)) {
					parseCategory();
					tag = null;
				} else if (Tags.POLL.equals(tag)) {
					if (mParsePolls) {
						parsePoll();
					}
					tag = null;
				} else if (Tags.EXPANSION.equals(tag)) {
					parseExpansion();
					tag = null;
				}
			} else if (type == END_TAG) {
				tag = null;
			} else if (type == TEXT) {
				String text = mParser.getText();

				if (Tags.YEAR_PUBLISHED.equals(tag)) {
					values.put(Games.YEAR_PUBLISHED, text);
				} else if (Tags.MIN_PLAYERS.equals(tag)) {
					values.put(Games.MIN_PLAYERS, StringUtils.parseInt(text));
				} else if (Tags.MAX_PLAYERS.equals(tag)) {
					values.put(Games.MAX_PLAYERS, StringUtils.parseInt(text));
				} else if (Tags.PLAYING_TIME.equals(tag)) {
					values.put(Games.PLAYING_TIME, StringUtils.parseInt(text));
				} else if (Tags.AGE.equals(tag)) {
					values.put(Games.MINIMUM_AGE, StringUtils.parseInt(text));
				} else if (Tags.NAME.equals(tag)) {
					values.put(Games.GAME_NAME, text);
					values.put(Games.GAME_SORT_NAME, StringUtils.createSortName(text, sortIndex));
				} else if (Tags.DESCRIPTION.equals(tag)) {
					values.put(Games.DESCRIPTION, text);
				} else if (Tags.THUMBNAIL.equals(tag)) {
					values.put(Games.THUMBNAIL_URL, text);
				} else if (Tags.IMAGE.equals(tag)) {
					values.put(Games.IMAGE_URL, text);
				} else if (Tags.STATISTICS.equals(tag)) {
					parseStats(values);
					tag = null;
				}
			}
		}

		values.put(Games.UPDATED, System.currentTimeMillis());
		return values;
	}

	private void parseDesigner() throws XmlPullParserException, IOException {
		ContentValues values = new ContentValues();
		final int designerId = parseIntegerAttribute(Tags.ID);
		values.put(Designers.DESIGNER_ID, designerId);

		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == TEXT) {
				values.put(Designers.DESIGNER_NAME, mParser.getText());
			}
		}

		if (!mGameDesignerIds.remove(Integer.valueOf(designerId))) {
			insertMaybe(Designers.CONTENT_URI, values, designerId);
			addInsert(Games.buildDesignersUri(mGameId), GamesDesigners.DESIGNER_ID, designerId);
		}
	}

	private void parseArtist() throws XmlPullParserException, IOException {
		ContentValues values = new ContentValues();
		final int artistId = parseIntegerAttribute(Tags.ID);
		values.put(Artists.ARTIST_ID, artistId);

		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == TEXT) {
				values.put(Artists.ARTIST_NAME, mParser.getText());
			}
		}

		if (!mGameArtistIds.remove(Integer.valueOf(artistId))) {
			insertMaybe(Artists.CONTENT_URI, values, artistId);
			addInsert(Games.buildArtistsUri(mGameId), GamesArtists.ARTIST_ID, artistId);
		}
	}

	private void parsePublisher() throws XmlPullParserException, IOException {
		ContentValues values = new ContentValues();
		final int publisherId = parseIntegerAttribute(Tags.ID);
		values.put(Publishers.PUBLISHER_ID, publisherId);

		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == TEXT) {
				values.put(Publishers.PUBLISHER_NAME, mParser.getText());
			}
		}

		if (!mGamePublisherIds.remove(Integer.valueOf(publisherId))) {
			insertMaybe(Publishers.CONTENT_URI, values, publisherId);
			addInsert(Games.buildPublishersUri(mGameId), GamesPublishers.PUBLISHER_ID, publisherId);
		}
	}

	private void parseMechanic() throws XmlPullParserException, IOException {
		ContentValues values = new ContentValues();
		final int mechanicId = parseIntegerAttribute(Tags.ID);
		values.put(Mechanics.MECHANIC_ID, mechanicId);

		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == TEXT) {
				values.put(Mechanics.MECHANIC_NAME, mParser.getText());
			}
		}

		if (!mGameMechanicIds.remove(Integer.valueOf(mechanicId))) {
			insertMaybe(Mechanics.CONTENT_URI, values, mechanicId);
			addInsert(Games.buildMechanicsUri(mGameId), GamesMechanics.MECHANIC_ID, mechanicId);
		}
	}

	private void parseCategory() throws XmlPullParserException, IOException {
		ContentValues values = new ContentValues();
		final int categoryId = parseIntegerAttribute(Tags.ID);
		values.put(Categories.CATEGORY_ID, categoryId);

		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == TEXT) {
				values.put(Categories.CATEGORY_NAME, mParser.getText());
			}
		}

		if (!mGameCategoryIds.remove(Integer.valueOf(categoryId))) {
			insertMaybe(Categories.CONTENT_URI, values, categoryId);
			addInsert(Games.buildCategoriesUri(mGameId), GamesCategories.CATEGORY_ID, categoryId);
		}
	}

	private void parseExpansion() throws XmlPullParserException, IOException {
		ContentValues values = new ContentValues();
		final int expansionId = parseIntegerAttribute(Tags.ID);
		boolean inbound = Tags.TRUE.equals(parseStringAttribute(Tags.INBOUND));

		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == TEXT) {
				values.put(GamesExpansions.EXPANSION_NAME, mParser.getText());
			}
		}

		if (!mGameExpansionIds.remove(Integer.valueOf(expansionId))) {
			values.put(GamesExpansions.EXPANSION_ID, expansionId);
			values.put(GamesExpansions.INBOUND, inbound);
			addInsert(Games.buildExpansionsUri(mGameId), values);
		}
	}

	private ContentValues parseStats(ContentValues values) throws XmlPullParserException, IOException {
		String tag = null;
		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == START_TAG) {
				tag = mParser.getName();

				if (Tags.STATS_RANKS.equals(tag)) {
					parseRanks();
					tag = null;
				}
			} else if (type == END_TAG) {
				tag = null;
			} else if (type == TEXT) {
				String text = mParser.getText();

				if (Tags.STATS_USERS_RATED.equals(tag)) {
					values.put(Games.STATS_USERS_RATED, StringUtils.parseInt(text));
				} else if (Tags.STATS_AVERAGE.equals(tag)) {
					values.put(Games.STATS_AVERAGE, StringUtils.parseDouble(text));
				} else if (Tags.STATS_BAYES_AVERAGE.equals(tag)) {
					values.put(Games.STATS_BAYES_AVERAGE, StringUtils.parseDouble(text));
				} else if (Tags.STATS_STANDARD_DEVIATION.equals(tag)) {
					values.put(Games.STATS_STANDARD_DEVIATION, StringUtils.parseDouble(text));
				} else if (Tags.STATS_MEDIAN.equals(tag)) {
					values.put(Games.STATS_MEDIAN, StringUtils.parseInt(text));
				} else if (Tags.STATS_NUMBER_OWNED.equals(tag)) {
					values.put(Games.STATS_NUMBER_OWNED, StringUtils.parseInt(text));
				} else if (Tags.STATS_NUMBER_TRADING.equals(tag)) {
					values.put(Games.STATS_NUMBER_TRADING, StringUtils.parseInt(text));
				} else if (Tags.STATS_NUMBER_WANTING.equals(tag)) {
					values.put(Games.STATS_NUMBER_WANTING, StringUtils.parseInt(text));
				} else if (Tags.STATS_NUMBER_WISHING.equals(tag)) {
					values.put(Games.STATS_NUMBER_WISHING, StringUtils.parseInt(text));
				} else if (Tags.STATS_NUMBER_COMMENTS.equals(tag)) {
					values.put(Games.STATS_NUMBER_COMMENTS, StringUtils.parseInt(text));
				} else if (Tags.STATS_NUMBER_WEIGHTS.equals(tag)) {
					values.put(Games.STATS_NUMBER_WEIGHTS, StringUtils.parseInt(text));
				} else if (Tags.STATS_AVERAGE_WEIGHT.equals(tag)) {
					values.put(Games.STATS_AVERAGE_WEIGHT, StringUtils.parseDouble(text));
				}
			}
		}
		return values;
	}

	private void parseRanks() throws XmlPullParserException, IOException {
		List<ContentValues> valuesList = new ArrayList<ContentValues>();
		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == START_TAG) {
				if (Tags.STATS_RANKS_RANK.equals(mParser.getName())) {
					ContentValues values = new ContentValues();
					values.put(GameRanks.GAME_RANK_BAYES_AVERAGE,
						parseDoubleAttribute(Tags.STATS_RANKS_RANK_BAYESAVERAGE));
					values.put(GameRanks.GAME_RANK_FRIENDLY_NAME,
						parseStringAttribute(Tags.STATS_RANKS_RANK_FRIENDLYNAME));
					values.put(GameRanks.GAME_RANK_ID, parseIntegerAttribute(Tags.STATS_RANKS_RANK_ID));
					values.put(GameRanks.GAME_RANK_NAME, parseStringAttribute(Tags.STATS_RANKS_RANK_NAME));
					values.put(GameRanks.GAME_RANK_TYPE, parseStringAttribute(Tags.STATS_RANKS_RANK_TYPE));
					values.put(GameRanks.GAME_RANK_VALUE, parseIntegerAttribute(Tags.STATS_RANKS_RANK_VALUE));
					valuesList.add(values);
				}
			} else if (type == END_TAG) {
				if (Tags.STATS_RANKS.equals(mParser.getName())) {
					break;
				}
			}
		}

		List<Integer> rankIds = ResolverUtils.queryInts(mResolver, GameRanks.CONTENT_URI, GameRanks.GAME_RANK_ID,
			GameRanks.GAME_ID + "=?", new String[] { String.valueOf(mGameId) });

		for (ContentValues values : valuesList) {
			Integer rankId = values.getAsInteger(GameRanks.GAME_RANK_ID);
			if (rankIds.contains(rankId)) {
				addUpdate(Games.buildRanksUri(mGameId, rankId.intValue()), values);
				rankIds.remove(rankId);
			} else {
				addInsert(Games.buildRanksUri(mGameId), values);
			}
		}
		for (Integer rankId : rankIds) {
			addDelete(GameRanks.buildGameRankUri(rankId.intValue()));
		}
	}

	private void parsePoll() throws XmlPullParserException, IOException {
		final int depth = mParser.getDepth();
		int type;

		final String pollName = parseStringAttribute(Tags.POLL_NAME);
		ContentValues values = new ContentValues();
		values.put(GamePolls.POLL_NAME, pollName);
		values.put(GamePolls.POLL_TITLE, parseStringAttribute(Tags.POLL_TITLE));
		values.put(GamePolls.POLL_TOTAL_VOTES, parseStringAttribute(Tags.POLL_TOTAL_VOTES));

		List<String> players = new ArrayList<String>();
		if (mPollNames.remove(pollName)) {
			addUpdate(Games.buildPollsUri(mGameId, pollName), values);
			players = ResolverUtils.queryStrings(mResolver, Games.buildPollResultsUri(mGameId, pollName),
				GamePollResults.POLL_RESULTS_PLAYERS);
		} else {
			addInsert(Games.buildPollsUri(mGameId), values);
		}

		int sortIndex = 0;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == START_TAG) {
				if (Tags.POLL_RESULTS.equals(mParser.getName())) {
					parsePollResults(players, pollName, ++sortIndex);
				}
			}
		}

		for (String player : players) {
			addDelete(Games.buildPollResultsUri(mGameId, pollName, player));
		}
	}

	private void parsePollResults(List<String> existingPlayers, String pollName, int sortIndex)
		throws XmlPullParserException, IOException {
		ContentValues values = new ContentValues();
		String players = parseStringAttribute(Tags.POLL_RESULTS_PLAYERS);
		if (players == null) {
			players = "X";
		}
		values.put(GamePollResults.POLL_RESULTS_PLAYERS, players);
		values.put(GamePollResults.POLL_RESULTS_SORT_INDEX, sortIndex);

		List<String> existingValues = new ArrayList<String>();
		if (existingPlayers.remove(players)) {
			addUpdate(Games.buildPollResultsUri(mGameId, pollName, players), values);
			existingValues = ResolverUtils.queryStrings(mResolver,
				Games.buildPollResultsResultUri(mGameId, pollName, players),
				GamePollResultsResult.POLL_RESULTS_RESULT_VALUE);
		} else {
			addInsert(Games.buildPollResultsUri(mGameId, pollName), values);
		}

		int resultSortIndex = 0;
		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == START_TAG) {
				if (Tags.POLL_RESULT.equals(mParser.getName())) {
					ContentValues resultValues = new ContentValues();
					final String value = parseStringAttribute(Tags.POLL_RESULT_VALUE);
					resultValues.put(GamePollResultsResult.POLL_RESULTS_RESULT_VALUE, value);
					resultValues.put(GamePollResultsResult.POLL_RESULTS_RESULT_VOTES,
						parseIntegerAttribute(Tags.POLL_RESULT_VOTES));
					resultValues.put(GamePollResultsResult.POLL_RESULTS_RESULT_LEVEL,
						parseStringAttribute(Tags.POLL_RESULT_LEVEL));
					resultValues.put(GamePollResultsResult.POLL_RESULTS_RESULT_SORT_INDEX, ++resultSortIndex);

					if (existingValues.remove(value)) {
						addUpdate(Games.buildPollResultsResultUri(mGameId, pollName, players, value), resultValues);
					} else {
						addInsert(Games.buildPollResultsResultUri(mGameId, pollName, players), resultValues);
					}
				}
			}
		}

		for (String value : existingValues) {
			addDelete(Games.buildPollResultsResultUri(mGameId, pollName, players, value));
		}
	}

	private void insertMaybe(Uri uri, ContentValues values, int id) {
		if (!ResolverUtils.rowExists(mResolver, uri.buildUpon().appendPath(String.valueOf(id)).build())) {
			// insert outside of the batching process
			mResolver.insert(uri, values);
		}
	}

	private interface Tags {
		String BOARDGAMES = "boardgames";
		String BOARDGAME = "boardgame";
		String ID = "objectid";
		String YEAR_PUBLISHED = "yearpublished";
		String MIN_PLAYERS = "minplayers";
		String MAX_PLAYERS = "maxplayers";
		String PLAYING_TIME = "playingtime";
		String AGE = "age";
		String NAME = "name";
		String PRIMARY = "primary";
		String SORT_INDEX = "sortindex";
		String DESCRIPTION = "description";
		String THUMBNAIL = "thumbnail";
		String IMAGE = "image";
		String DESIGNER = "boardgamedesigner";
		String ARTIST = "boardgameartist";
		String PUBLISHER = "boardgamepublisher";
		String MECHANIC = "boardgamemechanic";
		String CATEGORY = "boardgamecategory";
		String EXPANSION = "boardgameexpansion";
		String INBOUND = "inbound";
		// family
		// podcastepisode
		// version
		// subdomain
		String POLL = "poll";
		String POLL_NAME = "name";
		String POLL_TITLE = "title";
		String POLL_TOTAL_VOTES = "totalvotes";
		String POLL_RESULTS = "results";
		String POLL_RESULTS_PLAYERS = "numplayers";
		String POLL_RESULT = "result";
		String POLL_RESULT_VALUE = "value";
		String POLL_RESULT_VOTES = "numvotes";
		String POLL_RESULT_LEVEL = "level";
		String STATISTICS = "statistics";
		String STATS_USERS_RATED = "usersrated";
		String STATS_AVERAGE = "average";
		String STATS_BAYES_AVERAGE = "bayesaverage";
		String STATS_RANKS = "ranks";
		String STATS_RANKS_RANK = "rank";
		String STATS_RANKS_RANK_TYPE = "type";
		String STATS_RANKS_RANK_ID = "id";
		String STATS_RANKS_RANK_NAME = "name";
		String STATS_RANKS_RANK_FRIENDLYNAME = "friendlyname";
		String STATS_RANKS_RANK_VALUE = "value";
		String STATS_RANKS_RANK_BAYESAVERAGE = "bayesaverage";
		String STATS_STANDARD_DEVIATION = "stddev";
		String STATS_MEDIAN = "median";
		String STATS_NUMBER_OWNED = "owned";
		String STATS_NUMBER_TRADING = "trading";
		String STATS_NUMBER_WANTING = "wanting";
		String STATS_NUMBER_WISHING = "wishing";
		String STATS_NUMBER_COMMENTS = "numcomments";
		String STATS_NUMBER_WEIGHTS = "numweights";
		String STATS_AVERAGE_WEIGHT = "averageweight";
		String TRUE = "true";
	}
}
