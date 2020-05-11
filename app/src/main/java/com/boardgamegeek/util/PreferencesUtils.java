package com.boardgamegeek.util;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.boardgamegeek.R;
import com.boardgamegeek.entities.HIndexEntity;
import com.boardgamegeek.extensions.ContextUtils;
import com.boardgamegeek.ui.PlayStatsActivity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

/**
 * Utility for getting and putting preferences.
 */
public class PreferencesUtils {


	private PreferencesUtils() {
	}


	public static final String KEY_GAME_H_INDEX = "hIndex";
	private static final String KEY_PLAYER_H_INDEX = "play_stats_player_h_index";
	public static final String KEY_H_INDEX_N_SUFFIX = "_n";

	public static int getGameHIndex(Context context) {
		return getInt(context, KEY_GAME_H_INDEX, 0);
	}

	private static final int NOTIFICATION_ID_PLAY_STATS_GAME_H_INDEX = 0;
	private static final int NOTIFICATION_ID_PLAY_STATS_PLAYER_H_INDEX = 1;

	public static void updateGameHIndex(@Nullable Context context, HIndexEntity gameHIndex) {
		if (context == null) return;
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = sharedPreferences.edit();
		updateHIndex(context, editor, gameHIndex, KEY_GAME_H_INDEX, R.string.game, NOTIFICATION_ID_PLAY_STATS_GAME_H_INDEX);
		editor.apply();
	}

	public static void updatePlayerHIndex(@Nullable Context context, HIndexEntity playerHIndex) {
		if (context == null) return;
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = sharedPreferences.edit();
		updateHIndex(context, editor, playerHIndex, KEY_PLAYER_H_INDEX, R.string.player, NOTIFICATION_ID_PLAY_STATS_PLAYER_H_INDEX);
		editor.apply();
	}

	private static void updateHIndex(@NonNull Context context, Editor editor, HIndexEntity hIndex, String key, @StringRes int typeResId, int notificationId) {
		if (hIndex.getH() != HIndexEntity.INVALID_H_INDEX) {
			int oldHIndex = getInt(context, key, 0);
			int oldN = getInt(context, key + KEY_H_INDEX_N_SUFFIX, 0);
			if (oldHIndex != hIndex.getH() || oldN != hIndex.getN()) {
				editor.putInt(key, hIndex.getH());
				editor.putInt(key + KEY_H_INDEX_N_SUFFIX, hIndex.getN());
				@StringRes int messageId = hIndex.getH() > oldHIndex || (hIndex.getH() == oldHIndex && hIndex.getN() < oldN) ?
					R.string.sync_notification_h_index_increase :
					R.string.sync_notification_h_index_decrease;
				notifyPlayStatChange(context, ContextUtils.getText(context, messageId, context.getString(typeResId), hIndex.getDescription()), notificationId);
			}
		}
	}

	private static void notifyPlayStatChange(@NonNull Context context, CharSequence message, int id) {
		Intent intent = new Intent(context, PlayStatsActivity.class);
		PendingIntent pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		NotificationCompat.Builder builder = NotificationUtils
			.createNotificationBuilder(context, R.string.title_play_stats, NotificationUtils.CHANNEL_ID_STATS, PlayStatsActivity.class)
			.setContentText(message)
			.setContentIntent(pi);
		NotificationUtils.notify(context, NotificationUtils.TAG_PLAY_STATS, id, builder);
	}

	private static int getInt(Context context, String key, int defaultValue) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getInt(key, defaultValue);
	}
}
