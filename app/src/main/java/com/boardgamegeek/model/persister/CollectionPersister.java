package com.boardgamegeek.model.persister;

import android.content.ContentProviderOperation;
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
import com.boardgamegeek.util.SelectionBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import hugo.weaving.DebugLog;
import timber.log.Timber;

public class CollectionPersister {
	private static final int NOT_DIRTY = 0;
	private final Context context;
	private final ContentResolver resolver;
	private final long updateTime;
	private final boolean isBriefSync;
	private final boolean includePrivateInfo;
	private final boolean includeStats;
	private final List<String> statusesToSync;

	public static class Builder {
		private final Context context;
		private boolean isBriefSync;
		private boolean includePrivateInfo;
		private boolean includeStats;
		private boolean validStatusesOnly;

		@DebugLog
		public Builder(Context context) {
			this.context = context;
		}

		@DebugLog
		public Builder brief() {
			isBriefSync = true;
			validStatusesOnly = false; // requires non-brief sync to fetch number of plays
			return this;
		}

		@DebugLog
		public Builder includePrivateInfo() {
			includePrivateInfo = true;
			return this;
		}

		@DebugLog
		public Builder includeStats() {
			includeStats = true;
			return this;
		}

		@DebugLog
		public Builder validStatusesOnly() {
			validStatusesOnly = true;
			isBriefSync = false; // we need to fetch the number of plays
			return this;
		}

		@DebugLog
		public CollectionPersister build() {
			List<String> statuses = null;
			if (validStatusesOnly) {
				String[] syncStatuses = PreferencesUtils.getSyncStatuses(context);
				statuses = syncStatuses != null ? Arrays.asList(syncStatuses) : new ArrayList<String>(0);
			}
			return new CollectionPersister(context, isBriefSync, includePrivateInfo, includeStats, statuses);
		}
	}

	public static class SaveResults {
		private int recordCount;
		private final List<Integer> savedCollectionIds;
		private final List<Integer> savedGameIds;

		public SaveResults() {
			recordCount = 0;
			savedCollectionIds = new ArrayList<>();
			savedGameIds = new ArrayList<>();
		}

		public void increaseRecordCount(int count) {
			recordCount += count;
		}

		public void addSavedCollectionId(int id) {
			savedCollectionIds.add(id);
		}

		public void addSavedGameId(int id) {
			savedGameIds.add(id);
		}

		public boolean hasGameBeenSaved(int gameId) {
			return savedGameIds.contains(gameId);
		}

		public int getRecordCount() {
			return recordCount;
		}

		public List<Integer> getSavedCollectionIds() {
			return savedCollectionIds;
		}
	}

	@DebugLog
	private CollectionPersister(Context context, boolean isBriefSync, boolean includePrivateInfo, boolean includeStats, List<String> statusesToSync) {
		this.context = context;
		this.isBriefSync = isBriefSync;
		this.includePrivateInfo = includePrivateInfo;
		this.includeStats = includeStats;
		this.statusesToSync = statusesToSync;
		resolver = this.context.getContentResolver();
		updateTime = System.currentTimeMillis();
	}

	@DebugLog
	public long getInitialTimestamp() {
		return updateTime;
	}

	/**
	 * Remove all collection items belonging to a game, except the ones in the specified list.
	 *
	 * @param gameId                 delete collection items with this game ID.
	 * @param protectedCollectionIds list of collection IDs not to delete.
	 * @return the number or rows deleted.
	 */
	@DebugLog
	public int delete(int gameId, List<Integer> protectedCollectionIds) {
		// determine the collection IDs that are no longer in the collection
		List<Integer> collectionIdsToDelete = ResolverUtils.queryInts(resolver,
			Collection.CONTENT_URI,
			Collection.COLLECTION_ID,
			String.format("collection.%s=?", Collection.GAME_ID),
			new String[] { String.valueOf(gameId) });
		if (protectedCollectionIds != null) {
			for (Integer id : protectedCollectionIds) {
				collectionIdsToDelete.remove(id);
			}
		}
		// remove them
		if (collectionIdsToDelete.size() > 0) {
			ArrayList<ContentProviderOperation> batch = new ArrayList<>();
			for (Integer collectionId : collectionIdsToDelete) {
				batch.add(ContentProviderOperation.newDelete(Collection.CONTENT_URI)
					.withSelection(Collection.COLLECTION_ID + "=?", new String[] { String.valueOf(collectionId) })
					.build());
			}
			ResolverUtils.applyBatch(context, batch);
		}

		return collectionIdsToDelete.size();
	}

