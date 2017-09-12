package com.boardgamegeek.ui.model;


import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;

import com.boardgamegeek.provider.BggContract.Collection;

public class CollectionItem {
	public static final Uri URI = Collection.CONTENT_URI;

	public static final String[] PROJECTION = {
		Collection._ID,
		Collection.COLLECTION_ID,
		Collection.COLLECTION_NAME,
		Collection.COLLECTION_SORT_NAME,
		Collection.COMMENT,
		Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY,
		Collection.PRIVATE_INFO_PRICE_PAID,
		Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY,
		Collection.PRIVATE_INFO_CURRENT_VALUE,
		Collection.PRIVATE_INFO_QUANTITY,
		Collection.PRIVATE_INFO_ACQUISITION_DATE,
		Collection.PRIVATE_INFO_ACQUIRED_FROM,
		Collection.PRIVATE_INFO_COMMENT,
		Collection.LAST_MODIFIED,
		Collection.COLLECTION_THUMBNAIL_URL,
		Collection.COLLECTION_IMAGE_URL,
		Collection.COLLECTION_YEAR_PUBLISHED,
		Collection.CONDITION,
		Collection.HASPARTS_LIST,
		Collection.WANTPARTS_LIST,
		Collection.WISHLIST_COMMENT,
		Collection.RATING,
		Collection.UPDATED,
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
		Collection.RATING_DIRTY_TIMESTAMP,
		Collection.COMMENT_DIRTY_TIMESTAMP,
		Collection.PRIVATE_INFO_DIRTY_TIMESTAMP,
		Collection.STATUS_DIRTY_TIMESTAMP,
		Collection.COLLECTION_DIRTY_TIMESTAMP,
		Collection.WISHLIST_COMMENT_DIRTY_TIMESTAMP,
		Collection.TRADE_CONDITION_DIRTY_TIMESTAMP,
		Collection.WANT_PARTS_DIRTY_TIMESTAMP,
		Collection.HAS_PARTS_DIRTY_TIMESTAMP
	};

	final int _ID = 0;
	final int COLLECTION_ID = 1;
	final int COLLECTION_NAME = 2;
	// int COLLECTION_SORT_NAME = 3;
	final int COMMENT = 4;
	final int PRIVATE_INFO_PRICE_PAID_CURRENCY = 5;
	final int PRIVATE_INFO_PRICE_PAID = 6;
	final int PRIVATE_INFO_CURRENT_VALUE_CURRENCY = 7;
	final int PRIVATE_INFO_CURRENT_VALUE = 8;
	final int PRIVATE_INFO_QUANTITY = 9;
	final int PRIVATE_INFO_ACQUISITION_DATE = 10;
	final int PRIVATE_INFO_ACQUIRED_FROM = 11;
	final int PRIVATE_INFO_COMMENT = 12;
	final int LAST_MODIFIED = 13;
	final int COLLECTION_THUMBNAIL_URL = 14;
	final int COLLECTION_IMAGE_URL = 15;
	final int COLLECTION_YEAR_PUBLISHED = 16;
	final int CONDITION = 17;
	final int HAS_PARTS_LIST = 18;
	final int WANT_PARTS_LIST = 19;
	final int WISHLIST_COMMENT = 20;
	final int RATING = 21;
	final int UPDATED = 22;
	final int STATUS_OWN = 23;
	final int STATUS_PREVIOUSLY_OWNED = 24;
	final int STATUS_FOR_TRADE = 25;
	final int STATUS_WANT = 26;
	final int STATUS_WANT_TO_BUY = 27;
	final int STATUS_WISHLIST = 28;
	final int STATUS_WANT_TO_PLAY = 29;
	final int STATUS_PRE_ORDERED = 30;
	final int STATUS_WISHLIST_PRIORITY = 31;
	final int NUM_PLAYS = 32;
	final int RATING_DIRTY_TIMESTAMP = 33;
	final int COMMENT_DIRTY_TIMESTAMP = 34;
	final int PRIVATE_INFO_DIRTY_TIMESTAMP = 35;
	final int STATUS_DIRTY_TIMESTAMP = 36;
	final int COLLECTION_DIRTY_TIMESTAMP = 37;
	final int WISHLIST_COMMENT_DIRTY_TIMESTAMP = 38;
	final int TRADE_CONDITION_DIRTY_TIMESTAMP = 39;
	final int WANT_PARTS_DIRTY_TIMESTAMP = 40;
	final int HAS_PARTS_DIRTY_TIMESTAMP = 41;

