package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.text.format.DateUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteBuddyUserHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.SyncColumns;
import com.boardgamegeek.util.HttpUtils;

public class SyncBuddiesDetail extends SyncTask {
	private static final String TAG = makeLogTag(SyncBuddiesDetail.class);

	private static final int SYNC_BUDDY_DETAIL_DAYS = 21;
	private static final int SYNC_BUDDY_LIMIT = 10;

	@Override
	public void execute(RemoteExecutor executor, Context context) throws HandlerException {

		ContentResolver resolver = context.getContentResolver();
		Cursor cursor = null;

		try {
			long days = System.currentTimeMillis() - (SYNC_BUDDY_DETAIL_DAYS * DateUtils.DAY_IN_MILLIS);
			cursor = resolver.query(Buddies.CONTENT_URI, new String[] { Buddies.BUDDY_NAME }, SyncColumns.UPDATED
					+ "<? OR " + SyncColumns.UPDATED + " IS NULL", new String[] { String.valueOf(days) }, null);
			if (cursor.getCount() > 0) {
				LOGI(TAG, "Updating buddies older than " + SYNC_BUDDY_DETAIL_DAYS + " days old");
				fetchBuddies(executor, cursor);
			} else {
				fetchOldestBuddies(executor, resolver);
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
	}

	protected void fetchOldestBuddies(RemoteExecutor executor, ContentResolver resolver) throws HandlerException {
		LOGI(TAG, "Updating " + SYNC_BUDDY_LIMIT + " oldest buddies");
		Cursor cursor = null;
		try {
			cursor = resolver.query(Buddies.CONTENT_URI, new String[] { Buddies.BUDDY_NAME }, null, null,
					SyncColumns.UPDATED + " LIMIT " + SYNC_BUDDY_LIMIT);
			fetchBuddies(executor, cursor);
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
	}

	private void fetchBuddies(RemoteExecutor executor, Cursor cursor) throws HandlerException {
		if (cursor.moveToFirst()) {
			do {
				String name = cursor.getString(0);
				RemoteBuddyUserHandler handler = new RemoteBuddyUserHandler();
				executor.executeGet(HttpUtils.constructUserUrl(name), handler);
				if (handler.isBggDown()) {
					setIsBggDown(true);
					break;
				}

			} while (cursor.moveToNext());
		}
	}

	@Override
	public int getNotification() {
		return R.string.notification_text_buddies_detail;
	}
}
