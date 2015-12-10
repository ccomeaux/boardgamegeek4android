package com.boardgamegeek.tasks;


import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.boardgamegeek.events.CollectionItemUpdatedEvent;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.util.ResolverUtils;

import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;
import timber.log.Timber;

public class UpdateCollectionItemCommentTask extends AsyncTask<Void, Void, Void> {
	private final Context context;
	private final int gameId;
	private final int collectionId;
	private final String comment;

	@DebugLog
	public UpdateCollectionItemCommentTask(Context context, int gameId, int collectionId, String comment) {
		this.context = context;
		this.gameId = gameId;
		this.collectionId = collectionId;
		this.comment = comment;
	}

	@DebugLog
	@Override
	protected Void doInBackground(Void... params) {
		final ContentResolver resolver = context.getContentResolver();
		long internalId = getCollectionItemInternalId(resolver, collectionId, gameId);
		if (internalId != BggContract.INVALID_ID) {
			updateResolver(resolver, internalId);
		}
		return null;
	}

	@DebugLog
	private void updateResolver(@NonNull ContentResolver resolver, long internalId) {
		ContentValues values = new ContentValues(2);
		values.put(Collection.COMMENT, comment);
		values.put(Collection.COMMENT_DIRTY_TIMESTAMP, System.currentTimeMillis());
		resolver.update(Collection.buildUri(internalId), values, null, null);
	}

	@DebugLog
	@Override
	protected void onPostExecute(Void result) {
		Timber.i("Updated game ID %1$s, collection ID %2$s with comment %3$s", gameId, collectionId, comment);
		EventBus.getDefault().post(new CollectionItemUpdatedEvent());
	}

	@DebugLog
	private static long getCollectionItemInternalId(ContentResolver resolver, int collectionId, int gameId) {
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