	int id;
	long internalId;
	String name;
	// String sortName;
	String comment;
	long commentTimestamp;
	long lastModifiedDateTime;
	double rating;
	long ratingTimestamp;
	long updated;
	String priceCurrency;
	double price;
	String currentValueCurrency;
	double currentValue;
	int quantity;
	String acquiredFrom;
	String acquisitionDate;
	String privateComment;
	long privateInfoTimestamp;
	long statusTimestamp;
	String imageUrl;
	String thumbnailUrl;
	int year;
	String condition;
	String wantParts;
	String hasParts;
	int wishlistPriority;
	String wishlistComment;
	int numPlays;
	boolean own;
	boolean previouslyOwned;
	boolean wantToBuy;
	boolean wantToPlay;
	boolean preordered;
	boolean wantInTrade;
	boolean forTrade;
	boolean wishlist;
	long dirtyTimestamp;
	long wishlistCommentDirtyTimestamp;
	long tradeConditionDirtyTimestamp;
	long wantPartsDirtyTimestamp;
	long hasPartsDirtyTimestamp;

	public CollectionItem(Context context, Cursor cursor) {
		Resources r = context.getResources();

		id = cursor.getInt(COLLECTION_ID);
		internalId = cursor.getLong(_ID);
		name = cursor.getString(COLLECTION_NAME);
		// sortName = cursor.getString(COLLECTION_SORT_NAME);

		own = cursor.getInt(STATUS_OWN) == 1;
		previouslyOwned = cursor.getInt(STATUS_PREVIOUSLY_OWNED) == 1;
		wantToBuy = cursor.getInt(STATUS_WANT_TO_BUY) == 1;
		wantToPlay = cursor.getInt(STATUS_WANT_TO_PLAY) == 1;
		preordered = cursor.getInt(STATUS_PRE_ORDERED) == 1;
		wantInTrade = cursor.getInt(STATUS_WANT) == 1;
		forTrade = cursor.getInt(STATUS_FOR_TRADE) == 1;
		wishlist = cursor.getInt(STATUS_WISHLIST) == 1;

		comment = cursor.getString(COMMENT);
		commentTimestamp = cursor.getLong(COMMENT_DIRTY_TIMESTAMP);
		rating = cursor.getDouble(RATING);
		ratingTimestamp = cursor.getLong(RATING_DIRTY_TIMESTAMP);
		lastModifiedDateTime = cursor.getLong(LAST_MODIFIED);
		updated = cursor.getLong(UPDATED);
		priceCurrency = cursor.getString(PRIVATE_INFO_PRICE_PAID_CURRENCY);
		price = cursor.getDouble(PRIVATE_INFO_PRICE_PAID);
		currentValueCurrency = cursor.getString(PRIVATE_INFO_CURRENT_VALUE_CURRENCY);
		currentValue = cursor.getDouble(PRIVATE_INFO_CURRENT_VALUE);
		quantity = cursor.getInt(PRIVATE_INFO_QUANTITY);
		privateComment = cursor.getString(PRIVATE_INFO_COMMENT);
		acquiredFrom = cursor.getString(PRIVATE_INFO_ACQUIRED_FROM);
		acquisitionDate = cursor.getString(PRIVATE_INFO_ACQUISITION_DATE);
		privateInfoTimestamp = cursor.getLong(PRIVATE_INFO_DIRTY_TIMESTAMP);
		statusTimestamp = cursor.getLong(STATUS_DIRTY_TIMESTAMP);
		imageUrl = cursor.getString(COLLECTION_IMAGE_URL);
		thumbnailUrl = cursor.getString(COLLECTION_THUMBNAIL_URL);
		year = cursor.getInt(COLLECTION_YEAR_PUBLISHED);
		wishlistPriority = cursor.getInt(STATUS_WISHLIST_PRIORITY);
		wishlistComment = cursor.getString(WISHLIST_COMMENT);
		condition = cursor.getString(CONDITION);
		wantParts = cursor.getString(WANT_PARTS_LIST);
		hasParts = cursor.getString(HAS_PARTS_LIST);
		numPlays = cursor.getInt(NUM_PLAYS);
		dirtyTimestamp = cursor.getLong(COLLECTION_DIRTY_TIMESTAMP);
		wishlistCommentDirtyTimestamp = cursor.getLong(WISHLIST_COMMENT_DIRTY_TIMESTAMP);
		tradeConditionDirtyTimestamp = cursor.getLong(TRADE_CONDITION_DIRTY_TIMESTAMP);
		wantPartsDirtyTimestamp = cursor.getLong(WANT_PARTS_DIRTY_TIMESTAMP);
		hasPartsDirtyTimestamp = cursor.getLong(HAS_PARTS_DIRTY_TIMESTAMP);
	}

