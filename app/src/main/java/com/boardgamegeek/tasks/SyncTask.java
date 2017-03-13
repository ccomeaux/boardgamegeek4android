package com.boardgamegeek.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.boardgamegeek.util.NetworkUtils;

import hugo.weaving.DebugLog;

abstract class SyncTask extends AsyncTask<Void, Void, String> {
	protected final Context context;

	SyncTask(Context context) {
		this.context = context;
	}

	@DebugLog
	@Override
	protected String doInBackground(Void... params) {
		if (context == null) return "Null context";
		if (NetworkUtils.isOffline(context)) return "Offline";
		String errorMessage = doInBackground();
		return errorMessage;
	}

	abstract String doInBackground();
}
