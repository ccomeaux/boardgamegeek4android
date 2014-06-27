package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.Context;
import android.content.SyncResult;

public abstract class SyncTask extends ServiceTask {
	public abstract void execute(Context context, Account account, SyncResult syncResult);

	private boolean mIsCancelled = false;

	public void cancel() {
		mIsCancelled = true;
	}

	public boolean isCancelled() {
		return mIsCancelled;
	}
}
