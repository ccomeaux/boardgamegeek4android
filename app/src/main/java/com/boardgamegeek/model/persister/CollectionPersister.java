package com.boardgamegeek.model.persister;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.boardgamegeek.model.CollectionItem;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Thumbnails;
import com.boardgamegeek.util.FileUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.ResolverUtils;

import timber.log.Timber;

public class CollectionPersister {
	private Context mContext;
	private ContentResolver mResolver;
	private long mUpdateTime;
	private boolean mBrief;
	private boolean mIncludePrivateInfo;
	private boolean mIncludeStats;
	private List<Integer> mGameIds;
	private List<String> mSyncStatuses;

	public CollectionPersister(Context context) {
		mContext = context;
		mResolver = mContext.getContentResolver();
		mUpdateTime = System.currentTimeMillis();
		mGameIds = new ArrayList<Integer>();
	}

	public long getTimeStamp() {
		return mUpdateTime;
	}

	public CollectionPersister brief() {
		mBrief = true;
		return this;
	}

	public CollectionPersister includePrivateInfo() {
		mIncludePrivateInfo = true;
		return this;
	}

	public CollectionPersister includeStats() {
		mIncludeStats = true;
		return this;
	}

	public CollectionPersister validStatusesOnly() {
		String[] syncStatuses = PreferencesUtils.getSyncStatuses(mContext);
		mSyncStatuses = Arrays.asList(syncStatuses);
		return this;
	}

	public int delete(List<CollectionItem> items, int gameId) {
		if (items == null || items.size() == 0) {
			return 0;
		}

		// determine the collection IDs that are no longer in the collection
		List<Integer> collectionIds = ResolverUtils.queryInts(mResolver, Collection.CONTENT_URI,
			Collection.COLLECTION_ID, "collection." + Collection.GAME_ID + "=?",
			new String[] { String.valueOf(gameId) });
		for (CollectionItem item : items) {
			collectionIds.remove(Integer.valueOf(item.collectionId()));
		}

		// remove them
		if (collectionIds.size() > 0) {
			ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
			for (Integer collectionId : collectionIds) {
				batch.add(ContentProviderOperation.newDelete(Collection.CONTENT_URI)
					.withSelection(Collection.COLLECTION_ID + "=?", new String[] { String.valueOf(collectionId) })
					.build());
			}
			ResolverUtils.applyBatch(mContext, batch);
		}

		return collectionIds.size();
	}

	public int save(List<CollectionItem> items) {
		if (items != null && items.size() > 0) {
			ArrayList<ContentProviderOperation> batch = new ArrayList<>();
			mGameIds.clear();
			for (CollectionItem item : items) {
				if (isValid(item)) {
					insertOrUpdateGame(toGameValues(item), batch);
					insertOrUpdateCollection(toCollectionValues(item), batch);
					Timber.i("Batched game ID=" + item.gameId + "; collection ID=" + item.collectionId());
				} else {
					Timber.i("Skipped invalid game ID=" + item.gameId + "; collection ID=" + item.collectionId());
				}
			}
			ContentProviderResult[] result = ResolverUtils.applyBatch(mContext, batch);
			Timber.i("Saved " + items.size() + " collection items");
			return result.length;
		}
		return 0;
	}

	private boolean isValid(CollectionItem item) {
		if (mSyncStatuses == null) {
			return true;
		}
		if (item.own.equals("1") && mSyncStatuses.contains("own")) {
			return true;
		}
		if (item.prevowned.equals("1") && mSyncStatuses.contains("prevowned")) {
			return true;
		}
		if (item.fortrade.equals("1") && mSyncStatuses.contains("fortrade")) {
			return true;
		}
		if (item.want.equals("1") && mSyncStatuses.contains("want")) {
			return true;
		}
		if (item.wanttoplay.equals("1") && mSyncStatuses.contains("wanttoplay")) {
			return true;
		}
		if (item.wanttobuy.equals("1") && mSyncStatuses.contains("wanttobuy")) {
			return true;
		}
		if (item.wishlist.equals("1") && mSyncStatuses.contains("wishlist")) {
			return true;
		}
		if (item.preordered.equals("1") && mSyncStatuses.contains("preordered")) {
			return true;
		}
		return false;
	}

	private ContentValues toGameValues(CollectionItem item) {
		ContentValues values = new ContentValues();
		values.put(Games.UPDATED_LIST, mUpdateTime);
		values.put(Games.GAME_ID, item.gameId);
		values.put(Games.GAME_NAME, item.gameName());
		values.put(Games.GAME_SORT_NAME, item.gameSortName());
		if (!mBrief) {
			values.put(Games.NUM_PLAYS, item.numplays);
		}
		if (mIncludeStats) {
			values.put(Games.MIN_PLAYERS, item.statistics.minplayers);
			values.put(Games.MAX_PLAYERS, item.statistics.maxplayers);
			values.put(Games.PLAYING_TIME, item.statistics.playingtime);
			values.put(Games.STATS_NUMBER_OWNED, item.statistics.numberOwned());
		}
		return values;
	}

