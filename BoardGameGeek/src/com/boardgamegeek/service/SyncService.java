package com.boardgamegeek.service;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.text.SpannableString;

import com.boardgamegeek.R;
import com.boardgamegeek.auth.Authenticator;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.ui.PlaysActivity;
import com.boardgamegeek.util.NotificationUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.StringUtils;

public class SyncService extends Service {
	public static final String EXTRA_SYNC_TYPE = "com.boardgamegeek.SYNC_TYPE";
	public static final int FLAG_SYNC_COLLECTION = 0x00000001;
	public static final int FLAG_SYNC_BUDDIES = 0x00000002;
	public static final int FLAG_SYNC_PLAYS_DOWNLOAD = 0x00000004;
	public static final int FLAG_SYNC_PLAYS_UPLOAD = 0x00000008;
	public static final int FLAG_SYNC_PLAYS = FLAG_SYNC_PLAYS_DOWNLOAD | FLAG_SYNC_PLAYS_UPLOAD;
	public static final int FLAG_SYNC_ALL = FLAG_SYNC_COLLECTION | FLAG_SYNC_BUDDIES | FLAG_SYNC_PLAYS;
	public static final String ACTION_CANCEL_SYNC = "com.boardgamegeek.ACTION_SYNC_CANCEL";
	public static final String ACTION_PLAY_ID_CHANGED = "com.boardgamegeek.SyncService.ACTION_PLAY_ID_CHANGED";

	public static final String TIMESTAMP_COLLECTION_COMPLETE = "com.boardgamegeek.TIMESTAMP_COLLECTION_COMPLETE";
	public static final String TIMESTAMP_COLLECTION_PARTIAL = "com.boardgamegeek.TIMESTAMP_COLLECTION_PARTIAL";
	public static final String TIMESTAMP_BUDDIES = "com.boardgamegeek.TIMESTAMP_BUDDIES";
	public static final String TIMESTAMP_PLAYS_NEWEST_DATE = "com.boardgamegeek.TIMESTAMP_PLAYS_NEWEST_DATE";
	public static final String TIMESTAMP_PLAYS_OLDEST_DATE = "com.boardgamegeek.TIMESTAMP_PLAYS_OLDEST_DATE";

	public static final int INVALID_H_INDEX = -1;

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
		NotificationUtils.cancel(context, NotificationUtils.ID_SYNC);
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

	public static void hIndex(Context context) {
		int hIndex = calculateHIndex(context);
		if (hIndex != INVALID_H_INDEX) {
			int oldHIndex = PreferencesUtils.getHIndex(context);
			if (oldHIndex != hIndex) {
				PreferencesUtils.putHIndex(context, hIndex);
				notifyHIndex(context, hIndex, oldHIndex);
			}
		}
	}

	private static int calculateHIndex(Context context) {
		int hIndex = INVALID_H_INDEX;
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(Plays.CONTENT_SUM_URI,
				new String[] { "SUM(" + Plays.QUANTITY + ") as count" }, Plays.SYNC_STATUS + "=?",
				new String[] { String.valueOf(Play.SYNC_STATUS_SYNCED) }, "count DESC");
			if (cursor != null) {
				int i = 1;
				while (cursor.moveToNext()) {
					int numPlays = cursor.getInt(0);
					if (i > numPlays) {
						hIndex = i - 1;
						break;
					}
					if (numPlays == 0) {
						hIndex = 0;
						break;
					}
					i++;
				}
				if (hIndex == INVALID_H_INDEX) {
					hIndex = cursor.getCount();
				}
			}
		} finally {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
		return hIndex;
	}

	private static void notifyHIndex(Context context, int hIndex, int oldHIndex) {
		int messageId;
		if (hIndex > oldHIndex) {
			messageId = R.string.sync_notification_h_index_increase;
		} else {
			messageId = R.string.sync_notification_h_index_decrease;
		}
		SpannableString ss = StringUtils.boldSecondString(context.getString(messageId), String.valueOf(hIndex));
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://boardgamegeek.com/thread/953084"));
		PendingIntent pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		NotificationCompat.Builder builder = NotificationUtils
			.createNotificationBuilder(context, R.string.sync_notification_title_h_index, PlaysActivity.class)
			.setContentText(ss).setContentIntent(pi);
		NotificationUtils.notify(context, NotificationUtils.ID_H_INDEX, builder);
	}
}
