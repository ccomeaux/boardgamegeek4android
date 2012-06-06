package com.boardgamegeek.service;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.text.format.DateUtils;
import android.util.Log;

import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteBuddyUserHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.SyncColumns;
import com.boardgamegeek.util.HttpUtils;

public class SyncBuddiesDetail extends SyncTask {
	private static final String TAG = "SyncBuddiesDetail";

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
			if (cursor.moveToFirst()) {
				Log.i(TAG, "Updating buddies older than " + SYNC_BUDDY_DETAIL_DAYS + " days old");
				fetchBuddies(executor, cursor);
			} else {
				Log.i(TAG, "Updating " + SYNC_BUDDY_LIMIT + " oldest buddies");
				cursor.close();
				cursor = resolver.query(Buddies.CONTENT_URI, new String[] { Buddies.BUDDY_NAME }, null, null,
						SyncColumns.UPDATED + " LIMIT " + SYNC_BUDDY_LIMIT);
				fetchBuddies(executor, cursor);
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
	}

	private void fetchBuddies(RemoteExecutor executor, Cursor cursor) throws HandlerException {
		cursor.moveToPosition(-1);
		while (cursor.moveToNext()) {
			String name = cursor.getString(0);
			RemoteBuddyUserHandler handler = new RemoteBuddyUserHandler();
			executor.executeGet(HttpUtils.constructUserUrl(name), handler);
			if (handler.isBggDown()) {
				setIsBggDown(true);
				break;
			}
		}
	}

	@Override
	public int getNotification() {
		return R.string.notification_text_buddies_detail;
	}
}
