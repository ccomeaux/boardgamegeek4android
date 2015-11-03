package com.boardgamegeek.service;

import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;

public class InvalidUpdateTask extends UpdateTask {
	private final int syncType;

	public InvalidUpdateTask(int syncType) {
		this.syncType = syncType;
	}

	@Override
	public void execute(Context context) {
	}

	@NonNull
	@Override
	public String getDescription(Context context) {
		return context.getString(R.string.sync_update_invalid_sync_type, syncType);
	}

	@Override
	public boolean isValid() {
		return false;
	}
}
