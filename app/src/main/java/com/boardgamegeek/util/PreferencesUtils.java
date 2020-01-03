package com.boardgamegeek.util;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.entities.HIndexEntity;
import com.boardgamegeek.extensions.ContextUtils;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.ui.PlayStatsActivity;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

/**
 * Utility for getting and putting preferences.
 */
public class PreferencesUtils {
	public static final long VIEW_ID_COLLECTION = -1;
	public static final int INVALID_ARTICLE_ID = -1;

	public static final String LOG_PLAY_STATS_PREFIX = "logPlayStats";
	private static final String VIEW_DEFAULT_ID = "viewDefaultId";
	private static final String KEY_LAST_PLAY_TIME = "last_play_time";
	private static final String KEY_LAST_PLAY_LOCATION = "last_play_location";
	private static final String KEY_LAST_PLAY_PLAYERS = "last_play_players";
	public static final String KEY_GAME_H_INDEX = "hIndex";
	private static final String KEY_PLAYER_H_INDEX = "play_stats_player_h_index";
	public static final String KEY_H_INDEX_N_SUFFIX = "_n";
	private static final String KEY_PRIVACY_CHECK_TIMESTAMP = "privacy_check_timestamp";
	private static final String SEPARATOR_RECORD = "OV=I=XrecordX=I=VO";
	private static final String SEPARATOR_FIELD = "OV=I=XfieldX=I=VO";
	public static final String KEY_LOGIN = "login";
	public static final String KEY_LOGOUT = "logout";
	public static final String KEY_SYNC_STATUSES_OLD = "syncStatuses";
	private static final String KEY_HAS_SEEN_NAV_DRAWER = "has_seen_nav_drawer";
	private static final String KEY_HAPTIC_FEEDBACK = "haptic_feedback";
	private static final String LOG_PLAY_STATS_INCOMPLETE = LOG_PLAY_STATS_PREFIX + "Incomplete";
	private static final String LOG_PLAY_STATS_EXPANSIONS = LOG_PLAY_STATS_PREFIX + "Expansions";
	private static final String LOG_PLAY_STATS_ACCESSORIES = LOG_PLAY_STATS_PREFIX + "Accessories";
	private static final String LOG_EDIT_PLAYER_PROMPTED = "logEditPlayerPrompted";
	private static final String LOG_EDIT_PLAYER = "logEditPlayer";

	private static final String SEPARATOR = "OV=I=XseparatorX=I=VO";

	private static final int NOTIFICATION_ID_PLAY_STATS_GAME_H_INDEX = 0;
	private static final int NOTIFICATION_ID_PLAY_STATS_PLAYER_H_INDEX = 1;

	private PreferencesUtils() {
	}

	public static boolean showLogPlay(Context context) {
		return getBoolean(context, "logPlay", !getBoolean(context, "logHideLog", false));
	}

	public static boolean showQuickLogPlay(Context context) {
		return getBoolean(context, "quickLogPlay", !getBoolean(context, "logHideQuickLog", false));
	}

	public static boolean getEditPlayerPrompted(Context context) {
		return getBoolean(context, LOG_EDIT_PLAYER_PROMPTED, false);
	}

	public static boolean putEditPlayerPrompted(Context context) {
		return putBoolean(context, LOG_EDIT_PLAYER_PROMPTED, true);
	}

	public static boolean getEditPlayer(Context context) {
		return getBoolean(context, LOG_EDIT_PLAYER, false);
	}

	public static boolean putEditPlayer(Context context, boolean value) {
		return putBoolean(context, LOG_EDIT_PLAYER, value);
	}

	public static boolean showLogPlayQuantity(Context context) {
		return getBoolean(context, "logPlayQuantity", false);
	}

	public static boolean showLogPlayLength(Context context) {
		return getBoolean(context, "logPlayLength", !getBoolean(context, "logHideLength", true));
	}

	public static boolean showLogPlayLocation(Context context) {
		return getBoolean(context, "logPlayLocation", !getBoolean(context, "logHideLocation", true));
	}

	public static boolean showLogPlayIncomplete(Context context) {
		return getBoolean(context, "logPlayIncomplete", !getBoolean(context, "logHideIncomplete", true));
	}

