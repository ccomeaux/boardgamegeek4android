package com.boardgamegeek.service;

import android.content.Context;

import com.boardgamegeek.io.RemoteBggHandler;
import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.XmlHandler.HandlerException;

public abstract class UpdateTask extends ServiceTask {
	protected String mErrorMessage;

	public String getErrorMessage() {
		if (isBggDown()) {
			return "BGG appears to be down."; // getResources().getString(R.string.notification_bgg_down);
		}
		return mErrorMessage;
	}

	public abstract void execute(RemoteExecutor executor, Context context);

	protected void safelyExecuteGet(RemoteExecutor executor, String url, RemoteBggHandler handler) {
		try {
			executor.executeGet(url, handler);
		} catch (HandlerException e) {
			mErrorMessage = e.toString();
		}
		setIsBggDown(handler.isBggDown());
	}
}
