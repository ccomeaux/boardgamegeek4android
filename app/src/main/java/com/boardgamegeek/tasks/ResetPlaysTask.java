package com.boardgamegeek.tasks;

import android.content.ContentValues;
import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.pref.SyncPrefUtils;
import com.boardgamegeek.pref.SyncPrefs;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.service.SyncService;

import timber.log.Timber;

/**
 * Clears the plays sync timestamps and requests a full plays sync be performed.
 */
public class ResetPlaysTask extends ToastingAsyncTask {
	public ResetPlaysTask(Context context) {
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
		SyncPrefUtils.clearPlaysTimestamps(SyncPrefs.getPrefs(getContext()));
		ContentValues values = new ContentValues(1);
		values.put(Plays.SYNC_HASH_CODE, 0);
		int count = getContext().getContentResolver().update(Plays.CONTENT_URI, values, null, null);
		Timber.d("Cleared the hashcode from %,d plays.", count);
		SyncService.sync(getContext(), SyncService.FLAG_SYNC_PLAYS);
		return true;
	}
}