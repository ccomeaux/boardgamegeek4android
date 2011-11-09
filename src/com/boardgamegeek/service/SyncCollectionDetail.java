package com.boardgamegeek.service;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.text.format.DateUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RemoteGameHandler;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.provider.BggContract.SyncColumns;
import com.boardgamegeek.util.HttpUtils;

public class SyncCollectionDetail extends SyncTask {

	private static final int GAMES_PER_FETCH = 25;
	// TODO Perhaps move these contsants into preferences
	private static final int SYNC_GAME_AGE_IN_DAYS = 30;
	private static final int SYNC_GAME_LIMIT = 50;

	private RemoteExecutor mRemoteExecutor;

	@Override
	public void execute(RemoteExecutor executor, Context context) throws HandlerException {

		Cursor cursor = null;
		mRemoteExecutor = executor;
		ContentResolver resolver = context.getContentResolver();

		try {
			// Update games that haven't been synced or haven't been synced in a while
			long days = System.currentTimeMillis() - (SYNC_GAME_AGE_IN_DAYS * DateUtils.DAY_IN_MILLIS);
			cursor = resolver.query(Games.CONTENT_URI, new String[] { Games.GAME_ID }, SyncColumns.UPDATED + "<? OR "
					+ SyncColumns.UPDATED + " IS NULL", new String[] { String.valueOf(days) }, null);
			if (cursor.moveToFirst()) {
				fetchGames(cursor);
			} else {
				// If everything is relatively up to date, sync the oldest games
				cursor = resolver.query(Games.CONTENT_URI, new String[] { Games.GAME_ID }, null, null, Games.UPDATED
						+ " LIMIT " + SYNC_GAME_LIMIT);
				fetchGames(cursor);
			}

		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private void fetchGames(Cursor cursor) throws HandlerException {
		List<String> ids = new ArrayList<String>();
		cursor.moveToPosition(-1);
		while (cursor.moveToNext()) {
			final String id = cursor.getString(0);
			ids.add(id);
			if (ids.size() >= GAMES_PER_FETCH) {
				mRemoteExecutor.executeGet(HttpUtils.constructGameUrl(ids), new RemoteGameHandler());
				ids.clear();
			}
		}

		if (ids.size() > 0) {
			mRemoteExecutor.executeGet(HttpUtils.constructGameUrl(ids), new RemoteGameHandler());
		}
	}

	@Override
	public int getNotification() {
		return R.string.notification_text_collection_detail;
	}
}
