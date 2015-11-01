package com.boardgamegeek.service;

import android.content.Context;
import android.support.annotation.NonNull;

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
	public String getDescription() {
		return "invalid sync type " + syncType;
	}

	@Override
	public boolean isValid() {
		return false;
	}
}
