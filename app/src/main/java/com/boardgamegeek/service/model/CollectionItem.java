package com.boardgamegeek.service.model;

import android.database.Cursor;

import com.boardgamegeek.provider.BggContract.Collection;

public class CollectionItem {
	public static final String[] PROJECTION = {
		Collection._ID,
		Collection.GAME_ID,
		Collection.COLLECTION_ID,
		Collection.COLLECTION_NAME,
		Collection.RATING,
		Collection.RATING_DIRTY_TIMESTAMP,
		Collection.COMMENT,
		Collection.COMMENT_DIRTY_TIMESTAMP,
		Collection.PRIVATE_INFO_ACQUIRED_FROM,
		Collection.PRIVATE_INFO_ACQUISITION_DATE,
		Collection.PRIVATE_INFO_COMMENT,
		Collection.PRIVATE_INFO_CURRENT_VALUE,
		Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY,
		Collection.PRIVATE_INFO_PRICE_PAID,
		Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY,
		Collection.PRIVATE_INFO_QUANTITY,
		Collection.PRIVATE_INFO_DIRTY_TIMESTAMP,
		Collection.STATUS_OWN,
		Collection.STATUS_PREVIOUSLY_OWNED,
		Collection.STATUS_FOR_TRADE,
		Collection.STATUS_WANT,
		Collection.STATUS_WANT_TO_PLAY,
		Collection.STATUS_WANT_TO_BUY,
		Collection.STATUS_WISHLIST,
		Collection.STATUS_WISHLIST_PRIORITY,
		Collection.STATUS_PREORDERED,
		Collection.STATUS_DIRTY_TIMESTAMP,
		Collection.WISHLIST_COMMENT,
		Collection.WISHLIST_COMMENT_DIRTY_TIMESTAMP,
		Collection.CONDITION,
		Collection.TRADE_CONDITION_DIRTY_TIMESTAMP,
		Collection.WANTPARTS_LIST,
		Collection.WANT_PARTS_DIRTY_TIMESTAMP,
		Collection.HASPARTS_LIST,
		Collection.HAS_PARTS_DIRTY_TIMESTAMP,
		Collection.IMAGE_URL,
		Collection.THUMBNAIL_URL
	};

	private static final int _ID = 0;
	private static final int GAME_ID = 1;
	private static final int COLLECTION_ID = 2;
	private static final int COLLECTION_NAME = 3;
	private static final int RATING = 4;
	private static final int RATING_DIRTY_TIMESTAMP = 5;
	private static final int COMMENT = 6;
	private static final int COMMENT_DIRTY_TIMESTAMP = 7;
	private static final int PRIVATE_INFO_ACQUIRED_FROM = 8;
	private static final int PRIVATE_INFO_ACQUISITION_DATE = 9;
	private static final int PRIVATE_INFO_COMMENT = 10;
	private static final int PRIVATE_INFO_CURRENT_VALUE = 11;
	private static final int PRIVATE_INFO_CURRENT_VALUE_CURRENCY = 12;
	private static final int PRIVATE_INFO_PRICE_PAID = 13;
	private static final int PRIVATE_INFO_PRICE_PAID_CURRENCY = 14;
	private static final int PRIVATE_INFO_QUANTITY = 15;
	private static final int PRIVATE_INFO_DIRTY_TIMESTAMP = 16;
	private static final int STATUS_OWN = 17;
	private static final int STATUS_PREVIOUSLY_OWNED = 18;
	private static final int STATUS_FOR_TRADE = 19;
	private static final int STATUS_WANT = 20;
	private static final int STATUS_WANT_TO_PLAY = 21;
	private static final int STATUS_WANT_TO_BUY = 22;
	private static final int STATUS_WISHLIST = 23;
	private static final int STATUS_WISHLIST_PRIORITY = 24;
	private static final int STATUS_PREORDERED = 25;
	private static final int STATUS_DIRTY_TIMESTAMP = 26;
	private static final int WISHLIST_COMMENT = 27;
	private static final int WISHLIST_COMMENT_DIRTY_TIMESTAMP = 28;
	private static final int CONDITION = 29;
	private static final int TRADE_CONDITION_DIRTY_TIMESTAMP = 30;
	private static final int WANTPARTS_LIST = 31;
	private static final int WANT_PARTS_DIRTY_TIMESTAMP = 32;
	private static final int HASPARTS_LIST = 33;
	private static final int HAS_PARTS_DIRTY_TIMESTAMP = 34;
	private static final int IMAGE_URL = 35;
	private static final int THUMBNAIL_URL = 36;

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
	private String wishlistComment;
	private long wishlistCommentDirtyTimestamp;
	private String tradeCondition;
	private long tradeConditionDirtyTimestamp;
	private String wantParts;
	private long wantPartsDirtyTimestamp;
	private String hasParts;
	private long hasPartsDirtyTimestamp;
	public String imageUrl;
	public String thumbnailUrl;

	public static CollectionItem fromCursor(Cursor cursor) {
		CollectionItem collectionItem = new CollectionItem();
		collectionItem.internalId = cursor.getLong(_ID);
		collectionItem.collectionId = cursor.getInt(COLLECTION_ID);
		collectionItem.gameId = cursor.getInt(GAME_ID);
		collectionItem.collectionName = cursor.getString(COLLECTION_NAME);
		collectionItem.imageUrl = cursor.getString(IMAGE_URL);
		collectionItem.thumbnailUrl = cursor.getString(THUMBNAIL_URL);

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

		collectionItem.wishlistComment = cursor.getString(WISHLIST_COMMENT);
		collectionItem.wishlistCommentDirtyTimestamp = cursor.getLong(WISHLIST_COMMENT_DIRTY_TIMESTAMP);
		collectionItem.tradeCondition = cursor.getString(CONDITION);
		collectionItem.tradeConditionDirtyTimestamp = cursor.getLong(TRADE_CONDITION_DIRTY_TIMESTAMP);
		collectionItem.wantParts = cursor.getString(WANTPARTS_LIST);
		collectionItem.wantPartsDirtyTimestamp = cursor.getLong(WANT_PARTS_DIRTY_TIMESTAMP);
		collectionItem.hasParts = cursor.getString(HASPARTS_LIST);
		collectionItem.hasPartsDirtyTimestamp = cursor.getLong(HAS_PARTS_DIRTY_TIMESTAMP);

		return collectionItem;
	}

	public int getCollectionId() {
		return collectionId;
	}

	public long getInternalId() {
		return internalId;
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

	public String getWishlistComment() {
		return wishlistComment == null ? "" : wishlistComment;
	}

	public long getWishlistCommentDirtyTimestamp() {
		return wishlistCommentDirtyTimestamp;
	}

	public String getTradeCondition() {
		return tradeCondition == null ? "" : tradeCondition;
	}

	public long getTradeConditionDirtyTimestamp() {
		return tradeConditionDirtyTimestamp;
	}

	public String getWantParts() {
		return wantParts == null ? "" : wantParts;
	}

	public long getWantPartsDirtyTimestamp() {
		return wantPartsDirtyTimestamp;
	}

	public String getHasParts() {
		return hasParts == null ? "" : hasParts;
	}

	public long getHasPartsDirtyTimestamp() {
		return hasPartsDirtyTimestamp;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}
}
