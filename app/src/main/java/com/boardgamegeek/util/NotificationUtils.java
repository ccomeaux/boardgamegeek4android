package com.boardgamegeek.util;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build.VERSION_CODES;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Play;
import com.boardgamegeek.ui.HomeActivity;
import com.boardgamegeek.ui.PlayActivity;
import com.boardgamegeek.util.LargeIconLoader.Callback;

public class NotificationUtils {
	private static final String TAG_PREFIX = "com.boardgamegeek.";
	public static final String TAG_PLAY_STATS = TAG_PREFIX + "PLAY_STATS";
	public static final String TAG_PERSIST_ERROR = TAG_PREFIX + "PERSIST_ERROR";
	public static final String TAG_PLAY_TIMER = TAG_PREFIX + "PLAY_TIMER";
	public static final String TAG_PROVIDER_ERROR = TAG_PREFIX + "PROVIDER_ERROR";
	public static final String TAG_SYNC_PROGRESS = TAG_PREFIX + "SYNC_PROGRESS";
	public static final String TAG_SYNC_ERROR = TAG_PREFIX + "SYNC_ERROR";
	public static final String TAG_UPLOAD_PLAY = TAG_PREFIX + "UPLOAD_PLAY";
	public static final String TAG_UPLOAD_PLAY_ERROR = TAG_PREFIX + "UPLOAD_PLAY_ERROR";
	public static final String TAG_UPLOAD_COLLECTION = TAG_PREFIX + "UPLOAD_COLLECTION";
	public static final String TAG_UPLOAD_COLLECTION_ERROR = TAG_PREFIX + "UPLOAD_COLLECTION_ERROR";
	public static final String TAG_FIREBASE_MESSAGE = TAG_PREFIX + "FIREBASE_MESSAGE";

	public static final String CHANNEL_ID_SYNC_PROGRESS = "sync";
	public static final String CHANNEL_ID_ERROR = "sync_error";
	public static final String CHANNEL_ID_SYNC_UPLOAD = "sync_upload";
	public static final String CHANNEL_ID_PLAYING = "playing";
	public static final String CHANNEL_ID_STATS = "stats";
	public static final String CHANNEL_ID_FIREBASE_MESSAGES = "firebase_messages";

	@TargetApi(VERSION_CODES.O)
	public static void createNotificationChannels(Context context) {
		if (context == null) return;
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		if (notificationManager == null) return;

		NotificationChannel channel = new NotificationChannel(
			NotificationUtils.CHANNEL_ID_SYNC_PROGRESS,
			context.getString(R.string.channel_name_sync_progress),
			NotificationManager.IMPORTANCE_LOW);
		channel.setDescription(context.getString(R.string.channel_description_sync_progress));
		notificationManager.createNotificationChannel(channel);

		channel = new NotificationChannel(
			NotificationUtils.CHANNEL_ID_SYNC_UPLOAD,
			context.getString(R.string.channel_name_sync_upload),
			NotificationManager.IMPORTANCE_DEFAULT);
		channel.setDescription(context.getString(R.string.channel_description_sync_upload));
		notificationManager.createNotificationChannel(channel);

		channel = new NotificationChannel(
			NotificationUtils.CHANNEL_ID_ERROR,
			context.getString(R.string.channel_name_sync_error),
			NotificationManager.IMPORTANCE_DEFAULT);
		channel.setDescription(context.getString(R.string.channel_description_sync_error));
		channel.setLightColor(Color.RED);
		notificationManager.createNotificationChannel(channel);

		channel = new NotificationChannel(
			NotificationUtils.CHANNEL_ID_PLAYING,
			context.getString(R.string.channel_name_playing),
			NotificationManager.IMPORTANCE_DEFAULT);
		channel.setDescription(context.getString(R.string.channel_description_playing));
		channel.setLightColor(Color.BLUE);
		notificationManager.createNotificationChannel(channel);

		channel = new NotificationChannel(
			NotificationUtils.CHANNEL_ID_STATS,
			context.getString(R.string.channel_name_stats),
			NotificationManager.IMPORTANCE_DEFAULT);
		channel.setDescription(context.getString(R.string.channel_description_stats));
		notificationManager.createNotificationChannel(channel);

		channel = new NotificationChannel(
			NotificationUtils.CHANNEL_ID_FIREBASE_MESSAGES,
			context.getString(R.string.channel_name_firebase_messages),
			NotificationManager.IMPORTANCE_HIGH);
		channel.setDescription(context.getString(R.string.channel_description_firebase_messages));
		notificationManager.createNotificationChannel(channel);
	}

	/**
	 * Creates a {@link android.support.v4.app.NotificationCompat.Builder} with the correct icons, specified title, and
	 * pending intent that goes to the {@link com.boardgamegeek.ui.HomeActivity}.
	 */
	public static NotificationCompat.Builder createNotificationBuilder(Context context, String title, String channelId) {
		return createNotificationBuilder(context, title, channelId, HomeActivity.class);
	}

