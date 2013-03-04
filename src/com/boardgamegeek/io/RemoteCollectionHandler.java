package com.boardgamegeek.io;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;
import static org.xmlpull.v1.XmlPullParser.TEXT;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.boardgamegeek.database.ResolverUtils;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Thumbnails;
import com.boardgamegeek.util.FileUtils;
import com.boardgamegeek.util.StringUtils;

public class RemoteCollectionHandler extends RemoteBggHandler {
	private static final String TAG = makeLogTag(RemoteCollectionHandler.class);

	private static final int BATCH_SIZE = 25;

	// TODO: Parse Version Info

	private long mStartTime;
	private int mUpdateGameCount = 0;
	private int mInsertGameCount = 0;
	private int mSkipGameCount = 0;
	private int mUpdateCollectionCount = 0;
	private int mInsertCollectionCount = 0;
	private int mSkipCollectionCount = 0;

	private String[] mGameProjection = new String[] { Games.UPDATED_LIST };
	private String[] mCollectionProjection = new String[] { Collection.UPDATED };

	public RemoteCollectionHandler(long startTime) {
		super();
		mStartTime = startTime;
	}

	@Override
	public int getCount() {
		return mUpdateGameCount + mInsertGameCount + mSkipGameCount;
	}

	public int getNumUpdates() {
		return mUpdateCollectionCount + mUpdateGameCount;
	}

	public int getNumInserts() {
		return mInsertCollectionCount + mInsertGameCount;
	}

	public int getNumSkips() {
		return mSkipCollectionCount + mSkipGameCount;
	}

	@Override
	protected String getRootNodeName() {
		return Tags.ITEMS;
	}

	@Override
	protected void parseItems() throws XmlPullParserException, IOException {

		ContentValues gameValues = new ContentValues();
		ContentValues collectionValues = new ContentValues();

		final int depth = mParser.getDepth();
		int type;
		int batchCount = 0;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {
			if (type == START_TAG && Tags.ITEM.equals(mParser.getName())) {

				int gameId = parseIntegerAttribute(Tags.GAME_ID);
				int collectionId = parseIntegerAttribute(Tags.COLLECTION_ID);

				gameValues.clear();
				collectionValues.clear();
				parseItem(gameValues, collectionValues);

				insertOrUpdateGame(gameId, gameValues);
				insertOrUpdateCollectionItem(collectionId, gameId, collectionValues);

				batchCount++;
				if (batchCount >= BATCH_SIZE) {
					LOGI(TAG, "Applying a batch of " + BATCH_SIZE + " games.");
					batchCount = 0;
					ResolverUtils.applyBatch(mResolver, mBatch);
				}
			}
		}
		LOGI(TAG, "Updated " + mUpdateGameCount + ", inserted " + mInsertGameCount + ", skipped " + mSkipGameCount
			+ " games");
		LOGI(TAG, "Updated " + mUpdateCollectionCount + ", inserted " + mInsertCollectionCount + ", skipped "
			+ mSkipCollectionCount + " collection items");
	}

	private void maybeDeleteThumbnail(ContentValues values, Uri uri, String column) {
		if (!values.containsKey(column)) {
			return;
		}

		String oldThumbnailUrl = ResolverUtils.queryString(mResolver, uri, column);
		String newThumbnailUrl = values.getAsString(column);
		if (newThumbnailUrl.equals(oldThumbnailUrl)) {
			return;
		}

		String thumbnailFileName = FileUtils.getFileNameFromUrl(oldThumbnailUrl);
		if (!TextUtils.isEmpty(thumbnailFileName)) {
			addDelete(Thumbnails.buildUri(thumbnailFileName));
		}
	}

