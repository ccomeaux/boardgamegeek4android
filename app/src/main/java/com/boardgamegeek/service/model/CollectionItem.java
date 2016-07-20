package com.boardgamegeek.service.model;

import android.database.Cursor;

import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Collection;

public class CollectionItem {
	public static String[] PROJECTION = {
		BggContract.Collection._ID,
		BggContract.Collection.GAME_ID,
		BggContract.Collection.COLLECTION_ID,
		BggContract.Collection.COLLECTION_NAME,
		BggContract.Collection.RATING,
		BggContract.Collection.RATING_DIRTY_TIMESTAMP,
		BggContract.Collection.COMMENT,
		BggContract.Collection.COMMENT_DIRTY_TIMESTAMP,
		BggContract.Collection.PRIVATE_INFO_ACQUIRED_FROM,
		BggContract.Collection.PRIVATE_INFO_ACQUISITION_DATE,
		BggContract.Collection.PRIVATE_INFO_COMMENT,
		BggContract.Collection.PRIVATE_INFO_CURRENT_VALUE,
		BggContract.Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY,
		BggContract.Collection.PRIVATE_INFO_PRICE_PAID,
		BggContract.Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY,
		BggContract.Collection.PRIVATE_INFO_QUANTITY,
		BggContract.Collection.PRIVATE_INFO_DIRTY_TIMESTAMP,
		Collection.STATUS_OWN,
		Collection.STATUS_PREVIOUSLY_OWNED,
		Collection.STATUS_FOR_TRADE,
		Collection.STATUS_WANT,
		Collection.STATUS_WANT_TO_PLAY,
		Collection.STATUS_WANT_TO_BUY,
		Collection.STATUS_WISHLIST,
		Collection.STATUS_WISHLIST_PRIORITY,
		Collection.STATUS_PREORDERED,
		Collection.STATUS_DIRTY_TIMESTAMP
	};

	private static int _ID = 0;
	private static int GAME_ID = 1;
	private static int COLLECTION_ID = 2;
	private static int COLLECTION_NAME = 3;
	private static int RATING = 4;
	private static int RATING_DIRTY_TIMESTAMP = 5;
	private static int COMMENT = 6;
	private static int COMMENT_DIRTY_TIMESTAMP = 7;
	private static int PRIVATE_INFO_ACQUIRED_FROM = 8;
	private static int PRIVATE_INFO_ACQUISITION_DATE = 9;
	private static int PRIVATE_INFO_COMMENT = 10;
	private static int PRIVATE_INFO_CURRENT_VALUE = 11;
	private static int PRIVATE_INFO_CURRENT_VALUE_CURRENCY = 12;
	private static int PRIVATE_INFO_PRICE_PAID = 13;
	private static int PRIVATE_INFO_PRICE_PAID_CURRENCY = 14;
	private static int PRIVATE_INFO_QUANTITY = 15;
	private static int PRIVATE_INFO_DIRTY_TIMESTAMP = 16;
	private static int STATUS_OWN = 17;
	private static int STATUS_PREVIOUSLY_OWNED = 18;
	private static int STATUS_FOR_TRADE = 19;
	private static int STATUS_WANT = 20;
	private static int STATUS_WANT_TO_PLAY = 21;
	private static int STATUS_WANT_TO_BUY = 22;
	private static int STATUS_WISHLIST = 23;
	private static int STATUS_WISHLIST_PRIORITY = 24;
	private static int STATUS_PREORDERED = 25;
	private static int STATUS_DIRTY_TIMESTAMP = 26;

	private long internalId;
	private int collectionId;
	private int gameId;
	private String collectionName;
	private double rating;
	private long ratingTimestamp;
	private String comment;
	private long commentTimestamp;
	private String acquiredFrom;
	private String acquisitionDate;
	private String privateComment;
	private double currentValue;
	private String currentValueCurrency;
	private double pricePaid;
	private String pricePaidCurrency;
	private int quantity;
	private long privateInfoTimestamp;
	private boolean owned;
	private boolean previouslyOwned;
	private boolean forTrade;
	private boolean wantInTrade;
	private boolean wantToBuy;
	private boolean wishlist;
	private int wishlistPriority;
	private boolean wantToPlay;
	private boolean preordered;
	private long statusTimestamp;

