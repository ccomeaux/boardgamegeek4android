package com.boardgamegeek.service;

import android.content.Context;

import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.XmlHandler.HandlerException;

public abstract class SyncTask {

	private boolean mIsBggDown;

	public abstract void execute(RemoteExecutor executor, Context context) throws HandlerException;

	public abstract int getNotification();

	public boolean isBggDown() {
		return mIsBggDown;
	}

	protected void setIsBggDown(boolean value) {
		mIsBggDown = value;
	}
}
