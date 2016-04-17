package com.boardgamegeek.service;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Action;
import android.support.v4.app.NotificationCompat.Builder;

import com.boardgamegeek.io.BggService;
import com.boardgamegeek.io.BoardGameGeekService;
import com.boardgamegeek.util.NotificationUtils;

import java.util.ArrayList;
import java.util.List;

import hugo.weaving.DebugLog;
import timber.log.Timber;

public abstract class SyncUploadTask extends SyncTask {
	private final List<CharSequence> notificationMessages = new ArrayList<>();

	@DebugLog
	public SyncUploadTask(Context context, BggService bggService, BoardGameGeekService service) {
		super(context, bggService, service);
	}

	@StringRes
	protected abstract int getNotificationTitleResId();

	protected abstract Class<?> getNotificationIntentClass();

	protected abstract int getNotificationErrorId();

	protected abstract int getNotificationMessageId();

	@StringRes
	protected abstract int getUploadSummaryWithSize();

	@DebugLog
	protected void notifyUser(CharSequence message) {
		notificationMessages.add(message);
		NotificationCompat.Builder builder = createNotificationBuilder().setCategory(NotificationCompat.CATEGORY_SERVICE);
		if (notificationMessages.size() == 1) {
			addInitialMessage(builder);
		} else {
			addSubsequentMessage(builder);
		}
		NotificationUtils.notify(context, getNotificationMessageId(), builder);
	}

	@DebugLog
	private void addInitialMessage(@NonNull Builder builder) {
		CharSequence message = notificationMessages.get(0);
		builder.setContentText(message);
		NotificationCompat.BigTextStyle detail = new NotificationCompat.BigTextStyle(builder);
		detail.bigText(message);
		Action action = createMessageAction();
		if (action != null) {
			builder.addAction(action);
		}
	}

	@DebugLog
	private void addSubsequentMessage(@NonNull Builder builder) {
		String summary = String.format(context.getString(getUploadSummaryWithSize()), notificationMessages.size());
		builder.setContentText(summary);
		NotificationCompat.InboxStyle detail = new NotificationCompat.InboxStyle(builder);
		detail.setSummaryText(summary);
		for (int i = notificationMessages.size() - 1; i >= 0; i--) {
			detail.addLine(notificationMessages.get(i));
		}
	}

	@Nullable
	@DebugLog
	protected Action createMessageAction() {
		return null;
	}

	@DebugLog
	protected void notifyUploadError(String errorMessage) {
		Timber.e(errorMessage);
		NotificationCompat.Builder builder = createNotificationBuilder()
			.setContentText(errorMessage)
			.setCategory(NotificationCompat.CATEGORY_ERROR);
		NotificationCompat.BigTextStyle detail = new NotificationCompat.BigTextStyle(builder);
		detail.bigText(errorMessage);
		NotificationUtils.notify(context, getNotificationErrorId(), builder);
	}

	@DebugLog
	protected NotificationCompat.Builder createNotificationBuilder() {
		return NotificationUtils.createNotificationBuilder(context, getNotificationTitleResId(), getNotificationIntentClass());
	}
}
