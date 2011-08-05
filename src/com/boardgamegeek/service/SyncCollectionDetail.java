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

	private static final int SYNC_GAME_DETAIL_DAYS = 0;
	private static final int GAMES_PER_FETCH = 10;

	private RemoteExecutor mRemoteExecutor;

	@Override
	public void execute(RemoteExecutor executor, Context context) throws HandlerException {

		Cursor cursor = null;
		mRemoteExecutor = executor;
		ContentResolver resolver = context.getContentResolver();

		try {
			int count = 0;
			List<String> ids = new ArrayList<String>();
			long days = System.currentTimeMillis() - (SYNC_GAME_DETAIL_DAYS * DateUtils.DAY_IN_MILLIS);
			cursor = resolver.query(Games.CONTENT_URI, new String[] { Games.GAME_ID }, SyncColumns.UPDATED + "<? OR "
					+ SyncColumns.UPDATED + " IS NULL", new String[] { String.valueOf(days) }, null);
			while (cursor.moveToNext()) {
				final String id = cursor.getString(0);
				count++;
				ids.add(id);
				if (count == GAMES_PER_FETCH) {
					mRemoteExecutor.executeGet(HttpUtils.constructGameUrl(ids), new RemoteGameHandler());
					count = 0;
				}
			}

			if (count > 0) {
				mRemoteExecutor.executeGet(HttpUtils.constructGameUrl(ids), new RemoteGameHandler());
			}

		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	@Override
	public int getNotification() {
		return R.string.notification_text_collection_detail;
	}
}
