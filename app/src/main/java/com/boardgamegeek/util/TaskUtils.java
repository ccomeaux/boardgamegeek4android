package com.boardgamegeek.util;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;

/**
 * Execute async tasks in a version-safe manner.
 */
public class TaskUtils {
	private TaskUtils() {
	}

	@SafeVarargs
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static <T> void executeAsyncTask(AsyncTask<T, ?, ?> task, T... params) {
		if (VersionUtils.hasHoneycomb()) {
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
		} else {
			task.execute(params);
		}
	}
}
