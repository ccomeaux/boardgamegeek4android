package com.boardgamegeek.service;

import com.boardgamegeek.io.BggService;

import android.accounts.Account;
import android.content.Context;
import android.content.SyncResult;

public abstract class SyncTask extends ServiceTask {
	protected BggService mService;

	public SyncTask(BggService service) {
		mService = service;
	}

	public abstract void execute(Context context, Account account, SyncResult syncResult);

	private boolean mIsCancelled = false;

	public void cancel() {
		mIsCancelled = true;
	}

	public boolean isCancelled() {
		return mIsCancelled;
	}
}
