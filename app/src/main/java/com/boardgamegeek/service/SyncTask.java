package com.boardgamegeek.service;

import android.accounts.Account;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.support.annotation.NonNull;
import android.support.annotation.PluralsRes;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.BigTextStyle;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.util.NotificationUtils;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.PresentationUtils;

import timber.log.Timber;

public abstract class SyncTask {
	public static final int NO_NOTIFICATION = 0;

	protected final Context context;
	protected final BggService service;
	private boolean isCancelled = false;

	public SyncTask(Context context, BggService service) {
		this.context = context;
		this.service = service;
	}

	/**
	 * Unique ID for this sync class.
	 */
	public abstract int getSyncType();

	/**
	 * Perform the sync operation.
	 */
	public abstract void execute(Account account, SyncResult syncResult);

	/**
	 * Call this to cancel the task. If the task is running, it will cancel it's process at the earliest convenient
	 * time, as determined by the service.
	 */
	public void cancel() {
		isCancelled = true;
	}

	/**
	 * Returns whether this task has been cancelled. It may still be running, but will stop soon.
	 */
	public boolean isCancelled() {
		return isCancelled;
	}

	/***
	 * The resource ID of the context text to display in syncing progress and error notifications. It should describe
	 * the entire task.
	 */
	protected int getNotificationSummaryMessageId() {
		return NO_NOTIFICATION;
	}

	protected void updateProgressNotification() {
		updateProgressNotification(null);
	}

	protected void updateProgressNotificationAsPlural(@PluralsRes int detailResId, int quantity, Object... formatArgs) {
		updateProgressNotification(context.getResources().getQuantityString(detailResId, quantity, formatArgs));
	}

	/**
	 * If the user's preferences are set, show a notification with the current progress of the sync status. The content
	 * text is set by the sync task, while the detail message is displayed in BigTextStyle.
	 */
	protected void updateProgressNotification(String detail) {
		Timber.i(detail);
		if (!PreferencesUtils.getSyncShowNotifications(this.context)) return;

		String message = getNotificationSummaryMessageId() == NO_NOTIFICATION ?
			"" :
			context.getString(getNotificationSummaryMessageId());

		final Intent intent = new Intent(context, CancelReceiver.class);
		intent.setAction(SyncService.ACTION_CANCEL_SYNC);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
		NotificationCompat.Builder builder = NotificationUtils
			.createNotificationBuilder(context, R.string.sync_notification_title, NotificationUtils.CHANNEL_ID_SYNC_PROGRESS)
			.setContentText(message)
			.setPriority(NotificationCompat.PRIORITY_LOW)
			.setCategory(NotificationCompat.CATEGORY_SERVICE)
			.setOngoing(true)
			.setProgress(1, 0, true)
			.addAction(R.drawable.ic_stat_cancel, context.getString(R.string.cancel), pi);
		if (!TextUtils.isEmpty(detail)) {
			builder.setStyle(new BigTextStyle().bigText(detail));
		}
		NotificationUtils.notify(context, NotificationUtils.TAG_SYNC_PROGRESS, 0, builder);
	}

	/**
	 * If the user's preferences are set, show a notification message with the error message. This will replace any
	 * existing error notification.
	 */
	protected void showError(String detailMessage, @NonNull Throwable t) {
		showError(detailMessage, t.getLocalizedMessage());
	}

	/**
	 * If the user's preferences are set, show a notification message with the error message. This will replace any
	 * existing error notification.
	 */
	protected void showError(String detailMessage, int httpCode) {
		showError(detailMessage, PresentationUtils.getHttpErrorMessage(context, httpCode));
	}

	/**
	 * If the user's preferences are set, show a notification message with the error message. This will replace any
	 * existing error notification.
	 */
	protected void showError(String detailMessage, String errorMessage) {
		Timber.w(detailMessage + "\n" + errorMessage);

		if (!PreferencesUtils.getSyncShowErrors(context)) return;

		String contentMessage = getNotificationSummaryMessageId() == NO_NOTIFICATION ?
			detailMessage :
			context.getString(getNotificationSummaryMessageId());
		String bigText = getNotificationSummaryMessageId() == NO_NOTIFICATION ?
			errorMessage :
			detailMessage + "\n" + errorMessage;

		NotificationCompat.Builder builder = NotificationUtils
			.createNotificationBuilder(context, R.string.sync_notification_title_error, NotificationUtils.CHANNEL_ID_ERROR)
			.setContentText(contentMessage)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setCategory(NotificationCompat.CATEGORY_ERROR);
		if (!TextUtils.isEmpty(bigText)) {
			builder.setStyle(new BigTextStyle().bigText(bigText));
		}

		NotificationUtils.notify(context, NotificationUtils.TAG_SYNC_ERROR, 0, builder);
	}

	/**
	 * Sleep for the specified number of milliseconds. Returns true if thread was interrupted. This typically means the
	 * task should stop processing.
	 */
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
