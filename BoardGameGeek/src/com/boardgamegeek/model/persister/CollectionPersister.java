package com.boardgamegeek.model.persister;

import java.util.ArrayList;
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
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.Thumbnails;
import com.boardgamegeek.util.FileUtils;
import com.boardgamegeek.util.ResolverUtils;

public class CollectionPersister {
	public static int save(Context context, CollectionItem item) {
		return save(context, item, System.currentTimeMillis());
	}

	public static int save(Context context, CollectionItem item, long updateTime) {
		List<CollectionItem> items = new ArrayList<CollectionItem>(1);
		items.add(item);
		return save(context, items, updateTime);
	}

	public static int save(Context context, List<CollectionItem> items, long updateTime) {
		ContentResolver resolver = context.getContentResolver();
		ArrayList<ContentProviderOperation> batch = new ArrayList<>();
		if (items != null) {
			for (CollectionItem item : items) {
				insertOrUpdateGame(resolver, toGameValues(item, updateTime), batch);
				insertOrUpdateCollection(resolver, toCollectionValues(item, updateTime), batch);
			}
		}
		ContentProviderResult[] result = ResolverUtils.applyBatch(resolver, batch);
		if (result == null) {
			return 0;
		} else {
			return result.length;
		}
	}

	private static ContentValues toGameValues(CollectionItem item, long updateTime) {
		ContentValues values = new ContentValues();
		values.put(Games.UPDATED_LIST, updateTime);
		values.put(Games.GAME_ID, item.gameId);
		values.put(Games.GAME_NAME, item.gameName());
		values.put(Games.GAME_SORT_NAME, item.gameSortName());
		// don't overwrite the game's value
		// values.put(Games.YEAR_PUBLISHED, item.yearpublished);
		// values.put(Collection.IMAGE_URL, item.image);
		// values.put(Collection.THUMBNAIL_URL, item.thumbnail);
		values.put(Games.NUM_PLAYS, item.numplays);
		// TODO: store when stats are retrieved
		// values.put(Games.MIN_PLAYERS, parseIntegerAttribute(Tags.MIN_PLAYERS));
		// values.put(Games.MAX_PLAYERS, parseIntegerAttribute(Tags.MAX_PLAYERS));
		// values.put(Games.PLAYING_TIME, parseIntegerAttribute(Tags.PLAYING_TIME));
		// values.put(Games.STATS_NUMBER_OWNED, parseIntegerAttribute(Tags.NUM_OWNED));
		return values;
	}

	private static ContentValues toCollectionValues(CollectionItem item, long updateTime) {
		ContentValues values = new ContentValues();
		values.put(Collection.UPDATED, updateTime);
		values.put(Collection.UPDATED_LIST, updateTime);
		values.put(Collection.GAME_ID, item.gameId);
		values.put(Collection.COLLECTION_ID, item.collectionId);
		values.put(Collection.COLLECTION_NAME, item.collectionName());
		values.put(Collection.COLLECTION_SORT_NAME, item.collectionSortName());
		values.put(Collection.COLLECTION_YEAR_PUBLISHED, item.yearpublished);
		values.put(Collection.COLLECTION_IMAGE_URL, item.image);
		values.put(Collection.COLLECTION_THUMBNAIL_URL, item.thumbnail);
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
		values.put(Collection.PRIVATE_INFO_PRICE_PAID_CURRENCY, item.pricePaidCurrency);
		values.put(Collection.PRIVATE_INFO_PRICE_PAID, item.pricePaid());
		values.put(Collection.PRIVATE_INFO_CURRENT_VALUE_CURRENCY, item.currentValueCurrency);
		values.put(Collection.PRIVATE_INFO_CURRENT_VALUE, item.currentValue());
		values.put(Collection.PRIVATE_INFO_QUANTITY, item.quantity);
		values.put(Collection.PRIVATE_INFO_ACQUISITION_DATE, item.acquisitionDate);
		values.put(Collection.PRIVATE_INFO_ACQUIRED_FROM, item.acquiredFrom);
		values.put(Collection.PRIVATE_INFO_COMMENT, item.privatecomment);
		values.put(Collection.COMMENT, item.comment);
		values.put(Collection.WANTPARTS_LIST, item.wantpartslist);
		values.put(Collection.CONDITION, item.conditiontext);
		values.put(Collection.HASPARTS_LIST, item.haspartslist);
		values.put(Collection.WISHLIST_COMMENT, item.wishlistcomment);
		return values;
	}

	private static void insertOrUpdateGame(ContentResolver resolver, ContentValues values,
		ArrayList<ContentProviderOperation> batch) {
		Builder cpo = null;
		Uri uri = Games.buildGameUri(values.getAsInteger(Games.GAME_ID));
		if (ResolverUtils.rowExists(resolver, uri)) {
			values.remove(Games.GAME_ID);
			cpo = ContentProviderOperation.newUpdate(uri);
		} else {
			cpo = ContentProviderOperation.newInsert(Games.CONTENT_URI);
		}
		batch.add(cpo.withValues(values).withYieldAllowed(true).build());
	}

	private static void insertOrUpdateCollection(ContentResolver resolver, ContentValues values,
		ArrayList<ContentProviderOperation> batch) {
		Builder cpo = null;
		Uri uri = Collection.buildItemUri(values.getAsInteger(Collection.COLLECTION_ID));
		if (ResolverUtils.rowExists(resolver, uri)) {
			values.remove(Collection.COLLECTION_ID);
			cpo = ContentProviderOperation.newUpdate(uri);
			maybeDeleteThumbnail(resolver, values, uri, batch);
		} else {
			cpo = ContentProviderOperation.newInsert(Collection.CONTENT_URI);
		}
		batch.add(cpo.withValues(values).build());
	}

	private static void maybeDeleteThumbnail(ContentResolver resolver, ContentValues values, Uri uri,
		ArrayList<ContentProviderOperation> batch) {
		if (!values.containsKey(Collection.THUMBNAIL_URL)) {
			// nothing to do - no thumbnail
			return;
		}

		String newThumbnailUrl = values.getAsString(Collection.THUMBNAIL_URL);
		if (newThumbnailUrl == null) {
			newThumbnailUrl = "";
		}

		String oldThumbnailUrl = ResolverUtils.queryString(resolver, uri, Collection.THUMBNAIL_URL);
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
