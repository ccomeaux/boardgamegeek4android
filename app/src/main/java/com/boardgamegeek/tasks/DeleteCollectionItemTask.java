package com.boardgamegeek.tasks;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;

import com.boardgamegeek.events.CollectionItemDeletedEvent;
import com.boardgamegeek.provider.BggContract.Collection;

import org.greenrobot.eventbus.EventBus;

public class DeleteCollectionItemTask extends AsyncTask<Void, Void, Void> {
	private final Context context;
	private final long internalId;

	public DeleteCollectionItemTask(Context context, long internalId) {
		this.context = context;
		this.internalId = internalId;
	}

	@Override
	protected Void doInBackground(Void... params) {
		final ContentResolver resolver = context.getContentResolver();
		ContentValues values = new ContentValues();
		values.put(Collection.COLLECTION_DELETE_TIMESTAMP, System.currentTimeMillis());
		resolver.update(Collection.buildUri(internalId), values, null, null);
		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		EventBus.getDefault().post(new CollectionItemDeletedEvent());
	}
}
