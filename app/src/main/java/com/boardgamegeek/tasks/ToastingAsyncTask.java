package com.boardgamegeek.tasks;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import android.widget.Toast;

import com.boardgamegeek.extensions.AsyncTaskKt;

import kotlin.Unit;
import kotlinx.coroutines.CoroutineScope;

/**
 * A task that requires no input or output other than a context that will show a success or failure message via a Toast
 * when complete.
 */
public abstract class ToastingAsyncTask {
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

	protected abstract Boolean doInBackground();

	public void execute() {
		AsyncTaskKt.launchTaskWithResult(
			() -> doInBackground(),
			result -> {
				@StringRes final int resId = result ? getSuccessMessageResource() : getFailureMessageResource();
				if (resId != 0 && context != null) {
					Toast.makeText(context, resId, Toast.LENGTH_LONG).show();
				}
				return Unit.INSTANCE;
			}
		);
	}
}