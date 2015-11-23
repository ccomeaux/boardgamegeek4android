package com.boardgamegeek.tasks;


import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;

import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.service.SyncService;
import com.boardgamegeek.util.ResolverUtils;

import hugo.weaving.DebugLog;
import timber.log.Timber;

public class UpdateCollectionItemRatingTask extends AsyncTask<Void, Void, Void> {
	private final Context context;
	private final int gameId;
	private final int collectionId;
	private final double rating;

	@DebugLog
	public UpdateCollectionItemRatingTask(Context context, int gameId, int collectionId, double rating) {
		this.context = context;
		this.gameId = gameId;
		this.collectionId = collectionId;
		this.rating = rating;
	}

	@DebugLog
	@Override
	protected Void doInBackground(Void... params) {
		final ContentResolver resolver = context.getContentResolver();
		long internalId = getCollectionItemInternalId(resolver, collectionId, gameId);
		if (internalId != BggContract.INVALID_ID) {
			updateResolver(resolver, internalId);
			SyncService.sync(context, SyncService.FLAG_SYNC_COLLECTION_UPLOAD);
		}
		return null;
	}

	private void updateResolver(ContentResolver resolver, long internalId) {
		ContentValues values = new ContentValues(2);
		values.put(Collection.RATING, rating);
		values.put(Collection.RATING_DIRTY_TIMESTAMP, System.currentTimeMillis());
		resolver.update(Collection.buildUri(internalId), values, null, null);
	}

	@DebugLog
	@Override
	protected void onPostExecute(Void result) {
		Timber.i("Updated game ID %1$s, collection ID %2$s with rating %3$s", gameId, collectionId, rating);
	}

	@DebugLog
	private long getCollectionItemInternalId(ContentResolver resolver, int collectionId, int gameId) {
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
}
