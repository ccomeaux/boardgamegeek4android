package com.boardgamegeek.service;

import android.content.Context;

import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.XmlHandler.HandlerException;

public abstract class SyncTask {
	public static final int NO_NOTIFICATION = 0;

	private boolean mIsBggDown;

	public abstract void execute(RemoteExecutor executor, Context context) throws HandlerException;

	public int getNotification() {
		return NO_NOTIFICATION;
	}

	public boolean isBggDown() {
		return mIsBggDown;
	}

	protected void setIsBggDown(boolean value) {
		mIsBggDown = value;
	}
}
