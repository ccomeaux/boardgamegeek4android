package com.boardgamegeek.service;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Action;
import android.support.v4.app.NotificationCompat.Builder;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.util.LargeIconLoader;
import com.boardgamegeek.util.LargeIconLoader.Callback;
import com.boardgamegeek.util.NotificationUtils;
import com.boardgamegeek.util.PresentationUtils;

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

	protected abstract Intent getNotificationIntent();

	protected abstract String getNotificationMessageTag();

	protected abstract String getNotificationErrorTag();

	@DebugLog
	protected void notifyUser(final CharSequence title, final CharSequence message, final int id, String imageUrl, final String thumbnailUrl) {
		LargeIconLoader loader = new LargeIconLoader(context, imageUrl, thumbnailUrl, new Callback() {
			@Override
			public void onSuccessfulIconLoad(Bitmap bitmap) {
				buildAndNotify(title, message, id, bitmap);
			}

			@Override
			public void onFailedIconLoad() {
				buildAndNotify(title, message, id, null);
			}
		});
		loader.executeInBackground();

		notificationMessages.add(PresentationUtils.getText(context, R.string.msg_play_upload, title, message));
		showNotificationSummary();
	}

	private void buildAndNotify(CharSequence title, CharSequence message, int id, Bitmap largeIcon) {
		Builder builder = createNotificationBuilder()
			.setCategory(NotificationCompat.CATEGORY_SERVICE)
			.setContentTitle(title)
			.setContentText(message)
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
	private void showNotificationSummary() {
		Builder builder = createNotificationBuilder()
			.setGroup(getNotificationMessageTag())
			.setGroupSummary(true);
		final int messageCount = notificationMessages.size();
		if (messageCount == 1) {
			builder.setContentText(notificationMessages.get(0));
		} else {
			NotificationCompat.InboxStyle detail = new NotificationCompat.InboxStyle(builder);
			for (int i = messageCount - 1; i >= 0; i--) {
				detail.addLine(notificationMessages.get(i));
			}
		}
		NotificationUtils.notify(context, getNotificationMessageTag(), 0, builder);
	}

	@Nullable
	@DebugLog
	protected Action createMessageAction() {
		return null;
	}

	@DebugLog
	protected void notifyUploadError(CharSequence errorMessage) {
		if (TextUtils.isEmpty(errorMessage)) return;
		Timber.e(errorMessage.toString());
		NotificationCompat.Builder builder = createNotificationBuilder()
			.setContentText(errorMessage)
			.setCategory(NotificationCompat.CATEGORY_ERROR);
		NotificationCompat.BigTextStyle detail = new NotificationCompat.BigTextStyle(builder);
		detail.bigText(errorMessage);
		NotificationUtils.notify(context, getNotificationErrorTag(), 0, builder);
	}

	@DebugLog
	private NotificationCompat.Builder createNotificationBuilder() {
		return NotificationUtils.createNotificationBuilder(context,
			context.getString(getNotificationTitleResId()),
			getNotificationIntent());
	}
}
