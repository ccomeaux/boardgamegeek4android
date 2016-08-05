package com.boardgamegeek.tasks;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import com.boardgamegeek.events.CollectionItemUpdatedEvent;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;

import org.greenrobot.eventbus.EventBus;

import timber.log.Timber;

public class AddCollectionItemTask extends AsyncTask<Void, Void, Void> {
	private final Context context;
	private final int gameId;

	public AddCollectionItemTask(Context context, int gameId) {
		this.context = context;
		this.gameId = gameId;
	}

	@Override
	protected Void doInBackground(Void... params) {
		final ContentResolver resolver = context.getContentResolver();

		Cursor cursor = null;
		try {
			cursor = resolver.query(Games.buildGameUri(gameId),
				new String[] { Games.GAME_NAME, Games.GAME_SORT_NAME, Games.YEAR_PUBLISHED, Games.IMAGE_URL, Games.THUMBNAIL_URL },
				null, null, null);
			if (cursor == null || !cursor.moveToFirst()) return null;
			String gameName = cursor.getString(0);
			String sortName = cursor.getString(1);
			int yearPublished = cursor.getInt(2);
			String imageUrl = cursor.getString(3);
			String thumbnailUrl = cursor.getString(4);

			ContentValues values = new ContentValues();
			values.put(Collection.GAME_ID, gameId);
			values.put(Collection.COLLECTION_NAME, gameName);
			values.put(Collection.COLLECTION_SORT_NAME, sortName);
			values.put(Collection.COLLECTION_YEAR_PUBLISHED, yearPublished);
			values.put(Collection.COLLECTION_IMAGE_URL, imageUrl);
			values.put(Collection.COLLECTION_THUMBNAIL_URL, thumbnailUrl);
			values.put(Collection.COLLECTION_DIRTY_TIMESTAMP, System.currentTimeMillis());

			Uri response = resolver.insert(Collection.CONTENT_URI, values);
			Timber.i(response != null ? response.toString() : null);
		} finally {
			if (cursor != null) cursor.close();
		}
		return null;
	}

	@Override
	protected void onPostExecute(Void aVoid) {
		EventBus.getDefault().post(new CollectionItemUpdatedEvent());
	}
}
