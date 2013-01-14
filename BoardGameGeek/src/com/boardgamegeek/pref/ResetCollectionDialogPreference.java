package com.boardgamegeek.pref;

import android.content.Context;
import android.util.AttributeSet;

import com.boardgamegeek.R;
import com.boardgamegeek.service.SyncService;

public class ResetCollectionDialogPreference extends AsyncDialogPreference {

	public ResetCollectionDialogPreference(Context context) {
		super(context);
	}

	public ResetCollectionDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected Task getTask() {
		return new Task();
	}

	@Override
	protected int getInfoMessageResource() {
		return R.string.pref_sync_reset_collection_info_message;
	}

	@Override
	protected int getConfirmMessageResource() {
		return R.string.pref_sync_reset_confirm_message;
	}

	private class Task extends AsyncDialogPreference.Task {

		@Override
		protected Void doInBackground(Void... params) {
			SyncService.clearCollection(getContext());
			SyncService.sync(getContext(), SyncService.FLAG_SYNC_COLLECTION);
			return null;
		}
	}
}