	private void insertOrUpdateGame(int gameId, ContentValues values) {
		Cursor cursor = null;
		try {
			values.put(Games.UPDATED_LIST, System.currentTimeMillis());

			Uri uri = Games.buildGameUri(gameId);
			cursor = mResolver.query(uri, mGameProjection, null, null, null);
			if (cursor.moveToFirst()) {
				long lastUpdated = cursor.getLong(0);
				if (lastUpdated >= mStartTime) {
					mSkipGameCount++;
				}
			} else {
				mInsertGameCount++;
				values.put(Games.GAME_ID, gameId);
				addInsert(Games.CONTENT_URI, values);
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
	}

	private void insertOrUpdateCollectionItem(int itemId, int gameId, ContentValues values) {
		Cursor cursor = null;
		try {
			values.put(Collection.GAME_ID, gameId);
			values.put(Collection.UPDATED, System.currentTimeMillis());

			Uri uri = Collection.buildItemUri(itemId);
			cursor = mResolver.query(uri, mCollectionProjection, null, null, null);
			if (cursor.moveToFirst()) {
				long lastUpdated = cursor.getLong(0);
				if (lastUpdated < mStartTime) {
					mUpdateCollectionCount++;
					maybeDeleteThumbnail(values, uri, Collection.THUMBNAIL_URL);
					addUpdate(uri, values);
				} else {
					mSkipCollectionCount++;
				}
			} else {
				mInsertCollectionCount++;
				values.put(Collection.COLLECTION_ID, itemId);
				addInsert(Collection.CONTENT_URI, values);
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
	}

	private void parseItem(ContentValues gameValues, ContentValues collectionValues) throws XmlPullParserException,
		IOException {

		String tag = null;
		int sortIndex = 1;

		final int depth = mParser.getDepth();
		int type;
		while (((type = mParser.next()) != END_TAG || mParser.getDepth() > depth) && type != END_DOCUMENT) {

			if (type == START_TAG) {
				tag = mParser.getName();

				if (Tags.NAME.equals(tag)) {
					sortIndex = parseIntegerAttribute(Tags.SORT_INDEX, 1);
				} else if (Tags.STATS.equals(tag)) {
					gameValues.put(Games.MIN_PLAYERS, parseIntegerAttribute(Tags.MIN_PLAYERS));
					gameValues.put(Games.MAX_PLAYERS, parseIntegerAttribute(Tags.MAX_PLAYERS));
					gameValues.put(Games.PLAYING_TIME, parseIntegerAttribute(Tags.PLAYING_TIME));
					gameValues.put(Games.STATS_NUMBER_OWNED, parseIntegerAttribute(Tags.NUM_OWNED));
				} else if (Tags.STATUS.equals(tag)) {
					collectionValues.put(Collection.STATUS_OWN, parseBooleanAttribute(Tags.STATUS_OWN));
					collectionValues.put(Collection.STATUS_PREVIOUSLY_OWNED,
						parseBooleanAttribute(Tags.STATUS_PREVIOUSLY_OWNED));
					collectionValues.put(Collection.STATUS_FOR_TRADE, parseBooleanAttribute(Tags.STATUS_FOR_TRADE));
					collectionValues.put(Collection.STATUS_WANT, parseBooleanAttribute(Tags.STATUS_WANT));
					collectionValues.put(Collection.STATUS_WANT_TO_PLAY,
						parseBooleanAttribute(Tags.STATUS_WANT_TO_PLAY));
					collectionValues.put(Collection.STATUS_WANT_TO_BUY, parseBooleanAttribute(Tags.STATUS_WANT_TO_BUY));
					collectionValues.put(Collection.STATUS_WISHLIST, parseBooleanAttribute(Tags.STATUS_WISHLIST));
					collectionValues.put(Collection.STATUS_WISHLIST_PRIORITY,
						parseIntegerAttribute(Tags.STATUS_WISHLIST_PRIORITY));
					collectionValues.put(Collection.STATUS_PREORDERED, parseBooleanAttribute(Tags.STATUS_PREORDERED));
					collectionValues.put(Collection.LAST_MODIFIED, parseDateAttribute(Tags.LAST_MODIFIED));
				} else if (Tags.RATING.equals(tag)) {
					collectionValues.put(Collection.RATING, parseDoubleAttribute(Tags.VALUE));
				} else if (Tags.PRIVATE_INFO.equals(tag)) {
					collectionValues.put(Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY,
						parseStringAttribute(Tags.PRIVATE_INFO_PRICE_PAID_CURRENCY));
					collectionValues.put(Collection.PRIVATE_INFO_PRICE_PAID,
						parseDoubleAttribute(Tags.PRIVATE_INFO_PRICE_PAID));
					collectionValues.put(Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY,
						parseStringAttribute(Tags.PRIVATE_INFO_CURRENT_VALUE_CURRENCY));
					collectionValues.put(Collection.PRIVATE_INFO_CURRENT_VALUE,
						parseDoubleAttribute(Tags.PRIVATE_INFO_CURRENT_VALUE));
					collectionValues.put(Collection.PRIVATE_INFO_QUANTITY,
						parseIntegerAttribute(Tags.PRIVATE_INFO_QUANTITY));
					collectionValues.put(Collection.PRIVATE_INFO_ACQUISITION_DATE,
						parseStringAttribute(Tags.PRIVATE_INFO_ACQUISITION_DATE));
					collectionValues.put(Collection.PRIVATE_INFO_ACQUIRED_FROM,
						parseStringAttribute(Tags.PRIVATE_INFO_ACQUIRED_FROM));
				}
			} else if (type == END_TAG) {
				tag = null;
				sortIndex = 1;
			} else if (type == TEXT) {
				String text = mParser.getText();
				if (Tags.NAME.equals(tag)) {
					String sortName = StringUtils.createSortName(text, sortIndex);
					collectionValues.put(Collection.COLLECTION_NAME, text);
					collectionValues.put(Collection.COLLECTION_SORT_NAME, sortName);
					gameValues.put(Games.GAME_NAME, text);
					gameValues.put(Games.GAME_SORT_NAME, sortName);
				} else if (Tags.ORIGINAL_NAME.equals(tag)) {
					gameValues.put(Games.GAME_NAME, text);
				} else if (Tags.YEAR_PUBLISHED.equals(tag)) {
					int year = StringUtils.parseInt(text);
					collectionValues.put(Collection.COLLECTION_YEAR_PUBLISHED, year);
					gameValues.put(Games.YEAR_PUBLISHED, year);
				} else if (Tags.IMAGE.equals(tag)) {
					collectionValues.put(Collection.COLLECTION_IMAGE_URL, text);
					gameValues.put(Games.IMAGE_URL, text);
				} else if (Tags.THUMBNAIL.equals(tag)) {
					collectionValues.put(Collection.COLLECTION_THUMBNAIL_URL, text);
					gameValues.put(Games.THUMBNAIL_URL, text);
				} else if (Tags.NUM_PLAYS.equals(tag)) {
					gameValues.put(Games.NUM_PLAYS, parseIntegerAttribute(Tags.NUM_PLAYS));
				} else if (Tags.COMMENT.equals(tag)) {
					collectionValues.put(Collection.COMMENT, text);
				} else if (Tags.CONDITION.equals(tag)) {
					collectionValues.put(Collection.CONDITION, text);
				} else if (Tags.HASPARTS_LIST.equals(tag)) {
					collectionValues.put(Collection.HASPARTS_LIST, text);
				} else if (Tags.WANTPARTS_LIST.equals(tag)) {
					collectionValues.put(Collection.WANTPARTS_LIST, text);
				} else if (Tags.WISHLIST_COMMENT.equals(tag)) {
					collectionValues.put(Collection.WISHLIST_COMMENT, text);
				} else if (Tags.PRIVATE_INFO_COMMENT.equals(tag)) {
					collectionValues.put(Collection.PRIVATE_INFO_COMMENT, text);
				}
			}
		}
	}

	private interface Tags {
		String ITEMS = "items";
		String ITEM = "item";
		String GAME_ID = "objectid";
		String COLLECTION_ID = "collid";
		String NAME = "name";
		String ORIGINAL_NAME = "originalname";
		String SORT_INDEX = "sortindex";
		String YEAR_PUBLISHED = "yearpublished";
		String IMAGE = "image";
		String THUMBNAIL = "thumbnail";

		String STATS = "stats";
		String MIN_PLAYERS = "minplayers";
		String MAX_PLAYERS = "maxplayers";
		String PLAYING_TIME = "playingtime";
		String NUM_OWNED = "numowned";
		String RATING = "rating";
		String VALUE = "value";

		String STATUS = "status";
		String STATUS_OWN = "own";
		String STATUS_PREVIOUSLY_OWNED = "prevowned";
		String STATUS_FOR_TRADE = "fortrade";
		String STATUS_WANT = "want";
		String STATUS_WANT_TO_PLAY = "wanttoplay";
		String STATUS_WANT_TO_BUY = "wanttobuy";
		String STATUS_WISHLIST = "wishlist";
		String STATUS_WISHLIST_PRIORITY = "wishlistpriority";
		String STATUS_PREORDERED = "preordered";

		String LAST_MODIFIED = "lastmodified";

		String NUM_PLAYS = "numplays";

		String PRIVATE_INFO = "privateinfo";
		String PRIVATE_INFO_PRICE_PAID_CURRENCY = "pp_currency";
		String PRIVATE_INFO_PRICE_PAID = "pricepaid";
		String PRIVATE_INFO_CURRENT_VALUE_CURRENCY = "cv_currency";
		String PRIVATE_INFO_CURRENT_VALUE = "currvalue";
		String PRIVATE_INFO_QUANTITY = "quantity";
		String PRIVATE_INFO_ACQUISITION_DATE = "acquisitiondate";
		String PRIVATE_INFO_ACQUIRED_FROM = "acquiredfrom";
		String PRIVATE_INFO_COMMENT = "privatecomment";

		String COMMENT = "comment";
		String CONDITION = "conditiontext";
		String WANTPARTS_LIST = "wantpartslist";
		String HASPARTS_LIST = "haspartslist";
		String WISHLIST_COMMENT = "wishlistcomment";
	}
}
