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
import android.util.Log;

import com.boardgamegeek.Utility;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.SyncColumns;

public class RemoteCollectionHandler extends XmlHandler {
	private static final String TAG = "RemoteCollectionHandler";

	private long mStartTime;
	private XmlPullParser mParser;
	private ContentResolver mResolver;

	public RemoteCollectionHandler(long startTime) {
		super(BggContract.CONTENT_AUTHORITY);
		mStartTime = startTime;
	}

	@Override
	public boolean parse(XmlPullParser parser, ContentResolver resolver, String authority)
		throws XmlPullParserException, IOException {

		mParser = parser;
		mResolver = resolver;

		int type;
		while ((type = mParser.next()) != END_DOCUMENT) {
			if (type == START_TAG && Tags.ITEMS.equals(mParser.getName())) {

				int itemCount = Utility.parseInt(parser.getAttributeValue(null, Tags.TOTAL_ITEMS));
				Log.i(TAG, "Expecting " + itemCount + " items");

				parseItems();
			}
		}

		return false;
	}

	// TODO: Parse version-specific info

	private void parseItems() throws XmlPullParserException, IOException {

		final int depth = mParser.getDepth();
		int updateCount = 0;
		int insertCount = 0;
		int skipCount = 0;
		Cursor cursor = null;
		String[] projection = new String[] { SyncColumns.UPDATED_LIST };

		try {
			int type;
			while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
				if (type == START_TAG && Tags.ITEM.equals(mParser.getName())) {

					int gameId = Utility.parseInt(mParser.getAttributeValue(null, Tags.GAME_ID));
					// int collectionId =
					// Utility.parseInt(mParser.getAttributeValue(null,
					// Tags.COLLECTION_ID));

					ContentValues values = parseItem();
					// values.put(Games.COLLECTION_ID, collectionId);
					values.put(Games.UPDATED_LIST, System.currentTimeMillis());

					Uri uri = Games.buildGameUri(gameId);
					cursor = mResolver.query(uri, projection, null, null, null);
					if (cursor.moveToFirst()) {
						long lastUpdated = cursor.getLong(0);
						if (lastUpdated < mStartTime) {
							mResolver.update(uri, values, null, null);
							updateCount++;
						} else {
							skipCount++;
						}
					} else {
						values.put(Games.GAME_ID, gameId);
						mResolver.insert(Games.CONTENT_URI, values);
						insertCount++;
					}
					cursor.deactivate();
				}
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
			Log.i(TAG, "Updated " + updateCount + ", inserted " + insertCount + ", skipped " + skipCount
				+ " games");
		}
	}

