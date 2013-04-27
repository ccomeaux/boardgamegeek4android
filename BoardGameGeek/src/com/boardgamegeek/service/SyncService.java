package com.boardgamegeek.service;

import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.provider.BggContract;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

public class SyncService extends Service {
	public static final String EXTRA_SYNC_TYPE = "com.boardgamegeek.SYNC_TYPE";
	public static final int FLAG_SYNC_COLLECTION = 0x00000001;
	public static final int FLAG_SYNC_BUDDIES = 0x00000002;
	public static final int FLAG_SYNC_PLAYS_DOWNLOAD = 0x00000004;
	public static final int FLAG_SYNC_PLAYS_UPLOAD = 0x00000008;
	public static final int FLAG_SYNC_PLAYS = FLAG_SYNC_PLAYS_DOWNLOAD | FLAG_SYNC_PLAYS_UPLOAD;
	public static final int FLAG_SYNC_ALL = FLAG_SYNC_COLLECTION | FLAG_SYNC_BUDDIES | FLAG_SYNC_PLAYS;

	public static final String TIMESTAMP_COLLECTION_COMPLETE = "com.boardgamegeek.TIMESTAMP_COLLECTION_COMPLETE";
	public static final String TIMESTAMP_COLLECTION_PARTIAL = "com.boardgamegeek.TIMESTAMP_COLLECTION_PARTIAL";
	public static final String TIMESTAMP_BUDDIES = "com.boardgamegeek.TIMESTAMP_BUDDIES";
	public static final String TIMESTAMP_PLAYS_NEWEST_DATE = "com.boardgamegeek.TIMESTAMP_PLAYS_NEWEST_DATE";
	public static final String TIMESTAMP_PLAYS_OLDEST_DATE = "com.boardgamegeek.TIMESTAMP_PLAYS_OLDEST_DATE";

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

	public static void sync(Context context, int syncType) {
		Account account = Authenticator.getAccount(context);
		if (account != null) {
			Bundle extras = new Bundle();
			extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
			extras.putInt(EXTRA_SYNC_TYPE, syncType);
			ContentResolver.requestSync(account, BggContract.CONTENT_AUTHORITY, extras);
		}
	}

	public static void cancelSync(Context context) {
		Account account = Authenticator.getAccount(context);
		if (account != null) {
			ContentResolver.cancelSync(account, BggContract.CONTENT_AUTHORITY);
		}
	}

	public static boolean isActiveOrPending(Context context) {
		Account account = Authenticator.getAccount(context);
		boolean syncActive = ContentResolver.isSyncActive(account, BggContract.CONTENT_AUTHORITY);
		boolean syncPending = ContentResolver.isSyncPending(account, BggContract.CONTENT_AUTHORITY);
		return syncActive || syncPending;
	}

	public static boolean clearCollection(Context context) {
		AccountManager accountManager = AccountManager.get(context);
		Account account = Authenticator.getAccount(context);
		if (accountManager != null && account != null) {
			accountManager.setUserData(account, SyncService.TIMESTAMP_COLLECTION_COMPLETE, null);
			accountManager.setUserData(account, SyncService.TIMESTAMP_COLLECTION_PARTIAL, null);
			return true;
		}
		return false;
	}

	public static boolean clearBuddies(Context context) {
		AccountManager accountManager = AccountManager.get(context);
		Account account = Authenticator.getAccount(context);
		if (accountManager != null && account != null) {
			accountManager.setUserData(account, SyncService.TIMESTAMP_BUDDIES, null);
			return true;
		}
		return false;
	}

	public static boolean clearPlays(Context context) {
		AccountManager accountManager = AccountManager.get(context);
		Account account = Authenticator.getAccount(context);
		if (accountManager != null && account != null) {
			accountManager.setUserData(account, SyncService.TIMESTAMP_PLAYS_NEWEST_DATE, null);
			accountManager.setUserData(account, SyncService.TIMESTAMP_PLAYS_OLDEST_DATE, null);
			return true;
		}
		return false;
	}
}
