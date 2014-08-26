package com.boardgamegeek.service;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.accounts.Account;
import android.content.Context;
import android.content.SyncResult;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.CollectionResponse;
import com.boardgamegeek.model.persister.CollectionPersister;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.util.PreferencesUtils;

/**
 * Syncs the user's collection in brief mode, one collection status at a time.
 */
public class SyncCollectionListComplete extends SyncTask {
	private static final String TAG = makeLogTag(SyncCollectionListComplete.class);
	private static final String STATUS_PLAYED = "played";

	public SyncCollectionListComplete(BggService service) {
		super(service);
	}

	@Override
	public void execute(Context context, Account account, SyncResult syncResult) {
		LOGI(TAG, "Syncing full collection list...");
		boolean success = true;
		try {
			CollectionPersister persister = new CollectionPersister(context).brief();

			List<String> statuses = new ArrayList<String>(Arrays.asList(PreferencesUtils.getSyncStatuses(context)));
			if (statuses.remove(STATUS_PLAYED)) {
				statuses.add(0, STATUS_PLAYED);
			}

			for (int i = 0; i < statuses.size(); i++) {
				if (isCancelled()) {
					success = false;
					break;
				}

				String status = statuses.get(i);
				if (TextUtils.isEmpty(status)) {
					LOGI(TAG, "...skipping blank status");
					continue;
				}
				LOGI(TAG, "...syncing status [" + status + "]");

				Map<String, String> options = new HashMap<String, String>();
				options.put(BggService.COLLECTION_QUERY_KEY_BRIEF, "1");
				options.put(status, "1");
				for (int j = 0; j < i; j++) {
					options.put(statuses.get(j), "0");
				}

				requestAndPersist(account.name, persister, options, syncResult);

				options.put(BggService.COLLECTION_QUERY_KEY_SUBTYPE, BggService.THING_SUBTYPE_BOARDGAME_ACCESSORY);
				requestAndPersist(account.name, persister, options, syncResult);
			}

			if (success) {
				LOGI(TAG, "...deleting old collection entries");
				// Delete all collection items that weren't updated in the sync above
				int count = context.getContentResolver().delete(Collection.CONTENT_URI, Collection.UPDATED_LIST + "<?",
					new String[] { String.valueOf(persister.getTimeStamp()) });
				LOGI(TAG, "...deleted " + count + " old collection entries");
				// TODO: delete games as well?!
				// TODO: delete thumbnail images associated with this list (both collection and game)

				Authenticator.putLong(context, SyncService.TIMESTAMP_COLLECTION_COMPLETE, persister.getTimeStamp());
				Authenticator.putLong(context, SyncService.TIMESTAMP_COLLECTION_PARTIAL, persister.getTimeStamp());
			}
		} finally {
			LOGI(TAG, "...complete!");
		}
	}

	private void requestAndPersist(String username, CollectionPersister persister, Map<String, String> options,
		SyncResult syncResult) {
		CollectionResponse response = getCollectionResponse(mService, username, options);
		if (response.items != null && response.items.size() > 0) {
			int rows = persister.save(response.items);
			syncResult.stats.numEntries += response.items.size();
			LOGI(TAG, "...saved " + rows + " records for " + response.items.size() + " collection items");
		} else {
			LOGI(TAG, "...no collection items to save");
		}
	}

	@Override
	public int getNotification() {
		return R.string.sync_notification_collection_full;
	}
}
