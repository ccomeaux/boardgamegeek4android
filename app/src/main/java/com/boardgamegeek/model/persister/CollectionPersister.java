package com.boardgamegeek.model.persister;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import hugo.weaving.DebugLog;
import timber.log.Timber;

public class CollectionPersister {
	private static final int NOT_DIRTY = 0;
	private static final int MAXIMUM_BATCH_SIZE = 100;
	private final Context context;
	private final ContentResolver resolver;
	private final long updateTime;
	private boolean isBriefSync;
	private boolean includePrivateInfo;
	private boolean includeStats;
	private final List<Integer> persistedGameIds;
	private List<String> statusesToSync;

	@DebugLog
	public CollectionPersister(Context context) {
		this.context = context;
		resolver = this.context.getContentResolver();
		updateTime = System.currentTimeMillis();
		persistedGameIds = new ArrayList<>();
	}

	@DebugLog
	public long getInitialTimestamp() {
		return updateTime;
	}

	@DebugLog
	public CollectionPersister brief() {
		isBriefSync = true;
		return this;
	}

	@DebugLog
	public CollectionPersister includePrivateInfo() {
		includePrivateInfo = true;
		return this;
	}

	@DebugLog
	public CollectionPersister includeStats() {
		includeStats = true;
		return this;
	}

	@DebugLog
	public CollectionPersister validStatusesOnly() {
		String[] syncStatuses = PreferencesUtils.getSyncStatuses(context);
		statusesToSync = Arrays.asList(syncStatuses);
		return this;
	}

	@DebugLog
	public int delete(List<CollectionItem> items, int gameId) {
		if (items == null || items.size() == 0) {
			return 0;
		}

		// determine the collection IDs that are no longer in the collection
		List<Integer> collectionIds = ResolverUtils.queryInts(resolver, Collection.CONTENT_URI,
			Collection.COLLECTION_ID, "collection." + Collection.GAME_ID + "=?",
			new String[] { String.valueOf(gameId) });
		for (CollectionItem item : items) {
			collectionIds.remove(Integer.valueOf(item.collectionId()));
		}

		// remove them
		if (collectionIds.size() > 0) {
			ArrayList<ContentProviderOperation> batch = new ArrayList<>();
			for (Integer collectionId : collectionIds) {
				batch.add(ContentProviderOperation.newDelete(Collection.CONTENT_URI)
					.withSelection(Collection.COLLECTION_ID + "=?", new String[] { String.valueOf(collectionId) })
					.build());
			}
			ResolverUtils.applyBatch(context, batch);
		}

		return collectionIds.size();
	}

	@DebugLog
	public int save(List<CollectionItem> items) {
		if (items != null && items.size() > 0) {
			int recordCount = 0;
			ArrayList<ContentProviderOperation> batch = new ArrayList<>();
			persistedGameIds.clear();
			for (CollectionItem item : items) {
				if (isSetToSync(item)) {
					saveGame(toGameValues(item), batch);
					saveCollectionItem(toCollectionValues(item), batch);
					Timber.d("Batched game %s [%s]; collection [%s]", item.gameName(), item.gameId, item.collectionId());
				} else {
					Timber.d("Skipped invalid game %s [%s]; collection [%s]", item.gameName(), item.gameId, item.collectionId());
				}
				// To prevent timing out during a sync, periodically save the batch
				if (batch.size() >= MAXIMUM_BATCH_SIZE) {
					recordCount += processBatch(batch, context);
				}
			}
			recordCount += processBatch(batch, context);
			Timber.i("Saved " + items.size() + " collection items");
			return recordCount;
		}
		return 0;
	}

	private int processBatch(ArrayList<ContentProviderOperation> batch, Context context) {
		if (batch != null && batch.size() > 0) {
			ContentProviderResult[] results = ResolverUtils.applyBatch(context, batch);
			Timber.i("Saved a batch of %d records", results.length);
			batch.clear();
			persistedGameIds.clear();
			return results.length;
		} else {
			Timber.i("No batch to save");
		}
		return 0;
	}

