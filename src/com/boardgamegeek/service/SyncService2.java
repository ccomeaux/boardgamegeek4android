package com.boardgamegeek.service;

import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.provider.BggContract;

import android.accounts.Account;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

public class SyncService2 extends Service {
	public static final String EXTRA_SYNC_TYPE = "com.boardgamegeek.SYNC_TYPE";
	public static final int FLAG_SYNC_COLLECTION = 0x00000001;
	public static final int FLAG_SYNC_BUDDIES = 0x00000002;
	public static final int FLAG_SYNC_PLAYS_DOWNLOAD = 0x00000004;
	public static final int FLAG_SYNC_PLAYS_UPLOAD = 0x00000008;
	public static final int FLAG_SYNC_PLAYS = FLAG_SYNC_PLAYS_DOWNLOAD | FLAG_SYNC_PLAYS_UPLOAD;
	public static final int FLAG_SYNC_ALL = FLAG_SYNC_COLLECTION | FLAG_SYNC_BUDDIES | FLAG_SYNC_PLAYS;

	public static final String TIMESTAMP_COLLECTION_COMPLETE = "TIMESTAMP_COLLECTION_COMPLETE";
	public static final String TIMESTAMP_COLLECTION_PARTIAL = "TIMESTAMP_COLLECTION_PARTIAL";

	private static final Object sSyncAdapterLock = new Object();
	private static SyncAdapter sSyncAdapter = null;

	@Override
	public void onCreate() {
		synchronized (sSyncAdapterLock) {
			if (sSyncAdapter == null) {
				sSyncAdapter = new SyncAdapter(getApplicationContext(), false);
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return sSyncAdapter.getSyncAdapterBinder();
	}

	public static void sync(Context content, int syncType) {
		Account account = Authenticator.getAccount(content);
		if (account != null) {
			Bundle extras = new Bundle();
			extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
			extras.putInt(EXTRA_SYNC_TYPE, syncType);
			ContentResolver.requestSync(account, BggContract.CONTENT_AUTHORITY, extras);
		}
	}

	public static boolean isActiveOrPending(Context context) {
		Account account = Authenticator.getAccount(context);
		boolean syncActive = ContentResolver.isSyncActive(account, BggContract.CONTENT_AUTHORITY);
		boolean syncPending = ContentResolver.isSyncPending(account, BggContract.CONTENT_AUTHORITY);
		return syncActive || syncPending;
	}
}
