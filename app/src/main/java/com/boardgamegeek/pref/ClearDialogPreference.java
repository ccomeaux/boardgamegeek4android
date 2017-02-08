package com.boardgamegeek.pref;

import android.content.Context;
import android.util.AttributeSet;

import com.boardgamegeek.tasks.ClearDatabaseTask;
import com.boardgamegeek.tasks.ToastingAsyncTask;

public class ClearDialogPreference extends AsyncDialogPreference {
	public ClearDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected ToastingAsyncTask getTask() {
		return new ClearDatabaseTask(getContext());
	}
}
