package com.boardgamegeek.util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

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

	public static NotificationCompat.Builder createNotificationBuilder(Context context, String title) {
		return createNotificationBuilder(context, title, HomeActivity.class);
	}

	public static NotificationCompat.Builder createNotificationBuilder(Context context, int titleId) {
		return createNotificationBuilder(context, titleId, HomeActivity.class);
	}

	public static NotificationCompat.Builder createNotificationBuilder(Context context, int titleId, Class<?> cls) {
		return createNotificationBuilder(context, context.getString(titleId), cls);
	}

	/**
	 * Creates a notification builder with the correct icons, specified title, and pending intent.
	 */
	public static NotificationCompat.Builder createNotificationBuilder(Context context, String title, Class<?> cls) {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
			.setSmallIcon(R.drawable.ic_stat_bgg).setColor(context.getResources().getColor(R.color.primary_dark))
			.setContentTitle(title).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
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
		String title = String.format(context.getString(R.string.notification_playing_game), play.gameName);
		NotificationCompat.Builder builder = NotificationUtils.createNotificationBuilder(context, title);

		Intent intent = ActivityUtils.createPlayIntent(context, play.playId, play.gameId, play.gameName, thumbnailUrl,
			imageUrl);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

		String info = "";
		if (!TextUtils.isEmpty(play.location)) {
			info += context.getString(R.string.at) + " " + play.location + " ";
		}
		if (play.getPlayerCount() > 0) {
			info += context.getResources().getQuantityString(R.plurals.player_description, play.getPlayerCount(), play.getPlayerCount());
		}

		builder
			.setContentText(info.trim())
			.setOnlyAlertOnce(true)
			.setContentIntent(pendingIntent);
		if (play.startTime > 0) {
			builder.setWhen(play.startTime).setUsesChronometer(true);
		}
		NotificationUtils.notify(context, NotificationUtils.ID_PLAY_TIMER, builder);
		// TODO - set large icon with game thumbnail
	}
}
