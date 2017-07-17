package com.boardgamegeek.service;

import android.accounts.Account;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.support.annotation.PluralsRes;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.BigTextStyle;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.util.NotificationUtils;
import com.boardgamegeek.util.PreferencesUtils;

import timber.log.Timber;

public abstract class SyncTask extends ServiceTask {
	protected final Context context;
	protected final BggService service;
	private final boolean shouldShowNotifications;
	private boolean isCancelled = false;

	public SyncTask(Context context, BggService service) {
		this.context = context;
		this.service = service;
		shouldShowNotifications = PreferencesUtils.getSyncShowNotifications(this.context);
	}

	public abstract int getSyncType();

	public abstract void execute(Account account, SyncResult syncResult);

	public void cancel() {
		isCancelled = true;
	}

	public boolean isCancelled() {
		return isCancelled;
	}

	protected void updateProgressNotification() {
		updateProgressNotification(null);
	}

	protected void updateProgressNotification(@StringRes int detailResId) {
		updateProgressNotification(context.getString(detailResId));
	}

	protected void updateProgressNotification(@StringRes int detailResId, Object... formatArgs) {
		updateProgressNotification(context.getString(detailResId, formatArgs));
	}

	protected void updateProgressNotificationAsPlural(@PluralsRes int detailResId, int quantity, Object... formatArgs) {
		updateProgressNotification(context.getResources().getQuantityString(detailResId, quantity, formatArgs));
	}

	protected void updateProgressNotification(String detail) {
		Timber.i(detail);
		if (!shouldShowNotifications) return;

		String message = "";
		if (getNotificationSummaryMessageId() != NO_NOTIFICATION) {
			message = context.getString(getNotificationSummaryMessageId());
		}

		PendingIntent pi = PendingIntent.getBroadcast(context, 0, new Intent(SyncService.ACTION_CANCEL_SYNC), 0);
		NotificationCompat.Builder builder = NotificationUtils
			.createNotificationBuilder(context, R.string.sync_notification_title, NotificationUtils.CHANNEL_ID_SYNC_PROGRESS)
			.setContentText(message)
			.setPriority(NotificationCompat.PRIORITY_LOW)
			.setCategory(NotificationCompat.CATEGORY_SERVICE)
			.setOngoing(true)
			.setProgress(1, 0, true)
			.addAction(R.drawable.ic_stat_cancel, context.getString(R.string.cancel), pi);
		if (!TextUtils.isEmpty(detail)) {
			final BigTextStyle bigTextStyle = new BigTextStyle().bigText(detail);
			builder.setStyle(bigTextStyle);
		}
		NotificationUtils.notify(context, NotificationUtils.TAG_SYNC_PROGRESS, 0, builder);
	}

	protected void showError(String message) {
		Timber.w(message);

		if (!PreferencesUtils.getSyncShowErrors(context)) return;

		NotificationCompat.Builder builder = NotificationUtils
			.createNotificationBuilder(context, R.string.sync_notification_title_error, NotificationUtils.CHANNEL_ID_ERROR)
			.setContentText(message)
			.setPriority(NotificationCompat.PRIORITY_HIGH)
			.setCategory(NotificationCompat.CATEGORY_ERROR);

		NotificationUtils.notify(context, NotificationUtils.TAG_SYNC_ERROR, 0, builder);
	}

	protected boolean wasSleepInterrupted(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Timber.w(e, "Sleeping interrupted during sync.");
			return true;
		}
		return false;
	}
}
