package com.boardgamegeek.service;

import android.content.Context;

import com.boardgamegeek.io.RemoteExecutor;
import com.boardgamegeek.io.XmlHandler.HandlerException;

public abstract class SyncTask extends ServiceTask {
	public abstract void execute(RemoteExecutor executor, Context context) throws HandlerException;
}