	private ContentValues toCollectionValues(CollectionItem item) {
		ContentValues values = new ContentValues();
		if (!mBrief && mIncludePrivateInfo && mIncludeStats) {
			values.put(Collection.UPDATED, mUpdateTime);
		}
		values.put(Collection.UPDATED_LIST, mUpdateTime);
		values.put(Collection.GAME_ID, item.gameId);
		values.put(Collection.COLLECTION_ID, item.collectionId());
		values.put(Collection.COLLECTION_NAME, item.collectionName());
		values.put(Collection.COLLECTION_SORT_NAME, item.collectionSortName());
		values.put(Collection.STATUS_OWN, item.own);
		values.put(Collection.STATUS_PREVIOUSLY_OWNED, item.prevowned);
		values.put(Collection.STATUS_FOR_TRADE, item.fortrade);
		values.put(Collection.STATUS_WANT, item.want);
		values.put(Collection.STATUS_WANT_TO_PLAY, item.wanttoplay);
		values.put(Collection.STATUS_WANT_TO_BUY, item.wanttobuy);
		values.put(Collection.STATUS_WISHLIST, item.wishlist);
		values.put(Collection.STATUS_WISHLIST_PRIORITY, item.wishlistpriority);
		values.put(Collection.STATUS_PREORDERED, item.preordered);
		values.put(Collection.LAST_MODIFIED, item.lastModifiedDate());
		if (!mBrief) {
			values.put(Collection.COLLECTION_YEAR_PUBLISHED, item.yearpublished);
			values.put(Collection.COLLECTION_IMAGE_URL, item.image);
			values.put(Collection.COLLECTION_THUMBNAIL_URL, item.thumbnail);
			values.put(Collection.COMMENT, item.comment);
			values.put(Collection.WANTPARTS_LIST, item.wantpartslist);
			values.put(Collection.CONDITION, item.conditiontext);
			values.put(Collection.HASPARTS_LIST, item.haspartslist);
			values.put(Collection.WISHLIST_COMMENT, item.wishlistcomment);
		}
		if (mIncludePrivateInfo) {
			values.put(Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY, item.pricePaidCurrency);
			values.put(Collection.PRIVATE_INFO_PRICE_PAID, item.pricePaid());
			values.put(Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY, item.currentValueCurrency);
			values.put(Collection.PRIVATE_INFO_CURRENT_VALUE, item.currentValue());
			values.put(Collection.PRIVATE_INFO_QUANTITY, item.getQuantity());
			values.put(Collection.PRIVATE_INFO_ACQUISITION_DATE, item.acquisitionDate);
			values.put(Collection.PRIVATE_INFO_ACQUIRED_FROM, item.acquiredFrom);
			values.put(Collection.PRIVATE_INFO_COMMENT, item.privatecomment);
		}
		if (mIncludeStats) {
			values.put(Collection.RATING, item.statistics.getRating());
		}
		return values;
	}

	private void insertOrUpdateGame(ContentValues values, ArrayList<ContentProviderOperation> batch) {
		int gameId = values.getAsInteger(Games.GAME_ID);
		if (mGameIds.contains(gameId)) {
			Timber.i("Already saved game [ID=" + gameId + "; NAME=" + values.getAsString(Games.GAME_NAME) + "]");
		} else {
			Builder cpo = null;
			Uri uri = Games.buildGameUri(gameId);
			if (ResolverUtils.rowExists(mResolver, uri)) {
				values.remove(Games.GAME_ID);
				cpo = ContentProviderOperation.newUpdate(uri);
			} else {
				cpo = ContentProviderOperation.newInsert(Games.CONTENT_URI);
			}
			batch.add(cpo.withValues(values).build());
			mGameIds.add(gameId);
		}
	}

	private void insertOrUpdateCollection(ContentValues values, ArrayList<ContentProviderOperation> batch) {
		Builder cpo = null;
		long existingId = BggContract.INVALID_ID;
		int collId = values.getAsInteger(Collection.COLLECTION_ID);
		if (collId == BggContract.INVALID_ID) {
			values.remove(Collection.COLLECTION_ID);
			existingId = ResolverUtils.queryLong(mResolver, Collection.CONTENT_URI, Collection._ID,
				BggContract.INVALID_ID, "collection." + Collection.GAME_ID + "=? AND " + Collection.COLLECTION_ID
					+ " IS NULL", new String[] { values.getAsString(Collection.GAME_ID) });
		} else {
			existingId = ResolverUtils.queryLong(mResolver, Collection.CONTENT_URI, Collection._ID,
				BggContract.INVALID_ID, Collection.COLLECTION_ID + "=?", new String[] { String.valueOf(collId) });
		}
		if (existingId != BggContract.INVALID_ID) {
			Uri uri = Collection.buildUri(existingId);
			cpo = ContentProviderOperation.newUpdate(uri);
			maybeDeleteThumbnail(values, uri, batch);
		} else {
			cpo = ContentProviderOperation.newInsert(Collection.CONTENT_URI);
		}
		batch.add(cpo.withValues(values).withYieldAllowed(true).build());
	}

	private void maybeDeleteThumbnail(ContentValues values, Uri uri, ArrayList<ContentProviderOperation> batch) {
		if (mBrief) {
			// thumbnail not returned in brief mode
			return;
		}

		String newThumbnailUrl = values.getAsString(Collection.COLLECTION_THUMBNAIL_URL);
		if (newThumbnailUrl == null) {
			newThumbnailUrl = "";
		}

		String oldThumbnailUrl = ResolverUtils.queryString(mResolver, uri, Collection.COLLECTION_THUMBNAIL_URL);
		if (newThumbnailUrl.equals(oldThumbnailUrl)) {
			// nothing to do - thumbnail hasn't changed
			return;
		}

		String thumbnailFileName = FileUtils.getFileNameFromUrl(oldThumbnailUrl);
		if (!TextUtils.isEmpty(thumbnailFileName)) {
			batch.add(ContentProviderOperation.newDelete(Thumbnails.buildUri(thumbnailFileName)).build());
		}
	}
}
