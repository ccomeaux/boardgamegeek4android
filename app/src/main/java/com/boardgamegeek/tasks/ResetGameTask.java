package com.boardgamegeek.tasks;


import android.content.ContentValues;
import android.content.Context;

import com.boardgamegeek.provider.BggContract.Games;

public class ResetGameTask extends ToastingAsyncTask {
	public ResetGameTask(Context context) {
		super(context);
	}

	@Override
	protected int getSuccessMessageResource() {
		return 0;
	}

	@Override
	protected int getFailureMessageResource() {
		return 0;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		ContentValues cv = new ContentValues(3);
		cv.put(Games.UPDATED_LIST, 0);
		cv.put(Games.UPDATED, 0);
		cv.put(Games.UPDATED_PLAYS, 0);
		int rows = getContext().getContentResolver().update(Games.CONTENT_URI, cv, null, null);
		return rows > 0;
	}
}
