package com.boardgamegeek.service;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;

import com.boardgamegeek.io.RemoteBggHandler;
import com.boardgamegeek.io.RemoteExecutor;

public abstract class UpdateTask extends ServiceTask {
	protected String mErrorMessage;

	public String getErrorMessage() {
		if (isBggDown()) {
			return "BGG is down."; // getResources().getString(R.string.notification_bgg_down);
		}
		return mErrorMessage;
	}

	public abstract void execute(RemoteExecutor executor, Context context);

	protected void safelyExecuteGet(RemoteExecutor executor, String url, RemoteBggHandler handler) {
		try {
			executor.executeGet(url, handler);
		} catch (IOException e) {
			mErrorMessage = e.toString();
		} catch (XmlPullParserException e) {
			mErrorMessage = e.toString();
		}
		setIsBggDown(handler.isBggDown());
	}
}
