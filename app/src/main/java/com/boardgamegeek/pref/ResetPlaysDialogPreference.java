package com.boardgamegeek.pref;

import android.content.Context;
import android.util.AttributeSet;

import com.boardgamegeek.R;
import com.boardgamegeek.service.SyncService;

public class ResetPlaysDialogPreference extends AsyncDialogPreference {

	public ResetPlaysDialogPreference(Context context) {
		super(context);
	}

	public ResetPlaysDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected Task getTask() {
		return new Task();
	}

	@Override
	protected int getSuccessMessageResource() {
		return R.string.pref_sync_reset_success;
	}

	@Override
	protected int getFailureMessageResource() {
		return R.string.pref_sync_reset_failure;
	}

	private class Task extends AsyncDialogPreference.Task {

		@Override
		protected Boolean doInBackground(Void... params) {
			boolean success = SyncService.clearPlays(getContext());
			if (success) {
				SyncService.sync(getContext(), SyncService.FLAG_SYNC_PLAYS);
			}
			return success;
		}
	}
}