	public static CollectionItem fromCursor(Cursor cursor) {
		CollectionItem collectionItem = new CollectionItem();
		collectionItem.internalId = cursor.getLong(_ID);
		collectionItem.collectionId = cursor.getInt(COLLECTION_ID);
		collectionItem.gameId = cursor.getInt(GAME_ID);
		collectionItem.collectionName = cursor.getString(COLLECTION_NAME);

		collectionItem.rating = cursor.getDouble(RATING);
		collectionItem.ratingTimestamp = cursor.getLong(RATING_DIRTY_TIMESTAMP);

		collectionItem.comment = cursor.getString(COMMENT);
		collectionItem.commentTimestamp = cursor.getLong(COMMENT_DIRTY_TIMESTAMP);

		collectionItem.acquiredFrom = cursor.getString(PRIVATE_INFO_ACQUIRED_FROM);
		collectionItem.acquisitionDate = cursor.getString(PRIVATE_INFO_ACQUISITION_DATE);
		collectionItem.privateComment = cursor.getString(PRIVATE_INFO_COMMENT);
		collectionItem.currentValue = cursor.getDouble(PRIVATE_INFO_CURRENT_VALUE);
		collectionItem.currentValueCurrency = cursor.getString(PRIVATE_INFO_CURRENT_VALUE_CURRENCY);
		collectionItem.pricePaid = cursor.getDouble(PRIVATE_INFO_PRICE_PAID);
		collectionItem.pricePaidCurrency = cursor.getString(PRIVATE_INFO_PRICE_PAID_CURRENCY);
		collectionItem.quantity = cursor.getInt(PRIVATE_INFO_QUANTITY);
		collectionItem.privateInfoTimestamp = cursor.getLong(PRIVATE_INFO_DIRTY_TIMESTAMP);

		collectionItem.owned = cursor.getInt(STATUS_OWN) == 1;
		collectionItem.previouslyOwned = cursor.getInt(STATUS_PREVIOUSLY_OWNED) == 1;
		collectionItem.forTrade = cursor.getInt(STATUS_FOR_TRADE) == 1;
		collectionItem.wantInTrade = cursor.getInt(STATUS_WANT) == 1;
		collectionItem.wantToBuy = cursor.getInt(STATUS_WANT_TO_BUY) == 1;
		collectionItem.wantToPlay = cursor.getInt(STATUS_WANT_TO_PLAY) == 1;
		collectionItem.preordered = cursor.getInt(STATUS_PREORDERED) == 1;
		collectionItem.wishlist = cursor.getInt(STATUS_WISHLIST) == 1;
		collectionItem.wishlistPriority = cursor.getInt(STATUS_WISHLIST_PRIORITY);
		collectionItem.statusTimestamp = cursor.getLong(STATUS_DIRTY_TIMESTAMP);

		return collectionItem;
	}

	public int getCollectionId() {
		return collectionId;
	}

	public long getInternalId() {
		return internalId;
	}

	public void setCollectionId(int collectionId) {
		this.collectionId = collectionId;
	}

	public int getGameId() {
		return gameId;
	}

	public String getCollectionName() {
		return collectionName;
	}

	public double getRating() {
		return rating;
	}

	public long getRatingTimestamp() {
		return ratingTimestamp;
	}

	public String getComment() {
		return comment == null ? "" : comment;
	}

	public long getCommentTimestamp() {
		return commentTimestamp;
	}

	public String getAcquiredFrom() {
		return acquiredFrom == null ? "" : acquiredFrom;
	}

	public String getAcquisitionDate() {
		return acquisitionDate == null ? "" : acquisitionDate;
	}

	public String getPrivateComment() {
		return privateComment == null ? "" : privateComment;
	}

	public double getCurrentValue() {
		return currentValue;
	}

	public String getCurrentValueCurrency() {
		return currentValueCurrency == null ? "" : currentValueCurrency;
	}

	public double getPricePaid() {
		return pricePaid;
	}

	public String getPricePaidCurrency() {
		return pricePaidCurrency == null ? "" : pricePaidCurrency;
	}

	public int getQuantity() {
		return quantity;
	}

	public long getPrivateInfoTimestamp() {
		return privateInfoTimestamp;
	}

	public boolean owned() {
		return owned;
	}

	public boolean previouslyOwned() {
		return previouslyOwned;
	}

	public boolean forTrade() {
		return forTrade;
	}

	public boolean wantInTrade() {
		return wantInTrade;
	}

	public boolean wantToBuy() {
		return wantToBuy;
	}

	public boolean wishlist() {
		return wishlist;
	}

	public int wishlistPriority() {
		return wishlistPriority;
	}

	public boolean wantToPlay() {
		return wantToPlay;
	}

	public boolean preordered() {
		return preordered;
	}

	public long getStatusTimestamp() {
		return statusTimestamp;
	}
}
