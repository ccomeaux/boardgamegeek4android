package com.boardgamegeek.tasks;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import com.boardgamegeek.events.CollectionItemUpdatedEvent;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.util.ResolverUtils;

import org.greenrobot.eventbus.EventBus;

import hugo.weaving.DebugLog;

public abstract class UpdateCollectionItemTask extends AsyncTask<Void, Void, Boolean> {
	@SuppressLint("StaticFieldLeak") @Nullable private final Context context;
	protected final int gameId;
	protected final int collectionId;
	protected long internalId;

	public UpdateCollectionItemTask(@Nullable Context context, int gameId, int collectionId, long internalId) {
		this.context = context == null ? null : context.getApplicationContext();
		this.gameId = gameId;
		this.collectionId = collectionId;
		this.internalId = internalId;
	}

	@DebugLog
	@Override
	protected Boolean doInBackground(Void... params) {
		if (context == null) return null;
		final ContentResolver resolver = context.getContentResolver();
		if (internalId == 0) {
			internalId = getCollectionItemInternalId(resolver, collectionId, gameId);
		}
		if (internalId != BggContract.INVALID_ID) {
			updateResolver(resolver, internalId);
		}
		return false;
	}

	@Override
	protected void onPostExecute(Boolean result) {
		if (result) EventBus.getDefault().post(new CollectionItemUpdatedEvent(internalId));
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
