package com.boardgamegeek.tasks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.widget.Toast;

/**
 * A task that requires no input or output other than a context that will show a success or failure message via a Toast
 * when complete.
 */
public abstract class ToastingAsyncTask extends AsyncTask<Void, Void, Boolean> {
	@SuppressLint("StaticFieldLeak") @Nullable private final Context context;

	public ToastingAsyncTask(@Nullable Context context) {
		this.context = context == null ? null : context.getApplicationContext();
	}

	@Nullable
	protected Context getContext() {
		return context;
	}

	@StringRes
	protected abstract int getSuccessMessageResource();

	@StringRes
	protected abstract int getFailureMessageResource();

	@Override
	protected void onPostExecute(Boolean result) {
		@StringRes final int resId = result ? getSuccessMessageResource() : getFailureMessageResource();
		if (resId > 0) {
			Toast.makeText(context, resId, Toast.LENGTH_LONG).show();
		}
	}
}