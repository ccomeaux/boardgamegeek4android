package com.boardgamegeek.io;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;
import static org.xmlpull.v1.XmlPullParser.TEXT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.boardgamegeek.Utility;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Artists;
import com.boardgamegeek.provider.BggContract.Designers;
import com.boardgamegeek.provider.BggContract.GameRanks;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggDatabase.GamesArtists;
import com.boardgamegeek.provider.BggDatabase.GamesDesigners;
import com.boardgamegeek.util.StringUtils;

public class RemoteGameHandler extends XmlHandler {
	// private static final String TAG = "RemoteGameHandler";

	private XmlPullParser mParser;
	private ContentResolver mResolver;
	private int mGameId;

	public RemoteGameHandler() {
		super(BggContract.CONTENT_AUTHORITY);
	}

	@Override
	public boolean parse(XmlPullParser parser, ContentResolver resolver, String authority)
			throws XmlPullParserException, IOException {

		mParser = parser;
		mResolver = resolver;

		String[] projection = { Games.GAME_ID };

		int type;
		while ((type = parser.next()) != END_DOCUMENT) {
			if (type == START_TAG && Tags.BOARDGAME.equals(parser.getName())) {
				mGameId = parseIntegerAttribute(Tags.ID);

				ContentValues values = parseGame();

				Uri uri = Games.buildGameUri(mGameId);
				Cursor cursor = resolver.query(uri, projection, null, null, null);
				if (!cursor.moveToFirst()) {
					values.put(Games.GAME_ID, mGameId);
					values.put(Games.UPDATED_LIST, System.currentTimeMillis());
					mResolver.insert(Games.CONTENT_URI, values);
				} else {
					mResolver.update(uri, values, null, null);
				}

				cursor.close();
			}
		}

		return false;
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
				}
			} else if (type == END_TAG) {
				tag = null;
			} else if (type == TEXT) {
				String text = mParser.getText();

				if (Tags.YEAR_PUBLISHED.equals(tag)) {
					values.put(Games.YEAR_PUBLISHED, text);
				} else if (Tags.MIN_PLAYERS.equals(tag)) {
					values.put(Games.MIN_PLAYERS, Utility.parseInt(text));
				} else if (Tags.MAX_PLAYERS.equals(tag)) {
					values.put(Games.MAX_PLAYERS, Utility.parseInt(text));
				} else if (Tags.PLAYING_TIME.equals(tag)) {
					values.put(Games.PLAYING_TIME, Utility.parseInt(text));
				} else if (Tags.AGE.equals(tag)) {
					values.put(Games.MINIMUM_AGE, Utility.parseInt(text));
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

		mResolver.insert(Designers.CONTENT_URI, values);

		values.clear();
		values.put(GamesDesigners.GAME_ID, mGameId);
		values.put(GamesDesigners.DESIGNER_ID, designerId);
		mResolver.insert(Games.buildDesignersUri(mGameId), values);
		
		// TODO: delete all unused games-designers records
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

		mResolver.insert(Artists.CONTENT_URI, values);

		values.clear();
		values.put(GamesArtists.GAME_ID, mGameId);
		values.put(GamesArtists.ARTIST_ID, artistId);
		mResolver.insert(Games.buildArtistsUri(mGameId), values);
		
		// TODO: delete all unused games-artists records
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
					values.put(Games.STATS_USERS_RATED, Utility.parseInt(text));
				} else if (Tags.STATS_AVERAGE.equals(tag)) {
					values.put(Games.STATS_AVERAGE, Utility.parseDouble(text));
				} else if (Tags.STATS_BAYES_AVERAGE.equals(tag)) {
					values.put(Games.STATS_BAYES_AVERAGE, Utility.parseDouble(text));
				} else if (Tags.STATS_STANDARD_DEVIATION.equals(tag)) {
					values.put(Games.STATS_STANDARD_DEVIATION, Utility.parseDouble(text));
				} else if (Tags.STATS_MEDIAN.equals(tag)) {
					values.put(Games.STATS_MEDIAN, Utility.parseInt(text));
				} else if (Tags.STATS_NUMBER_OWNED.equals(tag)) {
					values.put(Games.STATS_NUMBER_OWNED, Utility.parseInt(text));
				} else if (Tags.STATS_NUMBER_TRADING.equals(tag)) {
					values.put(Games.STATS_NUMBER_TRADING, Utility.parseInt(text));
				} else if (Tags.STATS_NUMBER_WANTING.equals(tag)) {
					values.put(Games.STATS_NUMBER_WANTING, Utility.parseInt(text));
				} else if (Tags.STATS_NUMBER_WISHING.equals(tag)) {
					values.put(Games.STATS_NUMBER_WISHING, Utility.parseInt(text));
				} else if (Tags.STATS_NUMBER_COMMENTS.equals(tag)) {
					values.put(Games.STATS_NUMBER_COMMENTS, Utility.parseInt(text));
				} else if (Tags.STATS_NUMBER_WEIGHTS.equals(tag)) {
					values.put(Games.STATS_NUMBER_WEIGHTS, Utility.parseInt(text));
				} else if (Tags.STATS_AVERAGE_WEIGHT.equals(tag)) {
					values.put(Games.STATS_AVERAGE_WEIGHT, Utility.parseDouble(text));
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

		List<Integer> ids = getCurrentGameRankIds();
		for (ContentValues values : valuesList) {
			Integer id = values.getAsInteger(GameRanks.GAME_RANK_ID);
			if (ids.contains(id)) {
				mResolver.update(GameRanks.buildGameRankUri(id.intValue()), values, null, null);
				ids.remove(id);
			} else {
				mResolver.insert(Games.buildRanksUri(mGameId), values);
			}
		}
		for (Integer id : ids) {
			mResolver.delete(GameRanks.buildGameRankUri(id.intValue()), null, null);
		}
	}

	private List<Integer> getCurrentGameRankIds() {
		List<Integer> ids = new ArrayList<Integer>();
		Cursor c = mResolver.query(GameRanks.CONTENT_URI, new String[] { GameRanks.GAME_RANK_ID }, GameRanks.GAME_ID
				+ "=?", new String[] { "" + mGameId }, null);
		try {
			while (c.moveToNext()) {
				ids.add(c.getInt(0));
			}
			return ids;
		} finally {
			c.close();
		}
	}

	private String parseStringAttribute(String tag) {
		return mParser.getAttributeValue(null, tag);
	}

	private double parseDoubleAttribute(String tag) {
		return Utility.parseDouble(parseStringAttribute(tag));
	}

	private int parseIntegerAttribute(String tag) {
		return Utility.parseInt(parseStringAttribute(tag));
	}

	private int parseIntegerAttribute(String tag, int defaultValue) {
		return Utility.parseInt(parseStringAttribute(tag), defaultValue);
	}

	private interface Tags {
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
		// family
		// expansion
		// mechanic
		// publisher
		// podcastepisode
		// version
		// subdomain
		// category
		// poll
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
