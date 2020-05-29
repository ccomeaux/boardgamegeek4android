package com.boardgamegeek.tasks;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.pref.SyncPrefUtils;
import com.boardgamegeek.pref.SyncPrefs;
import com.boardgamegeek.service.SyncService;

/**
 * Clears the collection sync timestamps and requests a full collection sync be performed.
 */
public class ResetCollectionTask extends ToastingAsyncTask {
	public ResetCollectionTask(Context context) {
		super(context);
	}

	@Override
	protected int getSuccessMessageResource() {
		return R.string.pref_sync_reset_success;
	}

	@Override
	protected int getFailureMessageResource() {
		return R.string.pref_sync_reset_failure;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		if (getContext() == null) return false;
		SyncPrefUtils.clearCollection(SyncPrefs.getPrefs(getContext()));
		SyncService.sync(getContext(), SyncService.FLAG_SYNC_COLLECTION);
		return true;
	}
}