	/**
	 * Creates a {@link android.support.v4.app.NotificationCompat.Builder} with the correct icons, specified title, and
	 * pending intent that goes to the {@link com.boardgamegeek.ui.HomeActivity}.
	 */
	public static NotificationCompat.Builder createNotificationBuilder(Context context, @StringRes int titleResId, String channelId) {
		return createNotificationBuilder(context, titleResId, channelId, HomeActivity.class);
	}

	/**
	 * Creates a {@link android.support.v4.app.NotificationCompat.Builder} with the correct icons, specified title, and
	 * pending intent.
	 */
	public static NotificationCompat.Builder createNotificationBuilder(Context context, @StringRes int titleResId, String channelId, Class<?> cls) {
		return createNotificationBuilder(context, context.getString(titleResId), channelId, cls);
	}

	/**
	 * Creates a {@link android.support.v4.app.NotificationCompat.Builder} with the correct icons, specified title, and
	 * pending intent.
	 */
	public static NotificationCompat.Builder createNotificationBuilder(Context context, String title, String channelId, Class<?> cls) {
		return createNotificationBuilder(context, title, channelId, new Intent(context, cls));
	}

	/**
	 * Creates a {@link android.support.v4.app.NotificationCompat.Builder} with the correct icons, specified title, and
	 * pending intent.
	 */
	public static NotificationCompat.Builder createNotificationBuilder(Context context, @StringRes int titleResId, String channelId, Intent intent) {
		return createNotificationBuilder(context, context.getString(titleResId), channelId, intent);
	}

	/**
	 * Creates a {@link android.support.v4.app.NotificationCompat.Builder} with the correct icons, specified title, and
	 * pending intent.
	 */
	public static NotificationCompat.Builder createNotificationBuilder(Context context, String title, String channelId, Intent intent) {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
			.setSmallIcon(R.drawable.ic_stat_bgg)
			.setColor(ContextCompat.getColor(context, R.color.primary))
			.setContentTitle(title)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
		PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(resultPendingIntent);
		return builder;
	}

	/**
	 * Display the notification with a unique ID.
	 */
	public static void notify(Context context, String tag, int id, NotificationCompat.Builder builder) {
		NotificationManagerCompat nm = NotificationManagerCompat.from(context);
		nm.notify(tag, id, builder.build());
	}

	public static void showPersistErrorNotification(Context context, Exception e) {
		NotificationCompat.Builder builder = NotificationUtils
			.createNotificationBuilder(context, R.string.sync_notification_title, NotificationUtils.CHANNEL_ID_ERROR)
			.setContentText(e.getMessage())
			.setCategory(NotificationCompat.CATEGORY_ERROR)
			.setStyle(new NotificationCompat.BigTextStyle().bigText(e.toString()).setSummaryText(e.getMessage()));
		NotificationUtils.notify(context, NotificationUtils.TAG_PERSIST_ERROR, 0, builder);
	}

	/**
	 * Cancel the notification by a unique ID.
	 */
	public static void cancel(Context context, String tag) {
		cancel(context, tag, 0L);
	}

	/**
	 * Cancel the notification by a unique ID.
	 */
	public static void cancel(Context context, String tag, long id) {
		NotificationManagerCompat nm = NotificationManagerCompat.from(context);
		nm.cancel(tag, getIntegerId(id));
	}

	/**
	 * Launch the "Playing" notification.
	 */
	public static void launchPlayingNotification(final Context context, final long internalId, final Play play, final String thumbnailUrl, final String imageUrl, final String heroImageUrl) {
		LargeIconLoader loader = new LargeIconLoader(context, imageUrl, thumbnailUrl, heroImageUrl, new Callback() {
			@Override
			public void onSuccessfulIconLoad(Bitmap bitmap) {
				buildAndNotifyPlaying(context, internalId, play, thumbnailUrl, imageUrl, heroImageUrl, bitmap);
			}

			@Override
			public void onFailedIconLoad() {
				buildAndNotifyPlaying(context, internalId, play, thumbnailUrl, imageUrl, heroImageUrl, null);
			}
		});
		loader.executeOnMainThread();
	}

	private static void buildAndNotifyPlaying(Context context, long internalId, Play play, String thumbnailUrl, String imageUrl, String heroImageUrl, Bitmap largeIcon) {
		NotificationCompat.Builder builder = NotificationUtils.createNotificationBuilder(context, play.gameName, NotificationUtils.CHANNEL_ID_PLAYING);

		Intent intent = PlayActivity.createIntent(context, internalId, play.gameId, play.gameName, thumbnailUrl, imageUrl, heroImageUrl);
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
		NotificationUtils.notify(context, NotificationUtils.TAG_PLAY_TIMER, getIntegerId(internalId), builder);
	}

	public static int getIntegerId(long id) {
		if (id < Integer.MAX_VALUE) {
			return (int) id;
		} else {
			return (int) (id % Integer.MAX_VALUE);
		}
	}
}
