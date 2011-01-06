package com.boardgamegeek.service;

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

public class SyncCollectionDetail extends SyncTask {

	private static final int SYNC_GAME_DETAIL_DAYS = 1;
	private static final int GAMES_PER_FETCH=10;

	private RemoteExecutor mRemoteExecutor;

	@Override
	public void execute(RemoteExecutor executor, Context context) throws HandlerException {

		Cursor cursor = null;
		mRemoteExecutor = executor;
		ContentResolver resolver = context.getContentResolver();

		try {
			int count = 0;
			String ids = "";
			long days = System.currentTimeMillis() - (SYNC_GAME_DETAIL_DAYS * DateUtils.DAY_IN_MILLIS);
			cursor = resolver.query(Games.CONTENT_URI, new String[] { Games.GAME_ID },
				SyncColumns.UPDATED_DETAIL + "<?", new String[] { "" + days }, null);
			while (cursor.moveToNext()) {
				final int id = cursor.getInt(0);
				count++;
				ids = ids + "," + id;
				if (count == GAMES_PER_FETCH) {
					fetchGameDetail(ids);
					count = 0;
					ids = "";
				}
			}

			if (count > 0) {
				fetchGameDetail(ids);
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

	private void fetchGameDetail(String ids) throws HandlerException {
		final String url = SyncService.BASE_URL + "boardgame/" + ids.substring(1) + "?stats=1";
		mRemoteExecutor.executeGet(url, new RemoteGameHandler());
	}
}
