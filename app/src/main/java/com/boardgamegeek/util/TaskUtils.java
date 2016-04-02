package com.boardgamegeek.util;

import android.os.AsyncTask;

/**
 * Execute async tasks in a version-safe manner.
 */
public class TaskUtils {
	private TaskUtils() {
	}

	@SafeVarargs
	public static <T> void executeAsyncTask(AsyncTask<T, ?, ?> task, T... params) {
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
	}
}
