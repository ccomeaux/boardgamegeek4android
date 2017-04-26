package com.boardgamegeek.tasks;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import com.boardgamegeek.provider.BggContract.CollectionViews;

public class SelectCollectionViewTask extends AsyncTask<Void, Void, Void> {
	private final Context context;
	private final long viewId;

	public SelectCollectionViewTask(Context context, long viewId) {
		this.context = context.getApplicationContext();
		this.viewId = viewId;
	}

	@Override
	protected Void doInBackground(Void... params) {
		Cursor cursor = null;
		try {
			Uri uri = CollectionViews.buildViewUri(viewId);
			cursor = context.getContentResolver().query(uri,
				new String[] { CollectionViews.SELECTED_COUNT },
				null, null, null);
			if (cursor != null && cursor.moveToFirst()) {
				int currentCount = cursor.getInt(0);
				ContentValues cv = new ContentValues(2);
				cv.put(CollectionViews.SELECTED_COUNT, currentCount + 1);
				cv.put(CollectionViews.SELECTED_TIMESTAMP, System.currentTimeMillis());
				context.getContentResolver().update(uri, cv, null, null);
			}
		} finally {
			if (cursor != null) cursor.close();
		}
		return null;
	}
}