	@DebugLog
	private boolean isSetToSync(CollectionItem item) {
		if (statusesToSync == null) {
			return true;
		}
		if (item.own.equals("1") && statusesToSync.contains("own")) {
			return true;
		}
		if (item.prevowned.equals("1") && statusesToSync.contains("prevowned")) {
			return true;
		}
		if (item.fortrade.equals("1") && statusesToSync.contains("fortrade")) {
			return true;
		}
		if (item.want.equals("1") && statusesToSync.contains("want")) {
			return true;
		}
		if (item.wanttoplay.equals("1") && statusesToSync.contains("wanttoplay")) {
			return true;
		}
		if (item.wanttobuy.equals("1") && statusesToSync.contains("wanttobuy")) {
			return true;
		}
		if (item.wishlist.equals("1") && statusesToSync.contains("wishlist")) {
			return true;
		}
		//noinspection RedundantIfStatement
		if (item.preordered.equals("1") && statusesToSync.contains("preordered")) {
			return true;
		}
		return false;
	}

	@DebugLog
	private ContentValues toGameValues(CollectionItem item) {
		ContentValues values = new ContentValues();
		values.put(Games.UPDATED_LIST, updateTime);
		values.put(Games.GAME_ID, item.gameId);
		values.put(Games.GAME_NAME, item.gameName());
		values.put(Games.GAME_SORT_NAME, item.gameSortName());
		if (!isBriefSync) {
			values.put(Games.NUM_PLAYS, item.numplays);
		}
		if (includeStats) {
			values.put(Games.MIN_PLAYERS, item.statistics.minplayers);
			values.put(Games.MAX_PLAYERS, item.statistics.maxplayers);
			values.put(Games.PLAYING_TIME, item.statistics.playingtime);
			values.put(Games.STATS_NUMBER_OWNED, item.statistics.numberOwned());
		}
		return values;
	}

	@DebugLog
	private ContentValues toCollectionValues(CollectionItem item) {
		ContentValues values = new ContentValues();
		if (!isBriefSync && includePrivateInfo && includeStats) {
			values.put(Collection.UPDATED, updateTime);
		}
		values.put(Collection.UPDATED_LIST, updateTime);
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
		if (!isBriefSync) {
			values.put(Collection.COLLECTION_YEAR_PUBLISHED, item.yearpublished);
			values.put(Collection.COLLECTION_IMAGE_URL, item.image);
			values.put(Collection.COLLECTION_THUMBNAIL_URL, item.thumbnail);
			values.put(Collection.COMMENT, item.comment);
			values.put(Collection.WANTPARTS_LIST, item.wantpartslist);
			values.put(Collection.CONDITION, item.conditiontext);
			values.put(Collection.HASPARTS_LIST, item.haspartslist);
			values.put(Collection.WISHLIST_COMMENT, item.wishlistcomment);
		}
		if (includePrivateInfo) {
			values.put(Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY, item.pricePaidCurrency);
			values.put(Collection.PRIVATE_INFO_PRICE_PAID, item.pricePaid());
			values.put(Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY, item.currentValueCurrency);
			values.put(Collection.PRIVATE_INFO_CURRENT_VALUE, item.currentValue());
			values.put(Collection.PRIVATE_INFO_QUANTITY, item.getQuantity());
			values.put(Collection.PRIVATE_INFO_ACQUISITION_DATE, item.acquisitionDate);
			values.put(Collection.PRIVATE_INFO_ACQUIRED_FROM, item.acquiredFrom);
			values.put(Collection.PRIVATE_INFO_COMMENT, item.privatecomment);
		}
		if (includeStats) {
			values.put(Collection.RATING, item.statistics.getRating());
		}
		return values;
	}

