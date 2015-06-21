package com.boardgamegeek.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.pref.MultiSelectListPreference;
import com.boardgamegeek.provider.BggContract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility for getting and putting preferences.
 */
public class PreferencesUtils {
	public static final long VIEW_ID_COLLECTION = -1;
	private static final String VIEW_DEFAULT_ID = "viewDefaultId";
	private static final String KEY_LAST_PLAY_TIME = "last_play_time";
	private static final String KEY_LAST_PLAY_LOCATION = "last_play_location";
	private static final String KEY_LAST_PLAY_PLAYERS = "last_play_players";
	private static final String SEPARATOR_RECORD = "OV=I=XrecordX=I=VO";
	private static final String SEPARATOR_FIELD = "OV=I=XfieldX=I=VO";
	public static final String KEY_SYNC_STATUSES = "syncStatuses";

	private PreferencesUtils() {
	}

	public static boolean getExactSearch(Context context) {
		return getBoolean(context, "exactSearch", true);
	}

	public static boolean getSkipResults(Context context) {
		return getBoolean(context, "skipResults", true);
	}

	public static boolean showLogPlay(Context context) {
		return getBoolean(context, "logPlay", !getBoolean(context, "logHideLog", false));
	}

	public static boolean showQuickLogPlay(Context context) {
		return getBoolean(context, "quickLogPlay", !getBoolean(context, "logHideQuickLog", false));
	}

	public static boolean editPlayer(Context context) {
		return getBoolean(context, "logEditPlayer", false);
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

	public static boolean showLogPlayerWin(Context context) {
		return getBoolean(context, "logPlayerWin", !getBoolean(context, "logHideWin", true));
	}

	public static String[] getSyncStatuses(Context context) {
		return getStringArray(context, KEY_SYNC_STATUSES,
			context.getResources().getStringArray(R.array.pref_sync_status_default));
	}

	public static boolean addSyncStatus(Context context, String status) {
		if (TextUtils.isEmpty(status)) {
			return false;
		}
		if (isSyncStatus(context, status)) {
			return false;
		}

		String[] statuses = getStringArray(context, KEY_SYNC_STATUSES, null);
		Set<String> set = new HashSet<>();
		final int stringCount = statuses.length;
		set.addAll(Arrays.asList(statuses).subList(0, stringCount));

		set.add(status);

		String s = MultiSelectListPreference.buildString(set);

		return putString(context, KEY_SYNC_STATUSES, s);
	}

	public static boolean isSyncStatus(Context context) {
		String[] statuses = getSyncStatuses(context);
		return statuses != null && statuses.length > 0;
	}

	/**
	 * Determines if the specified status is set to be synced.
	 */
	public static boolean isSyncStatus(Context context, String status) {
		if (TextUtils.isEmpty(status)) {
			return false;
		}
		String[] statuses = getSyncStatuses(context);
		if (statuses == null) {
			return false;
		}
		for (String s : statuses) {
			if (s.equals(status)) {
				return true;
			}
		}
		return false;
	}

	public static boolean getSyncPlays(Context context) {
		return getBoolean(context, "syncPlays", false);
	}

	public static boolean isSyncPlays(String key) {
		return "syncPlays".equals(key);
	}

	public static boolean getSyncBuddies(Context context) {
		return getBoolean(context, "syncBuddies", false);
	}

	public static boolean getSyncShowNotifications(Context context) {
		return getBoolean(context, "sync_notifications", false);
	}

	public static boolean getSyncOnlyCharging(Context context) {
		return getBoolean(context, "sync_only_charging", false);
	}

	public static boolean getSyncOnlyWifi(Context context) {
		return getBoolean(context, "sync_only_wifi", false);
	}

	public static boolean getForumDates(Context context) {
		return getBoolean(context, "advancedForumDates", false);
	}

	public static boolean getAvoidBatching(Context context) {
		return getBoolean(context, "advancedDebugInsert", false);
	}

	public static int getNewPlayId(Context context, int oldPlayId) {
		return getInt(context, "playId" + oldPlayId, BggContract.INVALID_ID);
	}

	public static void putNewPlayId(Context context, int oldPlayId, int newPlayId) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = sharedPreferences.edit();
		if (newPlayId == BggContract.INVALID_ID) {
			editor.remove("playId" + oldPlayId);
		} else {
			editor.putInt("playId" + oldPlayId, newPlayId);
		}
		editor.apply();
	}

	public static void removeNewPlayId(Context context, int oldPlayId) {
		putNewPlayId(context, oldPlayId, BggContract.INVALID_ID);
	}

	public static int getHIndex(Context context) {
		return getInt(context, "hIndex", 0);
	}

	public static boolean putHIndex(Context context, int hIndex) {
		return putInt(context, "hIndex", hIndex);
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

	private static boolean remove(Context context, String key) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = sharedPreferences.edit();
		editor.remove(key);
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
		if (value == null) {
			return defValue;
		}
		return MultiSelectListPreference.parseStoredValue(value);
	}
}
