package com.boardgamegeek.tasks;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.provider.BggContract.Collection;

import hugo.weaving.DebugLog;
import timber.log.Timber;

public class UpdateCollectionItemCommentTask extends UpdateCollectionItemTask {
	private final String comment;

	@DebugLog
	public UpdateCollectionItemCommentTask(Context context, int gameId, int collectionId, long internalId, String comment) {
		super(context, gameId, collectionId, internalId);
		this.comment = comment;
	}

	@DebugLog
	@Override
	protected void updateResolver(@NonNull ContentResolver resolver, long internalId) {
		ContentValues values = new ContentValues(2);
		values.put(Collection.COMMENT, comment);
		values.put(Collection.COMMENT_DIRTY_TIMESTAMP, System.currentTimeMillis());
		resolver.update(Collection.buildUri(internalId), values, null, null);
	}

	@DebugLog
	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
		Timber.i("Updated game ID %1$s, collection ID %2$s with comment \"%3$s\"", gameId, collectionId, comment);
	}
}
