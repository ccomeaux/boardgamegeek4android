package com.boardgamegeek.util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.ui.HomeActivity;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.LinkedList;
import java.util.Queue;

public class NotificationUtils {
	public static final int ID_SYNC = 0;
	public static final int ID_SYNC_PLAY_UPLOAD = 1;
	public static final int ID_PLAY_TIMER = 2;
	public static final int ID_H_INDEX = 3;
	public static final int ID_SYNC_COLLECTION_UPLOAD = 4;
	public static final int ID_SYNC_ERROR = -1;
	public static final int ID_PROVIDER_ERROR = -2;
	public static final int ID_SYNC_PLAY_UPLOAD_ERROR = -3;
	public static final int ID_PERSIST_ERROR = -4;
	public static final int ID_SYNC_COLLECTION_UPLOAD_ERROR = -5;

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
		PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, intent,
			PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(resultPendingIntent);
		return builder;
	}

	/**
	 * Display the notification with a unique ID.
	 */
	public static void notify(Context context, int id, NotificationCompat.Builder builder) {
		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(id, builder.build());
	}

	/**
	 * Cancel the notification by a unique ID.
	 */
	public static void cancel(Context context, int id) {
		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(id);
	}

	/**
	 * Launch the "Playing" notification.
	 */
	public static void launchPlayingNotification(final Context context, final Play play, final String thumbnailUrl, final String imageUrl) {
		buildAndNotify(context, play, thumbnailUrl, imageUrl, null);
		Queue<String> imageUrls = new LinkedList<>();
		imageUrls.add(ImageUtils.appendImageUrl(imageUrl, ImageUtils.SUFFIX_MEDIUM));
		imageUrls.add(imageUrl);
		imageUrls.add(thumbnailUrl);
		imageUrls.add(ImageUtils.appendImageUrl(imageUrl, ImageUtils.SUFFIX_MEDIUM));
		tryLoadLargeIcon(context, play, thumbnailUrl, imageUrl, imageUrls);
	}

	private static void buildAndNotify(Context context, Play play, String thumbnailUrl, String imageUrl, Bitmap largeIcon) {
		NotificationCompat.Builder builder = NotificationUtils.createNotificationBuilder(context, play.gameName);

		Intent intent = ActivityUtils.createPlayIntent(context, play.playId, play.gameId, play.gameName, thumbnailUrl, imageUrl);
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
			.setLargeIcon(largeIcon)
			.setOnlyAlertOnce(true)
			.setContentIntent(pendingIntent);
		if (play.startTime > 0) {
			builder.setWhen(play.startTime).setUsesChronometer(true);
		}
		if (largeIcon != null) {
			builder.extend(new NotificationCompat.WearableExtender().setBackground(largeIcon));
		}

		NotificationUtils.notify(context, NotificationUtils.ID_PLAY_TIMER, builder);
	}

	/**
	 * Attempt to load the first icon in the queue. If this fails, try the next icon in the queue until successful or
	 * the queue is empty.
	 */
	private static void tryLoadLargeIcon(final Context context, final Play play, final String thumbnailUrl,
										 final String imageUrl, final Queue<String> imageUrls) {
		String path = imageUrls.poll();
		if (TextUtils.isEmpty(path)) {
			return;
		}
		Picasso.with(context.getApplicationContext())
			.load(HttpUtils.ensureScheme(path))
			.networkPolicy(NetworkPolicy.NO_STORE)
			.resize(400, 400) // recommended size for wearables
			.centerCrop()
			.into(new Target() {
				@Override
				public void onPrepareLoad(Drawable placeHolderDrawable) {
				}

				@Override
				public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
					buildAndNotify(context, play, thumbnailUrl, imageUrl, bitmap);
				}

				@Override
				public void onBitmapFailed(Drawable errorDrawable) {
					tryLoadLargeIcon(context, play, thumbnailUrl, imageUrl, imageUrls);
				}
			});
	}
}
