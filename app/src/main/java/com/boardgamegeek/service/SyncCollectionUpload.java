package com.boardgamegeek.service;

import android.accounts.Account;
import android.content.Context;
import android.content.SyncResult;

import com.boardgamegeek.io.BggService;

import hugo.weaving.DebugLog;
import timber.log.Timber;

public class SyncCollectionUpload extends SyncTask {
	@DebugLog
	public SyncCollectionUpload(Context context, BggService service) {
		super(context, service);
	}

	@DebugLog
	@Override
	public int getSyncType() {
		return SyncService.FLAG_SYNC_COLLECTION_UPLOAD;
	}

	@DebugLog
	@Override
	public void execute(Account account, SyncResult syncResult) {
		Timber.i("Collection uploading will go here.");
	}
}
