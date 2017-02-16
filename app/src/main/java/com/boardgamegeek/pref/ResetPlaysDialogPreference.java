package com.boardgamegeek.pref;

import android.content.Context;
import android.util.AttributeSet;

import com.boardgamegeek.tasks.ResetPlaysTask;
import com.boardgamegeek.tasks.ToastingAsyncTask;

public class ResetPlaysDialogPreference extends AsyncDialogPreference {
	public ResetPlaysDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected ToastingAsyncTask getTask() {
		return new ResetPlaysTask(getContext());
	}
}
