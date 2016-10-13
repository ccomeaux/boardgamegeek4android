package com.boardgamegeek.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.PluralsRes;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Action;
import android.support.v4.app.NotificationCompat.Builder;

import com.boardgamegeek.io.BggService;
import com.boardgamegeek.util.LargeIconLoader;
import com.boardgamegeek.util.LargeIconLoader.Callback;
import com.boardgamegeek.util.NotificationUtils;

import java.util.ArrayList;
import java.util.List;

import hugo.weaving.DebugLog;
import timber.log.Timber;

public abstract class SyncUploadTask extends SyncTask {
	private final List<CharSequence> notificationMessages = new ArrayList<>();

	@DebugLog
	public SyncUploadTask(Context context, BggService service) {
		super(context, service);
	}

	@StringRes
	protected abstract int getNotificationTitleResId();

	protected abstract Class<?> getNotificationIntentClass();

	protected abstract String getNotificationMessageTag();

	protected abstract String getNotificationErrorTag();

	@PluralsRes
	protected abstract int getUploadSummaryWithSize();

	@DebugLog
	protected void notifyUser(final CharSequence message, final int id, String imageUrl, final String thumbnailUrl) {
		buildAndNotify(message, id, null);
		LargeIconLoader loader = new LargeIconLoader(context, imageUrl, thumbnailUrl, new Callback() {
			@Override
			public void onSuccessfulIconLoad(Bitmap bitmap) {
				buildAndNotify(message, id, bitmap);
			}

			@Override
			public void onFailedIconLoad() {
				// oh well!
			}
		});
		loader.executeInBackground();

//		notificationMessages.add(message);
//		addSubsequentMessage(builder);
//		NotificationUtils.notify(context, getNotificationMessageTag(), id, builder);
	}

	private void buildAndNotify(CharSequence message, int id, Bitmap largeIcon) {
		Builder builder = createNotificationBuilder()
			.setCategory(NotificationCompat.CATEGORY_SERVICE);

		builder
			.setContentText(message.toString().trim())
			.setLargeIcon(largeIcon)
			.setOnlyAlertOnce(true)
			.setGroup(getNotificationMessageTag());
		NotificationCompat.BigTextStyle detail = new NotificationCompat.BigTextStyle(builder);
		detail.bigText(message);
		Action action = createMessageAction();
		if (action != null) {
			builder.addAction(action);
		}
		if (largeIcon != null) {
			builder.extend(new NotificationCompat.WearableExtender().setBackground(largeIcon));
		}
		NotificationUtils.notify(context, getNotificationMessageTag(), id, builder);
	}

	@DebugLog
	private void addSubsequentMessage(@NonNull Builder builder) {
		final int messageCount = notificationMessages.size();
		String summary = context.getResources().getQuantityString(getUploadSummaryWithSize(), messageCount, messageCount);
		builder
			.setContentText(summary)
			.setGroup(getNotificationMessageTag())
			.setGroupSummary(true);
		NotificationCompat.InboxStyle detail = new NotificationCompat.InboxStyle(builder);
		detail.setSummaryText(summary);
		for (int i = messageCount - 1; i >= 0; i--) {
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
		NotificationUtils.notify(context, getNotificationErrorTag(), 0, builder);
	}

	@DebugLog
	protected NotificationCompat.Builder createNotificationBuilder() {
		return NotificationUtils.createNotificationBuilder(context, getNotificationTitleResId(), getNotificationIntentClass());
	}
}
