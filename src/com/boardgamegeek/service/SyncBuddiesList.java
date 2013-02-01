package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SyncResult;
import android.net.Uri;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.database.ResolverUtils;
import com.boardgamegeek.io.RemoteBuddiesHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.UserUrlBuilder;

public class SyncBuddiesList extends SyncTask {
	private static final String TAG = makeLogTag(SyncBuddiesList.class);

	@Override
	public void execute(RemoteExecutor executor, Account account, SyncResult syncResult) throws IOException,
		XmlPullParserException {
		LOGI(TAG, "Syncing list of buddies in the collection...");
		try {
			if (!PreferencesUtils.getSyncBuddies(executor.getContext())) {
				LOGI(TAG, "...buddies not set to sync");
				return;
			}

			AccountManager accountManager = AccountManager.get(executor.getContext());
			String s = accountManager.getUserData(account, SyncService.TIMESTAMP_BUDDIES);
			long lastCompleteSync = TextUtils.isEmpty(s) ? 0 : Long.parseLong(s);
			if (lastCompleteSync >= 0 && DateTimeUtils.howManyDaysOld(lastCompleteSync) < 3) {
				LOGI(TAG, "...skipping; we synced already within the last 3 days");
				return;
			}

			ContentResolver resolver = executor.getContext().getContentResolver();
			long startTime = System.currentTimeMillis();

			insertSelf(resolver, account.name, startTime, syncResult);

			RemoteBuddiesHandler handler = new RemoteBuddiesHandler();
			String url = new UserUrlBuilder(account.name).buddies().build();
			executor.executePagedGet(url, handler);
			// syncResult.stats.numInserts += handler.getNumInserts();
			// syncResult.stats.numUpdates += handler.getNumUpdates();

			// TODO: delete avatar images associated with this list
			int count = resolver.delete(Buddies.CONTENT_URI, Buddies.UPDATED_LIST + "<?",
				new String[] { String.valueOf(startTime) });
			// syncResult.stats.numDeletes += count;

			accountManager.setUserData(account, SyncService.TIMESTAMP_BUDDIES, String.valueOf(startTime));
		} finally {
			LOGI(TAG, "...complete!");
		}
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_buddies_list;
	}

	private void insertSelf(ContentResolver resolver, String username, long startTime, SyncResult syncResult) {
		int selfId = 0;
		Uri uri = Buddies.buildBuddyUri(selfId);

		ContentValues values = new ContentValues();
		values.put(Buddies.UPDATED_LIST, startTime);
		values.put(Buddies.BUDDY_NAME, username);

		if (ResolverUtils.rowExists(resolver, uri)) {
			resolver.update(uri, values, null, null);
			// syncResult.stats.numUpdates++;
		} else {
			values.put(Buddies.BUDDY_ID, selfId);
			resolver.insert(Buddies.CONTENT_URI, values);
			// syncResult.stats.numInserts++;
		}
	}
}