	private ContentValues parseItem() throws XmlPullParserException, IOException {

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
				} else if (Tags.STATS.equals(tag)) {
					values.put(Games.MIN_PLAYERS, mParser.getAttributeValue(null, Tags.MIN_PLAYERS));
					values.put(Games.MAX_PLAYERS, mParser.getAttributeValue(null, Tags.MAX_PLAYERS));
					values.put(Games.PLAYING_TIME, mParser.getAttributeValue(null, Tags.PLAYING_TIME));
					values.put(Games.NUM_OWNED, mParser.getAttributeValue(null, Tags.NUM_OWNED));
				} else if (Tags.STATUS.equals(tag)) {
					// values.put(Games.STATUS_OWN,
					// mParser.getAttributeValue(null, Tags.STATUS_OWN));
					// values.put(Games.STATUS_PREVIOUSLY_OWNED,
					// mParser.getAttributeValue(null,
					// Tags.STATUS_PREVIOUSLY_OWNED));
					// values
					// .put(Games.STATUS_FOR_TRADE,
					// mParser.getAttributeValue(null, Tags.STATUS_FOR_TRADE));
					// values.put(Games.STATUS_WANT,
					// mParser.getAttributeValue(null, Tags.STATUS_WANT));
					// values.put(Games.STATUS_WANT_TO_PLAY,
					// mParser.getAttributeValue(null,
					// Tags.STATUS_WANT_TO_PLAY));
					// values.put(Games.STATUS_WANT_TO_BUY,
					// mParser.getAttributeValue(null,
					// Tags.STATUS_WANT_TO_BUY));
					// values.put(Games.STATUS_WISHLIST,
					// mParser.getAttributeValue(null, Tags.STATUS_WISHLIST));
					// values.put(Games.STATUS_PREORDERED,
					// mParser.getAttributeValue(null,
					// Tags.STATUS_PREORDERED));
				}
			} else if (type == END_TAG) {
				tag = null;
				sortIndex = 1;
			} else if (type == TEXT) {
				String text = mParser.getText();
				if (Tags.NAME.equals(tag)) {
					values.put(Games.GAME_NAME, text);
					values.put(Games.GAME_SORT_NAME, createSortName(text, sortIndex));
				} else if (Tags.YEAR_PUBLISHED.equals(tag)) {
					values.put(Games.YEAR_PUBLISHED, text);
				} else if (Tags.IMAGE.equals(tag)) {
					values.put(Games.IMAGE_URL, text);
				} else if (Tags.THUMBNAIL.equals(tag)) {
					values.put(Games.THUMBNAIL_URL, text);
				} else if (Tags.NUM_PLAYS.equals(tag)) {
					values.put(Games.NUM_PLAYS, Utility.parseInt(mParser.getAttributeValue(null,
						Tags.NUM_PLAYS)));
					// } else if (Tags.PRIVATE_INFO.equals(tag)) {
					// parsePrivateInfo(values);
				} else if (Tags.COMMENT.equals(tag)) {
					// values.put(Games.COMMENT, text);
				}
			}
		}

		return values;
	}

	// private ContentValues parsePrivateInfo(ContentValues values) throws
	// XmlPullParserException, IOException {
	// String tag = null;
	// final int depth = mParser.getDepth();
	// int type;
	// while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth)
	// && type != END_DOCUMENT) {
	//
	// if (type == START_TAG) {
	// tag = mParser.getName();
	// } else if (type == END_TAG) {
	// tag = null;
	// } else if (type == TEXT) {
	// String text = mParser.getText();
	// if (Tags.PRIVATE_INFO_ACQUIRED_FROM.equals(tag)) {
	// values.put(Games.PRIVATE_INFO_ACQUIRED_FROM, text);
	// } else if (Tags.PRIVATE_INFO_ACQUISITION_DATE.equals(tag)) {
	// // TODO: how to handle date in YYYY-MM-DD?
	// values.put(Games.PRIVATE_INFO_ACQUISITION_DATE, text);
	// } else if (Tags.PRIVATE_INFO_COMMENT.equals(tag)) {
	// values.put(Games.PRIVATE_INFO_COMMENT, text);
	// } else if (Tags.PRIVATE_INFO_CURRENT_VALUE.equals(tag)) {
	// values.put(Games.PRIVATE_INFO_CURRENT_VALUE, Utility.parseDouble(text));
	// } else if (Tags.PRIVATE_INFO_CURRENT_VALUE_CURRENCY.equals(tag)) {
	// values.put(Games.PRIVATE_INFO_CURRENT_VALUE_CURRENCY, text);
	// } else if (Tags.PRIVATE_INFO_PRICE_PAID.equals(tag)) {
	// values.put(Games.PRIVATE_INFO_PRICE_PAID, Utility.parseDouble(text));
	// } else if (Tags.PRIVATE_INFO_PRICE_PAID_CURRENCY.equals(tag)) {
	// values.put(Games.PRIVATE_INFO_PRICE_PAID_CURRENCY, text);
	// } else if (Tags.PRIVATE_INFO_QUANTITY.equals(tag)) {
	// values.put(Games.PRIVATE_INFO_QUANTITY, Utility.parseInt(text));
	// }
	// }
	// }
	// return values;
	// }

	private String createSortName(String name, int sortIndex) {
		if (sortIndex <= 1 || sortIndex > name.length()) {
			return name;
		}
		int i = sortIndex - 1;
		return name.substring(i) + ", " + name.substring(0, i).trim();
	}

	private interface Tags {
		String ITEMS = "items";
		String TOTAL_ITEMS = "totalitems";
		String ITEM = "item";
		String GAME_ID = "objectid";
		// String COLLECTION_ID = "collid";
		String NAME = "name";
		String SORT_INDEX = "sortindex";
		String YEAR_PUBLISHED = "yearpublished";
		String IMAGE = "image";
		String THUMBNAIL = "thumbnail";

		String STATS = "stats";
		String MIN_PLAYERS = "minplayers";
		String MAX_PLAYERS = "maxplayers";
		String PLAYING_TIME = "playingtime";
		String NUM_OWNED = "numowned";

		String STATUS = "status";
		// String STATUS_OWN = "own";
		// String STATUS_PREVIOUSLY_OWNED = "prevowned";
		// String STATUS_FOR_TRADE = "fortrade";
		// String STATUS_WANT = "want";
		// String STATUS_WANT_TO_PLAY = "wanttoplay";
		// String STATUS_WANT_TO_BUY = "wanttobuy";
		// String STATUS_WISHLIST = "wishlist";
		// String STATUS_PREORDERED = "preordered";

		String NUM_PLAYS = "numplays";

		// String PRIVATE_INFO = "privateinfo";
		// String PRIVATE_INFO_PRICE_PAID_CURRENCY = "pp_currency";
		// String PRIVATE_INFO_PRICE_PAID = "pricepaid";
		// String PRIVATE_INFO_CURRENT_VALUE_CURRENCY = "cv_currency";
		// String PRIVATE_INFO_CURRENT_VALUE = "currvalue";
		// String PRIVATE_INFO_QUANTITY = "quantity";
		// String PRIVATE_INFO_ACQUISITION_DATE = "acquisitiondate";
		// String PRIVATE_INFO_ACQUIRED_FROM = "acquiredfrom";
		// String PRIVATE_INFO_COMMENT = "privatecomment";

		String COMMENT = "comment";
	}
}
