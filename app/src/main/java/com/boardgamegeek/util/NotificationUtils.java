package com.boardgamegeek.util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.ui.HomeActivity;

public class NotificationUtils {
	public static final int ID_SYNC = 0;
	public static final int ID_SYNC_PLAY_UPLOAD = 1;
	public static final int ID_PLAY_TIMER = 2;
	public static final int ID_H_INDEX = 3;
	public static final int ID_SYNC_ERROR = -1;
	public static final int ID_PROVIDER_ERROR = -2;
	public static final int ID_SYNC_PLAY_UPLOAD_ERROR = -3;
	public static final int ID_PERSIST_ERROR = -4;

	public static NotificationCompat.Builder createNotificationBuilder(Context context, int titleId) {
		return createNotificationBuilder(context, titleId, HomeActivity.class);
	}

	/**
	 * Creates a notification builder with the correct icons, specified title, and pending intent.
	 */
	public static NotificationCompat.Builder createNotificationBuilder(Context context, int titleId, Class<?> cls) {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
			.setSmallIcon(R.drawable.ic_stat_bgg).setColor(context.getResources().getColor(R.color.primary_dark))
			.setContentTitle(context.getString(titleId)).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
		Intent intent = new Intent(context, cls);
		PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, intent,
			PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(resultPendingIntent);
		return builder;
	}

	public static void notify(Context context, int id, NotificationCompat.Builder builder) {
		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(id, builder.build());
	}

	public static void cancel(Context context, int id) {
		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(id);
	}

	public static void launchStartNotification(Context context, Play play, String thumbnailUrl, String imageUrl) {
		launchStartNotification(context, play, thumbnailUrl, imageUrl, false);
	}

	public static void launchStartNotificationWithTicker(Context context, Play play, String thumbnailUrl,
		String imageUrl) {
		launchStartNotification(context, play, thumbnailUrl, imageUrl, true);
	}

	private static void launchStartNotification(Context context, Play play, String thumbnailUrl, String imageUrl,
		boolean includeTicker) {
		Intent intent = ActivityUtils.createPlayIntent(context, play.playId, play.gameId, play.gameName, thumbnailUrl,
			imageUrl);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		NotificationCompat.Builder builder = NotificationUtils.createNotificationBuilder(context,
			R.string.notification_playing);

		builder.setContentText(play.gameName).setOnlyAlertOnce(true)
			.setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT));
		if (includeTicker) {
			builder.setTicker(String.format(context.getString(R.string.notification_playing_game), play.gameName));
		}
		if (play.startTime > 0) {
			builder.setWhen(play.startTime).setUsesChronometer(true);
		}
		NotificationUtils.notify(context, NotificationUtils.ID_PLAY_TIMER, builder);
		// TODO - set large icon with game thumbnail
	}
}
