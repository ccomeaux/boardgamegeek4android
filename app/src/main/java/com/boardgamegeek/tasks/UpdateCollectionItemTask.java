package com.boardgamegeek.tasks;

import android.content.ContentResolver;
import android.content.Context;
import android.os.AsyncTask;

import com.boardgamegeek.events.CollectionItemUpdatedEvent;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.util.ResolverUtils;

import org.greenrobot.eventbus.EventBus;

import hugo.weaving.DebugLog;

public abstract class UpdateCollectionItemTask extends AsyncTask<Void, Void, Void> {
	protected final Context context;
	protected final int gameId;
	protected final int collectionId;

	public UpdateCollectionItemTask(Context context, int gameId, int collectionId) {
		this.gameId = gameId;
		this.collectionId = collectionId;
		this.context = context;
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

	@Override
	protected void onPostExecute(Void aVoid) {
		EventBus.getDefault().post(new CollectionItemUpdatedEvent());
	}

	@DebugLog
	protected long getCollectionItemInternalId(ContentResolver resolver, int collectionId, int gameId) {
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

	protected abstract void updateResolver(ContentResolver resolver, long internalId);
}
