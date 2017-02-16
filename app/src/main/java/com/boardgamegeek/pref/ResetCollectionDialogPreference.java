package com.boardgamegeek.pref;

import android.content.Context;
import android.util.AttributeSet;

import com.boardgamegeek.tasks.ResetCollectionTask;
import com.boardgamegeek.tasks.ToastingAsyncTask;

public class ResetCollectionDialogPreference extends AsyncDialogPreference {
	public ResetCollectionDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected ToastingAsyncTask getTask() {
		return new ResetCollectionTask(getContext());
	}
}