	@DebugLog
	public SaveResults save(List<CollectionItem> items) {
		SaveResults saveResults = new SaveResults();
		if (items != null && items.size() > 0) {
			ArrayList<ContentProviderOperation> batch = new ArrayList<>();
			for (CollectionItem item : items) {
				batch.clear();
				if (isItemStatusSetToSync(item)) {
					if (saveResults.hasGameBeenSaved(item.gameId)) {
						Timber.i("Already saved game '%s' [ID=%s] during this sync; skipping save", item.gameName(), item.gameId);
					} else {
						addGameToBatch(item, batch);
						saveResults.addSavedGameId(item.gameId);
					}
					addItemToBatch(item, batch);
					ContentProviderResult[] results = ResolverUtils.applyBatch(context, batch);
					Timber.d("Saved a batch of %,d record(s)", results.length);

					saveResults.increaseRecordCount(results.length);
					saveResults.addSavedCollectionId(item.collectionId());
					Timber.i("Saved collection item '%s' [ID=%s, collection ID=%s]", item.gameName(), item.gameId, item.collectionId());
				} else {
					Timber.i("Skipped collection item '%s' [ID=%s, collection ID=%s] - collection status not synced", item.gameName(), item.gameId, item.collectionId());
				}
			}
			Timber.i("Processed %,d collection item(s)", items.size());
		}
		return saveResults;
	}

	@DebugLog
	private boolean isItemStatusSetToSync(CollectionItem item) {
		if (statusesToSync == null) return true; // null means we should always sync
		if (isStatusSetToSync(item.own, "own")) return true;
		if (isStatusSetToSync(item.prevowned, "prevowned")) return true;
		if (isStatusSetToSync(item.fortrade, "fortrade")) return true;
		if (isStatusSetToSync(item.want, "want")) return true;
		if (isStatusSetToSync(item.wanttoplay, "wanttoplay")) return true;
		if (isStatusSetToSync(item.wanttobuy, "wanttobuy")) return true;
		if (isStatusSetToSync(item.wishlist, "wishlist")) return true;
		if (isStatusSetToSync(item.preordered, "preordered")) return true;
		//noinspection RedundantIfStatement
		if (item.numplays > 0 && statusesToSync.contains("played")) return true;
		return false;
	}

