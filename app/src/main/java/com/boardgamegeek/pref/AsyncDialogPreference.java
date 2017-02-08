package com.boardgamegeek.pref;

import android.content.Context;
import android.util.AttributeSet;

import com.boardgamegeek.tasks.ToastingAsyncTask;
import com.boardgamegeek.util.TaskUtils;

public abstract class AsyncDialogPreference extends ConfirmDialogPreference {
	protected abstract ToastingAsyncTask getTask();

	public AsyncDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void execute() {
		TaskUtils.executeAsyncTask(getTask());
	}
}
