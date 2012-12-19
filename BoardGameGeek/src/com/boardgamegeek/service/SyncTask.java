package com.boardgamegeek.service;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;

import com.boardgamegeek.io.RemoteExecutor;

public abstract class SyncTask extends ServiceTask {
	public abstract void execute(RemoteExecutor executor, Context context) throws IOException, XmlPullParserException;
}
