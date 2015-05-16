package com.boardgamegeek.pref;

import android.content.Context;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.widget.Toast;

import com.boardgamegeek.util.TaskUtils;

public abstract class AsyncDialogPreference extends ConfirmDialogPreference {

	protected abstract Task getTask();

	protected abstract int getSuccessMessageResource();

	protected abstract int getFailureMessageResource();

	public AsyncDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void execute() {
		TaskUtils.executeAsyncTask(getTask());
	}

	protected abstract class Task extends AsyncTask<Void, Void, Boolean> {
		@Override
		protected void onPostExecute(Boolean result) {
			final int resId = result ? getSuccessMessageResource() : getFailureMessageResource();
			if (resId > 0) {
				Toast.makeText(getContext(), resId, Toast.LENGTH_LONG).show();
			}
		}
	}
}
