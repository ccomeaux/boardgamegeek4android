package com.boardgamegeek.tasks;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.events.CollectionItemUpdatedEvent;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Collection;

import org.greenrobot.eventbus.EventBus;

import hugo.weaving.DebugLog;
import timber.log.Timber;

public class UpdateCollectionItemCommentTask extends UpdateCollectionItemTask {
	private final String comment;

	@DebugLog
	public UpdateCollectionItemCommentTask(Context context, int gameId, int collectionId, String comment) {
		super(context, gameId, collectionId);
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
}
