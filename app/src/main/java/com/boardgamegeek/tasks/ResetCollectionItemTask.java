package com.boardgamegeek.tasks;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.service.UpdateService;

public class ResetCollectionItemTask extends AsyncTask<Void, Void, Boolean> {
	private final Context context;
	private long internalId;
	private int gameId;

	public ResetCollectionItemTask(Context context, long internalId, int gameId) {
		this.context = context.getApplicationContext();
		this.internalId = internalId;
		this.gameId = gameId;
	}

	@NonNull
	@Override
	protected Boolean doInBackground(Void... params) {
		ContentValues values = new ContentValues(9);
		values.put(Collection.COLLECTION_DIRTY_TIMESTAMP, 0);
		values.put(Collection.STATUS_DIRTY_TIMESTAMP, 0);
		values.put(Collection.COMMENT_DIRTY_TIMESTAMP, 0);
		values.put(Collection.RATING_DIRTY_TIMESTAMP, 0);
		values.put(Collection.PRIVATE_INFO_DIRTY_TIMESTAMP, 0);
		values.put(Collection.WISHLIST_COMMENT_DIRTY_TIMESTAMP, 0);
		values.put(Collection.TRADE_CONDITION_DIRTY_TIMESTAMP, 0);
		values.put(Collection.WANT_PARTS_DIRTY_TIMESTAMP, 0);
		values.put(Collection.HAS_PARTS_DIRTY_TIMESTAMP, 0);

		ContentResolver resolver = context.getContentResolver();
		int rows = resolver.update(Collection.buildUri(internalId), values, null, null);
		return rows > 0;
	}

	@Override
	protected void onPostExecute(Boolean result) {
		if (result) {
			UpdateService.start(context, UpdateService.SYNC_TYPE_GAME_COLLECTION, gameId);
		}
	}
}