	public static boolean showLogPlayNoWinStats(Context context) {
		return getBoolean(context, "logPlayNoWinStats", !getBoolean(context, "logHideNoWinStats", true));
	}

	public static boolean showLogPlayComments(Context context) {
		return getBoolean(context, "logPlayComments", !getBoolean(context, "logHideComments", true));
	}

	public static boolean showLogPlayPlayerList(Context context) {
		return getBoolean(context, "logPlayPlayerList", !getBoolean(context, "logHidePlayerList", false));
	}

	public static boolean showLogPlayerTeamColor(Context context) {
		return getBoolean(context, "logPlayerTeamColor", !getBoolean(context, "logHideTeamColor", true));
	}

	public static boolean showLogPlayerPosition(Context context) {
		return getBoolean(context, "logPlayerPosition", !getBoolean(context, "logHidePosition", true));
	}

	public static boolean showLogPlayerScore(Context context) {
		return getBoolean(context, "logPlayerScore", !getBoolean(context, "logHideScore", true));
	}

	public static boolean showLogPlayerRating(Context context) {
		return getBoolean(context, "logPlayerRating", !getBoolean(context, "logHideRating", true));
	}

	public static boolean showLogPlayerNew(Context context) {
		return getBoolean(context, "logPlayerNew", !getBoolean(context, "logHideNew", true));
	}

	public static boolean logPlayStatsIncomplete(Context context) {
		return getBoolean(context, LOG_PLAY_STATS_INCOMPLETE, false);
	}

	public static void putPlayStatsIncomplete(Context context, boolean value) {
		putBoolean(context, LOG_PLAY_STATS_INCOMPLETE, value);
	}

	public static boolean logPlayStatsExpansions(Context context) {
		return getBoolean(context, LOG_PLAY_STATS_EXPANSIONS, false);
	}

	public static void putPlayStatsExpansions(Context context, boolean value) {
		putBoolean(context, LOG_PLAY_STATS_EXPANSIONS, value);
	}

	public static boolean logPlayStatsAccessories(Context context) {
		return getBoolean(context, LOG_PLAY_STATS_ACCESSORIES, false);
	}

	public static void putPlayStatsAccessories(Context context, boolean value) {
		putBoolean(context, LOG_PLAY_STATS_ACCESSORIES, value);
	}

	public static boolean showLogPlayerWin(Context context) {
		return getBoolean(context, "logPlayerWin", !getBoolean(context, "logHideWin", true));
	}

	public static String[] getOldSyncStatuses(Context context) {
		return getStringArray(context, KEY_SYNC_STATUSES_OLD, context.getResources().getStringArray(R.array.pref_sync_status_default));
	}

	public static boolean getPlayUploadNotifications(Context context) {
		return getBoolean(context, "sync_uploads", true);
	}

	public static boolean getSyncShowNotifications(Context context) {
		return getBoolean(context, "sync_notifications", false);
	}

	public static boolean getSyncShowErrors(Context context) {
		return getBoolean(context, "sync_errors", false);
	}

	public static boolean getSyncOnlyCharging(Context context) {
		return getBoolean(context, "sync_only_charging", false);
	}

	public static boolean getSyncOnlyWifi(Context context) {
		return getBoolean(context, "sync_only_wifi", false);
	}

	public static Long getLastPrivacyCheckTimestamp(Context context) {
		return getLong(context, KEY_PRIVACY_CHECK_TIMESTAMP, 0L);
	}

	public static boolean setLastPrivacyCheckTimestamp(Context context) {
		return putLong(context, KEY_PRIVACY_CHECK_TIMESTAMP, System.currentTimeMillis());
	}

	public static boolean getForumDates(Context context) {
		return getBoolean(context, "advancedForumDates", false);
	}

	public static boolean getAvoidBatching(Context context) {
		return getBoolean(context, "advancedDebugInsert", false);
	}

	public static int getGameHIndex(Context context) {
		return getInt(context, KEY_GAME_H_INDEX, 0);
	}

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

	public static long getViewDefaultId(Context context) {
		return getLong(context, VIEW_DEFAULT_ID, VIEW_ID_COLLECTION);
	}

