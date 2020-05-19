package com.boardgamegeek.tasks;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.pref.SyncPrefUtils;
import com.boardgamegeek.pref.SyncPrefs;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.service.SyncService;

import timber.log.Timber;

/**
 * Clears the GeekBuddies sync timestamps and requests a full GeekBuddies sync be performed.
 */
public class ResetBuddiesTask extends ToastingAsyncTask {
	public ResetBuddiesTask(Context context) {
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
		SyncPrefUtils.clearBuddyListTimestamps(SyncPrefs.getPrefs(getContext()));
		int count = getContext().getContentResolver().delete(Buddies.CONTENT_URI, null, null);
		//TODO remove buddy colors
		Timber.i("Removed %d GeekBuddies", count);
		SyncService.sync(getContext(), SyncService.FLAG_SYNC_BUDDIES);
		return true;
	}
}
