package com.boardgamegeek.service;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SyncResult;
import android.net.Uri;

import com.boardgamegeek.R;
import com.boardgamegeek.database.ResolverUtils;
import com.boardgamegeek.io.RemoteBuddiesHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.util.HttpUtils;

public class SyncBuddiesList extends SyncTask {

	@Override
	public void execute(RemoteExecutor executor, Account account, SyncResult syncResult) throws IOException,
		XmlPullParserException {

		ContentResolver resolver = executor.getContext().getContentResolver();
		long startTime = System.currentTimeMillis();

		insertSelf(resolver, account.name, startTime, syncResult);

		RemoteBuddiesHandler handler = new RemoteBuddiesHandler();
		executor.executePagedGet(HttpUtils.constructUserUrl(account.name, true), handler);
		syncResult.stats.numInserts += handler.getNumInserts();
		syncResult.stats.numUpdates += handler.getNumUpdates();

		// TODO: delete avatar images associated with this list
		int count = resolver.delete(Buddies.CONTENT_URI, Buddies.UPDATED_LIST + "<?",
			new String[] { String.valueOf(startTime) });
		syncResult.stats.numDeletes += count;
	}

	@Override
	public int getNotification() {
		return R.string.notification_text_buddies_list;
	}

	private void insertSelf(ContentResolver resolver, String username, long startTime, SyncResult syncResult) {
		int selfId = 0;
		Uri uri = Buddies.buildBuddyUri(selfId);

		ContentValues values = new ContentValues();
		values.put(Buddies.UPDATED_LIST, startTime);
		values.put(Buddies.BUDDY_NAME, username);

		if (ResolverUtils.rowExists(resolver, uri)) {
			resolver.update(uri, values, null, null);
			syncResult.stats.numUpdates++;
		} else {
			values.put(Buddies.BUDDY_ID, selfId);
			resolver.insert(Buddies.CONTENT_URI, values);
			syncResult.stats.numInserts++;
		}
	}
}
