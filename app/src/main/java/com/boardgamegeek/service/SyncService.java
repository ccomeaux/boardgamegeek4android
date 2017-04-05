package com.boardgamegeek.service;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.model.PlayStats;
import com.boardgamegeek.util.NotificationUtils;
import com.boardgamegeek.util.PreferencesUtils;

public class SyncService extends Service {
	public static final String EXTRA_SYNC_TYPE = "com.boardgamegeek.SYNC_TYPE";
	public static final int FLAG_SYNC_NONE = 0;
	public static final int FLAG_SYNC_COLLECTION_DOWNLOAD = 1;
	public static final int FLAG_SYNC_COLLECTION_UPLOAD = 1 << 1;
	public static final int FLAG_SYNC_BUDDIES = 1 << 2;
	public static final int FLAG_SYNC_PLAYS_DOWNLOAD = 1 << 3;
	public static final int FLAG_SYNC_PLAYS_UPLOAD = 1 << 4;
	public static final int FLAG_SYNC_COLLECTION = FLAG_SYNC_COLLECTION_DOWNLOAD | FLAG_SYNC_COLLECTION_UPLOAD;
	public static final int FLAG_SYNC_PLAYS = FLAG_SYNC_PLAYS_DOWNLOAD | FLAG_SYNC_PLAYS_UPLOAD;
	public static final int FLAG_SYNC_ALL = FLAG_SYNC_COLLECTION | FLAG_SYNC_BUDDIES | FLAG_SYNC_PLAYS;
	public static final String ACTION_CANCEL_SYNC = "com.boardgamegeek.ACTION_SYNC_CANCEL";

	public static final String TIMESTAMP_COLLECTION_COMPLETE = "com.boardgamegeek.TIMESTAMP_COLLECTION_COMPLETE";
	public static final String TIMESTAMP_COLLECTION_PARTIAL = "com.boardgamegeek.TIMESTAMP_COLLECTION_PARTIAL";
	public static final String TIMESTAMP_BUDDIES = "com.boardgamegeek.TIMESTAMP_BUDDIES";
	public static final String TIMESTAMP_PLAYS_NEWEST_DATE = "com.boardgamegeek.TIMESTAMP_PLAYS_NEWEST_DATE";
	public static final String TIMESTAMP_PLAYS_OLDEST_DATE = "com.boardgamegeek.TIMESTAMP_PLAYS_OLDEST_DATE";

	private static final Object SYNC_ADAPTER_LOCK = new Object();
	@Nullable private static SyncAdapter syncAdapter = null;

	@Override
	public void onCreate() {
		synchronized (SYNC_ADAPTER_LOCK) {
			if (syncAdapter == null) {
				syncAdapter = new SyncAdapter(getApplicationContext());
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return syncAdapter != null ? syncAdapter.getSyncAdapterBinder() : null;
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
		NotificationUtils.cancel(context, NotificationUtils.TAG_SYNC_PROGRESS);
		Account account = Authenticator.getAccount(context);
		if (account != null) {
			ContentResolver.cancelSync(account, BggContract.CONTENT_AUTHORITY);
		}
	}

	public static boolean isActiveOrPending(Context context) {
		Account account = Authenticator.getAccount(context);
		if (account == null) {
			return false;
		}
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

	public static void calculateAndUpdateGameHIndex(@NonNull Context context) {
		int hIndex = calculateGameHIndex(context);
		PreferencesUtils.updateGameHIndex(context, hIndex);
	}

	private static int calculateGameHIndex(@NonNull Context context) {
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(
				PlayStats.getUri(false),
				PlayStats.PROJECTION,
				PlayStats.getSelection(context),
				PlayStats.getSelectionArgs(context),
				PlayStats.getSortOrder());
			if (cursor != null) {
				PlayStats stats = PlayStats.fromCursor(cursor);
				return stats.getHIndex();
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
		return PreferencesUtils.INVALID_H_INDEX;
	}

	public static boolean isPlaysSyncUpToDate(Context context) {
		return Authenticator.getLong(context, SyncService.TIMESTAMP_PLAYS_OLDEST_DATE, Long.MAX_VALUE) == 0;
	}
}
