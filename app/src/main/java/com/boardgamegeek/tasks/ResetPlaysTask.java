package com.boardgamegeek.tasks;

import android.content.Context;

import com.boardgamegeek.R;
import com.boardgamegeek.service.SyncService;

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
		boolean success = SyncService.clearPlays(getContext());
		if (success) {
			SyncService.sync(getContext(), SyncService.FLAG_SYNC_PLAYS);
		}
		return success;
	}
}