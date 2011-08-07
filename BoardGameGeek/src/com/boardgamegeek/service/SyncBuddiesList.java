package com.boardgamegeek.service;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;
import com.boardgamegeek.io.RemoteBuddiesHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.XmlHandler.HandlerException;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.util.HttpUtils;

public class SyncBuddiesList extends SyncTask {

	@Override
	public void execute(RemoteExecutor executor, Context context) throws HandlerException {

		ContentResolver resolver = context.getContentResolver();
		String username = BggApplication.getInstance().getUserName();

		final long startTime = System.currentTimeMillis();
		insertSelf(resolver, username);
		executor.executePagedGet(HttpUtils.constructUserUrl(username, true), new RemoteBuddiesHandler());
		resolver.delete(Buddies.CONTENT_URI, Buddies.UPDATED_LIST + "<?", new String[] { String.valueOf(startTime) });
	}

	@Override
	public int getNotification() {
		return R.string.notification_text_buddies_list;
	}

	private void insertSelf(ContentResolver resolver, String username) {
		int selfId = 0;
		Uri uri = Buddies.buildBuddyUri(selfId);

		ContentValues values = new ContentValues();
		values.put(Buddies.UPDATED_LIST, System.currentTimeMillis());
		values.put(Buddies.BUDDY_NAME, username);
		
		Cursor cursor = resolver.query(uri, new String[] { BaseColumns._ID, }, null, null, null);
		if (cursor.moveToFirst()) {
			resolver.update(uri, values, null, null);
		} else {
			values.put(Buddies.BUDDY_ID, selfId);
			resolver.insert(Buddies.CONTENT_URI, values);
		}
	}
}
