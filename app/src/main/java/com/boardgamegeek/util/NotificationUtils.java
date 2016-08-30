package com.boardgamegeek.util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.ui.HomeActivity;
import com.boardgamegeek.util.LargeIconLoader.Callback;

public class NotificationUtils {
	public static final String TAG_H_INDEX = "H-INDEX";
	public static final String TAG_PERSIST_ERROR = "PERSIST_ERROR";
	public static final String TAG_PLAY_TIMER = "PLAY_TIMER";
	public static final String TAG_PROVIDER_ERROR = "PROVIDER_ERROR";
	public static final String TAG_SYNC_PROGRESS = "SYNC_PROGRESS";
	public static final String TAG_UPDATE_ERROR = "UPDATE_ERROR";
	public static final String TAG_UPLOAD_PLAY = "UPLOAD_PLAY";
	public static final String TAG_UPLOAD_PLAY_ERROR = "UPLOAD_PLAY_ERROR";
	public static final String TAG_UPLOAD_COLLECTION = "UPLOAD_COLLECTION";
	public static final String TAG_UPLOAD_COLLECTION_ERROR = "UPLOAD_COLLECTION_ERROR";

	/**
	 * Creates a {@link android.support.v4.app.NotificationCompat.Builder} with the correct icons, specified title, and
	 * pending intent that goes to the {@link com.boardgamegeek.ui.HomeActivity}.
	 */
	public static NotificationCompat.Builder createNotificationBuilder(Context context, String title) {
		return createNotificationBuilder(context, title, HomeActivity.class);
	}

	/**
	 * Creates a {@link android.support.v4.app.NotificationCompat.Builder} with the correct icons, specified title, and
	 * pending intent that goes to the {@link com.boardgamegeek.ui.HomeActivity}.
	 */
	public static NotificationCompat.Builder createNotificationBuilder(Context context, int titleId) {
		return createNotificationBuilder(context, titleId, HomeActivity.class);
	}

	/**
	 * Creates a {@link android.support.v4.app.NotificationCompat.Builder} with the correct icons, specified title, and
	 * pending intent.
	 */
	public static NotificationCompat.Builder createNotificationBuilder(Context context, int titleId, Class<?> cls) {
		return createNotificationBuilder(context, context.getString(titleId), cls);
	}

	/**
	 * Creates a {@link android.support.v4.app.NotificationCompat.Builder} with the correct icons, specified title, and
	 * pending intent.
	 */
	public static NotificationCompat.Builder createNotificationBuilder(Context context, String title, Class<?> cls) {
		@SuppressWarnings("deprecation")
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
			.setSmallIcon(R.drawable.ic_stat_bgg)
			.setColor(context.getResources().getColor(R.color.primary))
			.setContentTitle(title)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
		Intent intent = new Intent(context, cls);
		PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(resultPendingIntent);
		return builder;
	}

	/**
	 * Display the notification with a unique ID.
	 */
	public static void notify(Context context, String tag, int id, NotificationCompat.Builder builder) {
		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(tag, id, builder.build());
	}

	/**
	 * Cancel the notification by a unique ID.
	 */
	public static void cancel(Context context, String tag, int id) {
		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(tag, id);
	}

	/**
	 * Launch the "Playing" notification.
	 */
	public static void launchPlayingNotification(final Context context, final Play play, final String thumbnailUrl, final String imageUrl) {
		buildAndNotify(context, play, thumbnailUrl, imageUrl, null);
		LargeIconLoader loader = new LargeIconLoader(context, imageUrl, thumbnailUrl, new Callback() {
			@Override
			public void onSuccessfulImageLoad(Bitmap bitmap) {
				buildAndNotify(context, play, thumbnailUrl, imageUrl, bitmap);
			}

			@Override
			public void onFailedImageLoad() {
				// oh well!
			}
		});
		loader.execute();
	}

	private static void buildAndNotify(Context context, Play play, String thumbnailUrl, String imageUrl, Bitmap largeIcon) {
		NotificationCompat.Builder builder = NotificationUtils.createNotificationBuilder(context, play.gameName);

		Intent intent = ActivityUtils.createPlayIntent(context, play.playId, play.gameId, play.gameName, thumbnailUrl, imageUrl);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, play.playId, intent, PendingIntent.FLAG_CANCEL_CURRENT);

		String info = "";
		if (!TextUtils.isEmpty(play.location)) {
			info += context.getString(R.string.at) + " " + play.location + " ";
		}
		if (play.getPlayerCount() > 0) {
			info += context.getResources().getQuantityString(R.plurals.player_description, play.getPlayerCount(), play.getPlayerCount());
		}

		builder
			.setContentText(info.trim())
			.setLargeIcon(largeIcon)
			.setOnlyAlertOnce(true)
			.setContentIntent(pendingIntent);
		if (play.startTime > 0) {
			builder.setWhen(play.startTime).setUsesChronometer(true);
		}
		if (largeIcon != null) {
			builder.extend(new NotificationCompat.WearableExtender().setBackground(largeIcon));
		}

		NotificationUtils.notify(context, NotificationUtils.TAG_PLAY_TIMER, play.playId, builder);
	}
}
