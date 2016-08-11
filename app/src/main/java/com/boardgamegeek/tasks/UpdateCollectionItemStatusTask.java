package com.boardgamegeek.tasks;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;

import com.boardgamegeek.provider.BggContract.Collection;

import java.util.List;

import hugo.weaving.DebugLog;
import timber.log.Timber;

public class UpdateCollectionItemStatusTask extends UpdateCollectionItemTask {
	private final List<String> statuses;
	private final int wishlistPriority;

	@DebugLog
	public UpdateCollectionItemStatusTask(Context context, int gameId, int collectionId, long internalId, List<String> statuses, int wishlistPriority) {
		super(context, gameId, collectionId, internalId);
		this.statuses = statuses;
		this.wishlistPriority = wishlistPriority;
	}

	@Override
	protected void updateResolver(ContentResolver resolver, long internalId) {
		ContentValues values = new ContentValues(10);
		putValue(values, Collection.STATUS_OWN);
		putValue(values, Collection.STATUS_PREORDERED);
		putValue(values, Collection.STATUS_FOR_TRADE);
		putValue(values, Collection.STATUS_WANT);
		putValue(values, Collection.STATUS_WANT_TO_PLAY);
		putValue(values, Collection.STATUS_WANT_TO_BUY);
		putValue(values, Collection.STATUS_WISHLIST);
		putValue(values, Collection.STATUS_PREVIOUSLY_OWNED);
		putWishlist(values);
		values.put(Collection.STATUS_DIRTY_TIMESTAMP, System.currentTimeMillis());
		resolver.update(Collection.buildUri(internalId), values, null, null);
	}

	private void putValue(ContentValues values, String statusColumn) {
		values.put(statusColumn, statuses.contains(statusColumn) ? 1 : 0);
	}

	private void putWishlist(ContentValues values) {
		if (statuses.contains(Collection.STATUS_WISHLIST)) {
			values.put(Collection.STATUS_WISHLIST, 1);
			values.put(Collection.STATUS_WISHLIST_PRIORITY, wishlistPriority);
			return;
		}
		values.put(Collection.STATUS_WISHLIST, 0);
	}

	@DebugLog
	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
		Timber.i("Updated game ID %1$s, collection ID %2$s with statuses \"%3$s\"", gameId, collectionId, statuses.toString());
	}
}
