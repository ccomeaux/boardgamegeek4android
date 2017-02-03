package com.boardgamegeek.tasks;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.provider.BggContract.Collection;

import hugo.weaving.DebugLog;
import timber.log.Timber;

public class UpdateCollectionItemTextTask extends UpdateCollectionItemTask {
	private final String text;
	private final String textColumn;
	private final String timestampColumn;

	@DebugLog
	public UpdateCollectionItemTextTask(Context context, int gameId, int collectionId, long internalId, String text, String textColumn, String timestampColumn) {
		super(context, gameId, collectionId, internalId);
		this.text = text;
		this.textColumn = textColumn;
		this.timestampColumn = timestampColumn;
	}

	@DebugLog
	@Override
	protected void updateResolver(@NonNull ContentResolver resolver, long internalId) {
		ContentValues values = new ContentValues(2);
		values.put(textColumn, text);
		values.put(timestampColumn, System.currentTimeMillis());
		resolver.update(Collection.buildUri(internalId), values, null, null);
	}

	@DebugLog
	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
		Timber.i("Updated game ID %1$s, collection ID %2$s with text \"%3$s\"", gameId, collectionId, text);
	}
}
