package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.xmlpull.v1.XmlPullParserException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.SyncResult;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.RetryableException;
import com.boardgamegeek.model.CollectionResponse;
import com.boardgamegeek.model.persister.CollectionPersister;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.PreferencesUtils;

public class SyncCollectionListModifiedSince extends SyncTask {
	private static final String TAG = makeLogTag(SyncCollectionListModifiedSince.class);
	private static final int MAX_RETRIES = 5;
	private static final int RETRY_BACKOFF = 100;
	private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

	// TODO: add HH:MM:SS

	@Override
	public void execute(RemoteExecutor executor, Account account, SyncResult syncResult) throws IOException,
		XmlPullParserException {
		AccountManager accountManager = AccountManager.get(executor.getContext());
		long date = getLong(account, accountManager, SyncService.TIMESTAMP_COLLECTION_PARTIAL);

		LOGI(TAG, "Syncing collection list modified since " + new Date(date) + "...");
		try {
			String[] statuses = PreferencesUtils.getSyncStatuses(executor.getContext());
			if (statuses == null || statuses.length == 0) {
				LOGI(TAG, "...no statuses set to sync");
				return;
			}

			long lastFullSync = getLong(account, accountManager, SyncService.TIMESTAMP_COLLECTION_COMPLETE);
			if (DateTimeUtils.howManyHoursOld(lastFullSync) < 3) {
				LOGI(TAG, "...skipping; we just did a complete sync");
			}

			CollectionPersister persister = new CollectionPersister(executor.getContext()).includeStats();
			BggService service = Adapter.createWithAuthRetry(executor.getContext());
			String modifiedSince = FORMAT.format(new Date(date));

			boolean cancelled = false;
			for (int i = 0; i < statuses.length; i++) {
				if (isCancelled()) {
					cancelled = true;
					break;
				}
				LOGI(TAG, "...syncing status [" + statuses[i] + "]");
				CollectionResponse response = getResponse(service, account.name, statuses[i], modifiedSince);
				if (response == null) {
					continue;
				}
				persister.save(response.items);
			}
			if (!cancelled) {
				accountManager.setUserData(account, SyncService.TIMESTAMP_COLLECTION_PARTIAL,
					String.valueOf(persister.getTimeStamp()));
			}
		} finally {
			LOGI(TAG, "...complete!");
		}
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_collection_partial;
	}

	private long getLong(Account account, AccountManager accountManager, String key) {
		String l = accountManager.getUserData(account, key);
		return TextUtils.isEmpty(l) ? 0 : Long.parseLong(l);
	}

	private CollectionResponse getResponse(BggService service, String username, String status, String modifiedSince) {
		Map<String, String> statuses = new HashMap<String, String>();
		statuses.put(status, "1");

		int retries = 0;
		while (true) {
			try {
				return service.collection(username, statuses, 0, 1, modifiedSince);
			} catch (Exception e) {
				if (e instanceof RetryableException || e.getCause() instanceof RetryableException) {
					retries++;
					if (retries > MAX_RETRIES) {
						break;
					}
					try {
						LOGI(TAG, "...retrying #" + retries);
						Thread.sleep(retries * retries * RETRY_BACKOFF);
					} catch (InterruptedException e1) {
						LOGI(TAG, "Interrupted while sleeping before retry " + retries);
						break;
					}
				} else {
					throw e;
				}
			}
		}
		return null;
	}
}
