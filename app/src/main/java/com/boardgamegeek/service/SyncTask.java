package com.boardgamegeek.service;

import android.accounts.Account;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.util.NotificationUtils;
import com.boardgamegeek.util.PreferencesUtils;

public abstract class SyncTask extends ServiceTask {
	protected Context mContext;
	protected BggService mService;
	private boolean mShowNotifications;
	private boolean mIsCancelled = false;

	public SyncTask(Context context, BggService service) {
		mContext = context;
		mService = service;
		mShowNotifications = PreferencesUtils.getSyncShowNotifications(mContext);
	}

	public abstract int getSyncType();

	public abstract void execute(Account account, SyncResult syncResult);

	public void cancel() {
		mIsCancelled = true;
	}

	public boolean isCancelled() {
		return mIsCancelled;
	}

	protected void showNotification() {
		showNotification(getNotification(), null);
	}

	protected void showNotification(String detail) {
		showNotification(getNotification(), detail);
	}

	private void showNotification(int messageId, String detail) {
		if (!mShowNotifications) {
			return;
		}

		if (messageId == NO_NOTIFICATION) {
			return;
		}

		String message = mContext.getString(messageId);
		PendingIntent pi = PendingIntent.getBroadcast(mContext, 0, new Intent(SyncService.ACTION_CANCEL_SYNC), 0);
		NotificationCompat.Builder builder = NotificationUtils
			.createNotificationBuilder(mContext, R.string.sync_notification_title).setContentText(message)
			.setPriority(NotificationCompat.PRIORITY_LOW).setCategory(NotificationCompat.CATEGORY_SERVICE)
			.setOngoing(true).setProgress(1, 0, true)
			.addAction(R.drawable.ic_stat_cancel, mContext.getString(R.string.cancel), pi);
		if (!TextUtils.isEmpty(detail)) {
			builder.setStyle(new NotificationCompat.BigTextStyle().setSummaryText(message).bigText(detail));
		}
		NotificationUtils.notify(mContext, NotificationUtils.ID_SYNC, builder);
	}
}
