package com.boardgamegeek.service;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import android.accounts.Account;
import android.content.SyncResult;

import com.boardgamegeek.io.RemoteExecutor;

public abstract class SyncTask extends ServiceTask {
	public abstract void execute(RemoteExecutor executor, Account account, SyncResult syncResult) throws IOException,
		XmlPullParserException;
}
