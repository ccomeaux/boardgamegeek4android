package com.boardgamegeek.util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;

import com.boardgamegeek.R;
import com.boardgamegeek.ui.HomeActivity;

public class NotificationUtils {
	public static final int ID_SYNC = 0;
	public static final int ID_SYNC_ERROR = -1;
	public static final int ID_SYNC_PLAY_UPLOAD = 1;

	public static NotificationCompat.Builder createNotificationBuilder(Context context, int titleId) {
		return createNotificationBuilder(context, titleId, HomeActivity.class);
	}

	public static NotificationCompat.Builder createNotificationBuilder(Context context, int titleId, Class<?> cls) {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
			.setSmallIcon(R.drawable.ic_stat_bgg)
			.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.title_logo))
			.setContentTitle(context.getString(titleId));
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
}
