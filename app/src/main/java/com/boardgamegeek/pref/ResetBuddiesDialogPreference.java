package com.boardgamegeek.pref;

import android.content.Context;
import android.util.AttributeSet;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.service.SyncService;

import timber.log.Timber;

public class ResetBuddiesDialogPreference extends AsyncDialogPreference {
	public ResetBuddiesDialogPreference(Context context, AttributeSet attrs) {
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
			boolean success = SyncService.clearBuddies(getContext());
			if (success) {
				int count = getContext().getContentResolver().delete(Buddies.CONTENT_URI, null, null);
				//TODO remove buddy colors
				Timber.i("Removed " + count + " GeekBuddies");
				SyncService.sync(getContext(), SyncService.FLAG_SYNC_BUDDIES);
			}
			return success;
		}
	}
}
