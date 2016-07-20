package com.boardgamegeek.tasks;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;

import com.boardgamegeek.events.CollectionItemUpdatedEvent;
import com.boardgamegeek.provider.BggContract.Collection;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import hugo.weaving.DebugLog;
import timber.log.Timber;

public class UpdateCollectionItemStatusTask extends UpdateCollectionItemTask {
	private final List<String> statuses;

	@DebugLog
	public UpdateCollectionItemStatusTask(Context context, int gameId, int collectionId, List<String> statuses) {
		super(context, gameId, collectionId);
		this.statuses = statuses;
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
		for (int i = 1; i <= 5; i++) {
			if (statuses.contains(String.valueOf(i))) {
				values.put(Collection.STATUS_WISHLIST, 1);
				values.put(Collection.STATUS_WISHLIST_PRIORITY, i);
				return;
			}
		}
		values.put(Collection.STATUS_WISHLIST, 0);
	}

	@DebugLog
	@Override
	protected void onPostExecute(Void result) {
		Timber.i("Updated game ID %1$s, collection ID %2$s with statuses \"%3$s\"", gameId, collectionId, statuses.toArray());
		EventBus.getDefault().post(new CollectionItemUpdatedEvent());
	}
}
