package com.boardgamegeek.ui.model;


import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.PresentationUtils;

import java.util.ArrayList;
import java.util.List;

public class GameCollectionItem {
	public static final String[] PROJECTION = {
		Collection._ID, Collection.COLLECTION_ID,
		Collection.COLLECTION_NAME,
		Collection.COLLECTION_YEAR_PUBLISHED,
		Collection.COLLECTION_THUMBNAIL_URL,
		Collection.STATUS_OWN,
		Collection.STATUS_PREVIOUSLY_OWNED,
		Collection.STATUS_FOR_TRADE,
		Collection.STATUS_WANT,
		Collection.STATUS_WANT_TO_BUY,
		Collection.STATUS_WISHLIST,
		Collection.STATUS_WANT_TO_PLAY,
		Collection.STATUS_PREORDERED,
		Collection.STATUS_WISHLIST_PRIORITY,
		Collection.NUM_PLAYS,
		Collection.COMMENT,
		Games.YEAR_PUBLISHED,
		Collection.RATING,
		Collection.IMAGE_URL
	};

	public static final Uri URI = Collection.CONTENT_URI;

	private static final int _ID = 0;
	private static final int COLLECTION_ID = 1;
	private static final int COLLECTION_NAME = 2;
	private static final int COLLECTION_YEAR = 3;
	private static final int COLLECTION_THUMBNAIL_URL = 4;
	private static final int STATUS_1 = 5;
	private static final int STATUS_N = 12;
	private static final int STATUS_WISHLIST = 10;
	private static final int STATUS_WISHLIST_PRIORITY = 13;
	private static final int NUM_PLAYS = 14;
	private static final int COMMENT = 15;
	private static final int YEAR_PUBLISHED = 16;
	private static final int RATING = 17;
	private static final int COLLECTION_IMAGE_URL = 18;

	private long internalId;
	private int collectionId;
	private int yearPublished;
	private String imageUrl;
	private String thumbnailUrl;
	private String collectionName;
	private int collectionYearPublished;
	private int numberOfPlays;
	private String comment;
	private double rating;
	private List<String> statuses;

	private GameCollectionItem() {
	}

	public static GameCollectionItem fromCursor(Context context, Cursor cursor) {
		GameCollectionItem item = new GameCollectionItem();
		item.internalId = cursor.getLong(_ID);
		item.collectionId = cursor.getInt(COLLECTION_ID);
		item.yearPublished = cursor.getInt(YEAR_PUBLISHED);
		item.imageUrl = cursor.getString(COLLECTION_IMAGE_URL);
		item.thumbnailUrl = cursor.getString(COLLECTION_THUMBNAIL_URL);
		item.collectionName = cursor.getString(COLLECTION_NAME);
		item.collectionYearPublished = cursor.getInt(COLLECTION_YEAR);
		item.numberOfPlays = cursor.getInt(NUM_PLAYS);
		item.comment = cursor.getString(COMMENT);
		item.rating = cursor.getDouble(RATING);

		item.statuses = new ArrayList<>();
		for (int i = STATUS_1; i <= STATUS_N; i++) {
			if (cursor.getInt(i) == 1) {
				if (i == STATUS_WISHLIST) {
					item.statuses.add(PresentationUtils.describeWishlist(context,
						cursor.getInt(STATUS_WISHLIST_PRIORITY)));
				} else {
					int index = i - STATUS_1;
					item.statuses.add(context.getResources().getStringArray(R.array.collection_status_filter_entries)[index]);
				}
			}
		}
		return item;
	}

	public static String getSelection() {
		return "collection." + Collection.GAME_ID + "=?";
	}

	public static String[] getSelectionArgs(int gameId) {
		return new String[] { String.valueOf(gameId) };
	}

	public long getInternalId() {
		return internalId;
	}

	public int getCollectionId() {
		return collectionId;
	}

	public int getYearPublished() {
		return yearPublished;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public String getCollectionName() {
		return collectionName;
	}

	public int getCollectionYearPublished() {
		return collectionYearPublished;
	}

	public int getNumberOfPlays() {
		return numberOfPlays;
	}

	public String getComment() {
		return comment;
	}

	public double getRating() {
		return rating;
	}

	public List<String> getStatuses() {
		return statuses;
	}
}
