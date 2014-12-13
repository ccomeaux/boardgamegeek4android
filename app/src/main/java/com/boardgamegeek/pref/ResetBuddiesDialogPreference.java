package com.boardgamegeek.pref;

import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.service.SyncService;

import android.content.Context;
import android.util.AttributeSet;

public class ResetBuddiesDialogPreference extends AsyncDialogPreference {
	private static final String TAG = makeLogTag(ResetBuddiesDialogPreference.class);

	public ResetBuddiesDialogPreference(Context context) {
		super(context);
	}

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
				LOGI(TAG, "Removed " + count + " GeekBuddies");
				SyncService.sync(getContext(), SyncService.FLAG_SYNC_BUDDIES);
			}
			return success;
		}
	}
}