	private boolean isStatusSetToSync(String status, String setting) {
		return status.equals("1") && statusesToSync.contains(setting);
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
		if (item.collectionId() != BggContract.INVALID_ID) {
			values.put(Collection.COLLECTION_ID, item.collectionId());
		}
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
	private void addGameToBatch(CollectionItem item, ArrayList<ContentProviderOperation> batch) {
		ContentProviderOperation.Builder cpo;
		Uri uri = Games.buildGameUri(item.gameId);
		ContentValues values = toGameValues(item);
		if (ResolverUtils.rowExists(resolver, uri)) {
			values.remove(Games.GAME_ID);
			cpo = ContentProviderOperation.newUpdate(uri);
		} else {
			cpo = ContentProviderOperation.newInsert(Games.CONTENT_URI);
		}
		batch.add(cpo.withValues(values).build());
	}

	@DebugLog
	private void addItemToBatch(CollectionItem item, ArrayList<ContentProviderOperation> batch) {
		ContentValues values = toCollectionValues(item);
		ContentProviderOperation.Builder cpo;
		long internalId = getCollectionItemInternalIdToUpdate(item.collectionId(), item.gameId);
		if (internalId != BggContract.INVALID_ID) {
			cpo = createUpdateOperation(values, batch, internalId);
		} else {
			internalId = getCollectionItemInternalIdToUpdate(item.gameId);
			if (internalId != BggContract.INVALID_ID) {
				cpo = createUpdateOperation(values, batch, internalId);
			} else {
				cpo = ContentProviderOperation.newInsert(Collection.CONTENT_URI);
			}
		}
		batch.add(cpo.withValues(values).build());
	}

	@DebugLog
	private ContentProviderOperation.Builder createUpdateOperation(ContentValues values, ArrayList<ContentProviderOperation> batch, long internalId) {
		removeDirtyValues(values, internalId);
		Uri uri = Collection.buildUri(internalId);
		ContentProviderOperation.Builder operation = ContentProviderOperation.newUpdate(uri);
		maybeDeleteThumbnail(values, uri, batch);
		return operation;
	}

	@DebugLog
	private void removeDirtyValues(ContentValues values, long internalId) {
		removeValuesIfDirty(values, internalId, Collection.STATUS_DIRTY_TIMESTAMP,
			Collection.STATUS_OWN,
			Collection.STATUS_PREVIOUSLY_OWNED,
			Collection.STATUS_FOR_TRADE,
			Collection.STATUS_WANT,
			Collection.STATUS_WANT_TO_BUY,
			Collection.STATUS_WISHLIST,
			Collection.STATUS_WANT_TO_PLAY,
			Collection.STATUS_PREORDERED,
			Collection.STATUS_WISHLIST_PRIORITY);
		removeValuesIfDirty(values, internalId, Collection.RATING_DIRTY_TIMESTAMP, Collection.RATING);
		removeValuesIfDirty(values, internalId, Collection.COMMENT_DIRTY_TIMESTAMP, Collection.COMMENT);
		removeValuesIfDirty(values, internalId, Collection.PRIVATE_INFO_DIRTY_TIMESTAMP,
			Collection.PRIVATE_INFO_ACQUIRED_FROM,
			Collection.PRIVATE_INFO_ACQUISITION_DATE,
			Collection.PRIVATE_INFO_COMMENT,
			Collection.PRIVATE_INFO_CURRENT_VALUE,
			Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY,
			Collection.PRIVATE_INFO_PRICE_PAID,
			Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY,
			Collection.PRIVATE_INFO_QUANTITY);
		removeValuesIfDirty(values, internalId, Collection.WISHLIST_COMMENT_DIRTY_TIMESTAMP, Collection.WISHLIST_COMMENT);
		removeValuesIfDirty(values, internalId, Collection.TRADE_CONDITION_DIRTY_TIMESTAMP, Collection.CONDITION);
		removeValuesIfDirty(values, internalId, Collection.WANT_PARTS_DIRTY_TIMESTAMP, Collection.WANTPARTS_LIST);
		removeValuesIfDirty(values, internalId, Collection.HAS_PARTS_DIRTY_TIMESTAMP, Collection.HASPARTS_LIST);
	}

	@DebugLog
	private long getCollectionItemInternalIdToUpdate(int collectionId, int gameId) {
		long internalId;
		if (collectionId == BggContract.INVALID_ID) {
			internalId = getCollectionItemInternalIdToUpdate(gameId);
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
	private long getCollectionItemInternalIdToUpdate(int gameId) {
		return ResolverUtils.queryLong(resolver,
			Collection.CONTENT_URI,
			Collection._ID,
			BggContract.INVALID_ID,
			"collection." + Collection.GAME_ID + "=? AND " +
				SelectionBuilder.whereNullOrEmpty(Collection.COLLECTION_ID) + " AND " +
				SelectionBuilder.whereZeroOrNull(Collection.COLLECTION_DIRTY_TIMESTAMP),
			new String[] { String.valueOf(gameId) });
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
	private void removeValuesIfDirty(ContentValues values, long internalId, String columnName, String... columns) {
		if (getDirtyTimestamp(internalId, columnName) != NOT_DIRTY) {
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
