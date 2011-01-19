package com.boardgamegeek.io;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;
import static org.xmlpull.v1.XmlPullParser.TEXT;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.boardgamegeek.Utility;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.StringUtils;

public class RemoteGameHandler extends XmlHandler {
	// private static final String TAG = "RemoteGameHandler";

	private XmlPullParser mParser;
	private ContentResolver mResolver;

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
				int id = Utility.parseInt(parser.getAttributeValue(null, Tags.ID));

				ContentValues values = parseGame();

				Uri uri = Games.buildGameUri(id);
				Cursor cursor = resolver.query(uri, projection, null, null, null);
				if (!cursor.moveToFirst()) {
					values.put(Games.GAME_ID, id);
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
					sortIndex = Utility.parseInt(mParser.getAttributeValue(null, Tags.SORT_INDEX), 1);
					String primary = mParser.getAttributeValue(null, Tags.PRIMARY);
					if (!Tags.TRUE.equals(primary)) {
						tag = null;
					}
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

		values.put(Games.UPDATED_DETAIL, System.currentTimeMillis());
		return values;
	}

	private ContentValues parseStats(ContentValues values) throws XmlPullParserException, IOException {
		String tag = null;
		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == START_TAG) {
				tag = mParser.getName();
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
		// family
		// expansion
		// artist
		// mechanic
		// designer
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
		// <ranks>
		// <rank type="subtype" id="1" name="boardgame"
		// friendlyname="Board Game Rank" value="17" bayesaverage="7.72547"/>
		// <rank type="family" id="5497" name="strategygames"
		// friendlyname="Strategy Game Rank" value="15" bayesaverage="7.7765"/>
		// </ranks>
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