	@DebugLog
	private void saveGame(ContentValues values, ArrayList<ContentProviderOperation> batch) {
		int gameId = values.getAsInteger(Games.GAME_ID);
		if (persistedGameIds.contains(gameId)) {
			Timber.i("Already saved game [ID=" + gameId + "; NAME=" + values.getAsString(Games.GAME_NAME) + "]");
		} else {
			Builder cpo;
			Uri uri = Games.buildGameUri(gameId);
			if (ResolverUtils.rowExists(resolver, uri)) {
				values.remove(Games.GAME_ID);
				cpo = ContentProviderOperation.newUpdate(uri);
			} else {
				cpo = ContentProviderOperation.newInsert(Games.CONTENT_URI);
			}
			batch.add(cpo.withValues(values).build());
			persistedGameIds.add(gameId);
		}
	}

	@DebugLog
	private void saveCollectionItem(ContentValues values, ArrayList<ContentProviderOperation> batch) {
		int collectionId = values.getAsInteger(Collection.COLLECTION_ID);
		int gameId = values.getAsInteger(Collection.GAME_ID);

		if (collectionId == BggContract.INVALID_ID) {
			values.remove(Collection.COLLECTION_ID);
		}

		long internalId = getCollectionItemInternalId(collectionId, gameId);

		Builder operation;
		if (internalId != BggContract.INVALID_ID) {
			removeValuesIfDirty(values, internalId, Collection.RATING_DIRTY_TIMESTAMP, Collection.RATING);
			removeValuesIfDirty(values, internalId, Collection.COMMENT_DIRTY_TIMESTAMP, Collection.COMMENT);
			Uri uri = Collection.buildUri(internalId);
			operation = ContentProviderOperation.newUpdate(uri);
			maybeDeleteThumbnail(values, uri, batch);
		} else {
			// insert
			operation = ContentProviderOperation.newInsert(Collection.CONTENT_URI);
		}
		batch.add(operation.withValues(values).withYieldAllowed(true).build());
	}

	@DebugLog
	private long getCollectionItemInternalId(int collectionId, int gameId) {
		long internalId;
		if (collectionId == BggContract.INVALID_ID) {
			internalId = ResolverUtils.queryLong(resolver,
				Collection.CONTENT_URI,
				Collection._ID,
				BggContract.INVALID_ID,
				"collection." + Collection.GAME_ID + "=? AND " + Collection.COLLECTION_ID + " IS NULL",
				new String[] { String.valueOf(gameId) });
		} else {
			internalId = ResolverUtils.queryLong(resolver,
				Collection.CONTENT_URI,
				Collection._ID,
				BggContract.INVALID_ID,
				Collection.COLLECTION_ID + "=?",
				new String[] { String.valueOf(collectionId) });
		}
		return internalId;
	}

	@DebugLog
	private void maybeDeleteThumbnail(ContentValues values, Uri uri, ArrayList<ContentProviderOperation> batch) {
		if (isBriefSync) {
			// thumbnail not returned in brief mode
			return;
		}

		String newThumbnailUrl = values.getAsString(Collection.COLLECTION_THUMBNAIL_URL);
		if (newThumbnailUrl == null) {
			newThumbnailUrl = "";
		}

		String oldThumbnailUrl = ResolverUtils.queryString(resolver, uri, Collection.COLLECTION_THUMBNAIL_URL);
		if (newThumbnailUrl.equals(oldThumbnailUrl)) {
			// nothing to do - thumbnail hasn't changed
			return;
		}

		String thumbnailFileName = FileUtils.getFileNameFromUrl(oldThumbnailUrl);
		if (!TextUtils.isEmpty(thumbnailFileName)) {
			batch.add(ContentProviderOperation.newDelete(Thumbnails.buildUri(thumbnailFileName)).build());
		}
	}

	@DebugLog
	private void removeValuesIfDirty(ContentValues values, long internalId, String commentDirtyTimestamp, String... columns) {
		if (getDirtyTimestamp(internalId, commentDirtyTimestamp) != NOT_DIRTY) {
			for (String column : columns) {
				values.remove(column);
			}
		}
	}

	@DebugLog
	private int getDirtyTimestamp(long internalId, String columnName) {
		if (internalId == BggContract.INVALID_ID) {
			return NOT_DIRTY;
		}
		return ResolverUtils.queryInt(resolver, Collection.buildUri(internalId), columnName, NOT_DIRTY);
	}
}
