package com.boardgamegeek.service;

import java.text.DateFormat;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteBuddyUserHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.SyncColumns;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.HttpUtils;

public class SyncBuddiesDetail extends SyncTask {
	private final static String TAG = "SyncBuddiesDetail";

	private final static int SYNC_BUDDY_DETAIL_DAYS = 7;

	@Override
	public void execute(RemoteExecutor executor, Context context) throws HandlerException {
		Cursor cursor = null;
		try {
			RemoteBuddyUserHandler handler = new RemoteBuddyUserHandler();
			ContentResolver resolver = context.getContentResolver();
			DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);

			cursor = resolver.query(Buddies.CONTENT_URI, new String[] { Buddies.BUDDY_NAME, SyncColumns.UPDATED },
					null, null, null);
			while (cursor.moveToNext()) {
				final String name = cursor.getString(0);
				final long lastUpdated = cursor.getLong(1);
				if (DateTimeUtils.howManyDaysOld(lastUpdated) > SYNC_BUDDY_DETAIL_DAYS) {
					executor.executeGet(HttpUtils.constructUserUrl(name), handler);
				} else {
					Log.v(TAG, "Skipping name=" + name + ", updated on " + dateFormat.format(lastUpdated));
				}
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	@Override
	public int getNotification() {
		return R.string.notification_text_buddies_detail;
	}
}