	public static String getSelection(int collectionId) {
		if (collectionId != 0) {
			return Collection.COLLECTION_ID + "=?";
		} else {
			return "collection." + Collection.GAME_ID + "=? AND " + Collection.COLLECTION_ID + " IS NULL";
		}
	}

	public static String[] getSelectionArgs(int collectionId, int gameId) {
		if (collectionId != 0) {
			return new String[] { String.valueOf(collectionId) };
		} else {
			return new String[] { String.valueOf(gameId) };
		}
	}

	public long getInternalId() {
		return internalId;
	}

	public long getUpdated() {
		return updated;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getComment() {
		return comment;
	}

	public long getCommentTimestamp() {
		return commentTimestamp;
	}

	public long getLastModifiedDateTime() {
		return lastModifiedDateTime;
	}

	public double getRating() {
		return rating;
	}

	public long getRatingTimestamp() {
		return ratingTimestamp;
	}

	public String getPriceCurrency() {
		return priceCurrency;
	}

	public double getPrice() {
		return price;
	}

	public String getCurrentValueCurrency() {
		return currentValueCurrency;
	}

	public double getCurrentValue() {
		return currentValue;
	}

	public int getQuantity() {
		return quantity;
	}

	public String getAcquiredFrom() {
		return acquiredFrom;
	}

	public String getAcquisitionDate() {
		return acquisitionDate;
	}

	public String getPrivateComment() {
		return privateComment;
	}

	public long getPrivateInfoTimestamp() {
		return privateInfoTimestamp;
	}

	public long getStatusTimestamp() {
		return statusTimestamp;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public int getYear() {
		return year;
	}

	public String getCondition() {
		return condition;
	}

	public String getWantParts() {
		return wantParts;
	}

	public String getHasParts() {
		return hasParts;
	}

	public int getWishlistPriority() {
		return wishlistPriority;
	}

	public String getWishlistComment() {
		return wishlistComment;
	}

	public int getNumberOfPlays() {
		return numPlays;
	}

	public boolean isOwn() {
		return own;
	}

	public boolean isPreviouslyOwned() {
		return previouslyOwned;
	}

	public boolean isWantToBuy() {
		return wantToBuy;
	}

	public boolean isWantToPlay() {
		return wantToPlay;
	}

	public boolean isPreordered() {
		return preordered;
	}

	public boolean isWantInTrade() {
		return wantInTrade;
	}

	public boolean isForTrade() {
		return forTrade;
	}

	public boolean isWishlist() {
		return wishlist;
	}

	public long getDirtyTimestamp() {
		return dirtyTimestamp;
	}

	public long getWishlistCommentDirtyTimestamp() {
		return wishlistCommentDirtyTimestamp;
	}

	public long getTradeConditionDirtyTimestamp() {
		return tradeConditionDirtyTimestamp;
	}

	public long getWantPartsDirtyTimestamp() {
		return wantPartsDirtyTimestamp;
	}

	public long getHasPartsDirtyTimestamp() {
		return hasPartsDirtyTimestamp;
	}
}