	public static boolean putViewDefaultId(Context context, long id) {
		return putLong(context, VIEW_DEFAULT_ID, id);
	}

	public static boolean removeViewDefaultId(Context context) {
		return remove(context, VIEW_DEFAULT_ID);
	}

	public static long getLastPlayTime(Context context) {
		return getLong(context, KEY_LAST_PLAY_TIME, 0);
	}

	public static boolean putLastPlayTime(Context context, long millis) {
		return putLong(context, KEY_LAST_PLAY_TIME, millis);
	}

	public static String getLastPlayLocation(Context context) {
		return getString(context, KEY_LAST_PLAY_LOCATION);
	}

	public static boolean putLastPlayLocation(Context context, String location) {
		return putString(context, KEY_LAST_PLAY_LOCATION, location);
	}

	public static List<Player> getLastPlayPlayers(Context context) {
		List<Player> players = new ArrayList<>();
		String playersString = getString(context, KEY_LAST_PLAY_PLAYERS);
		String[] playerStringArray = playersString.split(SEPARATOR_RECORD);
		for (String playerString : playerStringArray) {
			if (!TextUtils.isEmpty(playerString)) {
				String[] playerSplit = playerString.split(SEPARATOR_FIELD);
				if (playerSplit.length > 0 && playerSplit.length < 3) {
					Player player = new Player();
					player.name = playerSplit[0];
					if (playerSplit.length == 2) {
						player.username = playerSplit[1];
					}
					players.add(player);
				}
			}
		}
		return players;
	}

	public static boolean putLastPlayPlayers(Context context, List<Player> players) {
		StringBuilder sb = new StringBuilder();
		for (Player player : players) {
			sb.append(player.name).append(SEPARATOR_FIELD).append(player.username).append(SEPARATOR_RECORD);
		}
		return putString(context, KEY_LAST_PLAY_PLAYERS, sb.toString());
	}

	public static boolean hasSeenNavDrawer(Context context) {
		return getBoolean(context, KEY_HAS_SEEN_NAV_DRAWER, false);
	}

	public static void sawNavDrawer(Context context) {
		putBoolean(context, KEY_HAS_SEEN_NAV_DRAWER, true);
	}

	public static boolean getHapticFeedback(Context context) {
		return getBoolean(context, KEY_HAPTIC_FEEDBACK, true);
	}

	public static boolean putThreadArticle(Context context, int threadId, int articleId) {
		return putInt(context, getThreadKey(threadId), articleId);
	}

	public static int getThreadArticle(Context context, int threadId) {
		return getInt(context, getThreadKey(threadId), INVALID_ARTICLE_ID);
	}

	@NonNull
	private static String getThreadKey(long threadId) {
		return "THREAD-" + threadId;
	}

	private static boolean remove(Context context, String key) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = sharedPreferences.edit();
		editor.remove(key);
		return editor.commit();
	}

	private static boolean putBoolean(Context context, String key, boolean value) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = sharedPreferences.edit();
		editor.putBoolean(key, value);
		return editor.commit();
	}

	private static boolean putInt(Context context, String key, int value) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = sharedPreferences.edit();
		editor.putInt(key, value);
		return editor.commit();
	}

	private static boolean putLong(Context context, String key, long value) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = sharedPreferences.edit();
		editor.putLong(key, value);
		return editor.commit();
	}

	private static boolean putString(Context context, String key, String value) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = sharedPreferences.edit();
		editor.putString(key, value);
		return editor.commit();
	}

	private static boolean getBoolean(Context context, String key, boolean defaultValue) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getBoolean(key, defaultValue);
	}

	private static int getInt(Context context, String key, int defaultValue) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getInt(key, defaultValue);
	}

	private static long getLong(Context context, String key, long defaultValue) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getLong(key, defaultValue);
	}

	private static String getString(Context context, String key) {
		return getString(context, key, "");
	}

	private static String getString(Context context, String key, String defValue) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getString(key, defValue);
	}

	private static String[] getStringArray(Context context, String key, String[] defValue) {
		String value = getString(context, key, null);
		if (value == null) return defValue;
		return TextUtils.isEmpty(value) ? new String[0] : value.split(SEPARATOR);
	}
}
