package com.boardgamegeek.tasks;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import com.boardgamegeek.events.CollectionItemUpdatedEvent;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.util.StringUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

public class AddCollectionItemTask extends AsyncTask<Void, Void, Long> {
	private final Context context;
	private final int gameId;
	private final List<String> statuses;
	private final int wishlistPriority;

	public AddCollectionItemTask(Context context, int gameId, List<String> statuses, int wishlistPriority) {
		this.context = context;
		this.gameId = gameId;
		this.statuses = statuses;
		this.wishlistPriority = wishlistPriority;
	}

	@Override
	protected Long doInBackground(Void... params) {
		long internalId = 0;
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

			putValue(values, Collection.STATUS_OWN);
			putValue(values, Collection.STATUS_PREORDERED);
			putValue(values, Collection.STATUS_FOR_TRADE);
			putValue(values, Collection.STATUS_WANT);
			putValue(values, Collection.STATUS_WANT_TO_PLAY);
			putValue(values, Collection.STATUS_WANT_TO_BUY);
			putValue(values, Collection.STATUS_WISHLIST);
			putValue(values, Collection.STATUS_PREVIOUSLY_OWNED);
			putWishlist(values);
			values.put(Collection.STATUS_DIRTY_TIMESTAMP, System.currentTimeMillis());

			Uri response = resolver.insert(Collection.CONTENT_URI, values);
			internalId = response == null ? BggContract.INVALID_ID : StringUtils.parseLong(response.getLastPathSegment(), BggContract.INVALID_ID);
		} finally {
			if (cursor != null) cursor.close();
		}
		return internalId;
	}

	private void putValue(ContentValues values, String statusColumn) {
		values.put(statusColumn, statuses.contains(statusColumn) ? 1 : 0);
	}

	private void putWishlist(ContentValues values) {
		if (statuses.contains(Collection.STATUS_WISHLIST)) {
			values.put(Collection.STATUS_WISHLIST, 1);
			values.put(Collection.STATUS_WISHLIST_PRIORITY, wishlistPriority);
			return;
		}
		values.put(Collection.STATUS_WISHLIST, 0);
	}

	@Override
	protected void onPostExecute(Long internalId) {
		EventBus.getDefault().post(new CollectionItemUpdatedEvent(internalId));
	}
}
