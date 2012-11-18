package com.boardgamegeek.model;

import static com.boardgamegeek.util.LogUtils.LOGD;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.CursorUtils;

public class Play {
	/**
	 * Used for filtering to include all plays
	 */
	public static final int SYNC_STATUS_ALL = -2;
	/**
	 * The play has not been synced and isn't stored in the database
	 */
	public static final int SYNC_STATUS_NOT_STORED = -1;
	/**
	 * The play has been synced with the 'Geek
	 */
	public static final int SYNC_STATUS_SYNCED = 0;
	/**
	 * The play is ready to be synced, but doesn't exist on the 'Geek, or has local modifications?
	 */
	public static final int SYNC_STATUS_PENDING_UPDATE = 1;
	/**
	 * The play is currently being edited and will not sync until the user manually tries to sync it
	 */
	public static final int SYNC_STATUS_IN_PROGRESS = 2;
	/**
	 * The play is ready to be deleted
	 */
	public static final int SYNC_STATUS_PENDING_DELETE = 3;

	public static final int UNSYNCED_PLAY_ID = 100000000;

	private static final String TAG = makeLogTag(Play.class);
	private static final String KEY_PLAY_ID = "PLAY_ID";
	private static final String KEY_GAME_ID = "GAME_ID";
	private static final String KEY_GAME_NAME = "GAME_NAME";
	private static final String KEY_YEAR = "YEAR";
	private static final String KEY_MONTH = "MONTH";
	private static final String KEY_DATY = "DAY";
	private static final String KEY_QUANTITY = "QUANTITY";
	private static final String KEY_LENGTH = "LENGTH";
	private static final String KEY_LOCATION = "LOCATION";
	private static final String KEY_INCOMPLETE = "INCOMPLETE";
	private static final String KEY_NOWINSTATS = "NO_WIN_STATS";
	private static final String KEY_COMMENTS = "COMMENTS";
	private static final String KEY_PLAYERS = "PLAYERS";
	private static final String KEY_UPDATED = "UPDATED";
	private static final String KEY_SYNC_STATUS = "SYNC_STATUS";
	private static final String KEY_SAVED = "SAVED";

	private DateFormat df = DateFormat.getDateInstance(DateFormat.FULL);
	private List<Player> mPlayers = new ArrayList<Player>();

	public Play() {
		init(0, BggContract.INVALID_ID, "");
	}

	public Play(int playId, int gameId, String gameName) {
		init(playId, gameId, gameName);
	}

	private void init(int playId, int gameId, String gameName) {
		PlayId = playId;
		GameId = gameId;
		GameName = gameName;
		Quantity = 1;
		// set current date
		final Calendar c = Calendar.getInstance();
		Year = c.get(Calendar.YEAR);
		Month = c.get(Calendar.MONTH);
		Day = c.get(Calendar.DAY_OF_MONTH);
	}

	public Play(Bundle bundle) {
		PlayId = bundle.getInt(KEY_PLAY_ID);
		GameId = bundle.getInt(KEY_GAME_ID);
		GameName = bundle.getString(KEY_GAME_NAME);
		Year = bundle.getInt(KEY_YEAR);
		Month = bundle.getInt(KEY_MONTH);
		Day = bundle.getInt(KEY_DATY);
		Quantity = bundle.getInt(KEY_QUANTITY);
		Length = bundle.getInt(KEY_LENGTH);
		Location = bundle.getString(KEY_LOCATION);
		Incomplete = bundle.getBoolean(KEY_INCOMPLETE);
		NoWinStats = bundle.getBoolean(KEY_NOWINSTATS);
		Comments = bundle.getString(KEY_COMMENTS);
		Updated = bundle.getLong(KEY_UPDATED);
		SyncStatus = bundle.getInt(KEY_SYNC_STATUS);
		Saved = bundle.getLong(KEY_SAVED);
		mPlayers = bundle.getParcelableArrayList(KEY_PLAYERS);
	}

	public void saveState(Bundle bundle) {
		bundle.putInt(KEY_PLAY_ID, PlayId);
		bundle.putInt(KEY_GAME_ID, GameId);
		bundle.putString(KEY_GAME_NAME, GameName);
		bundle.putInt(KEY_YEAR, Year);
		bundle.putInt(KEY_MONTH, Month);
		bundle.putInt(KEY_DATY, Day);
		bundle.putInt(KEY_QUANTITY, Quantity);
		bundle.putInt(KEY_LENGTH, Length);
		bundle.putString(KEY_LOCATION, Location);
		bundle.putBoolean(KEY_INCOMPLETE, Incomplete);
		bundle.putBoolean(KEY_NOWINSTATS, NoWinStats);
		bundle.putString(KEY_COMMENTS, Comments);
		bundle.putLong(KEY_UPDATED, Updated);
		bundle.putInt(KEY_SYNC_STATUS, SyncStatus);
		bundle.putLong(KEY_SAVED, Saved);
		bundle.putParcelableArrayList(KEY_PLAYERS, (ArrayList<? extends Parcelable>) mPlayers);
	}

	public int PlayId;
	public int GameId;
	public String GameName;
	public int Year;
	public int Month;
	public int Day;
	public int Quantity;
	public int Length;
	public String Location;
	public boolean Incomplete;
	public boolean NoWinStats;
	public String Comments;
	public long Updated;
	public int SyncStatus;
	public long Saved;

	public Play fromCursor(Cursor c) {
		PlayId = CursorUtils.getInt(c, Plays.PLAY_ID, 0);
		GameId = CursorUtils.getInt(c, PlayItems.OBJECT_ID, BggContract.INVALID_ID);
		GameName = CursorUtils.getString(c, PlayItems.NAME);
		setDate(CursorUtils.getString(c, Plays.DATE));
		Quantity = CursorUtils.getInt(c, Plays.QUANTITY, 1);
		Length = CursorUtils.getInt(c, Plays.LENGTH);
		Location = CursorUtils.getString(c, Plays.LOCATION);
		Incomplete = CursorUtils.getBoolean(c, Plays.INCOMPLETE);
		NoWinStats = CursorUtils.getBoolean(c, Plays.NO_WIN_STATS);
		Comments = CursorUtils.getString(c, Plays.COMMENTS);
		Updated = CursorUtils.getLong(c, Plays.UPDATED_LIST);
		SyncStatus = CursorUtils.getInt(c, Plays.SYNC_STATUS);
		Saved = CursorUtils.getLong(c, Plays.UPDATED);
		return this;
	}

	public List<Player> getPlayers() {
		return mPlayers;
	}

	/**
	 * The date of the play in the yyyy-MM-dd format. This is the format the 'Geek uses and how it's stored in the
	 * Content DB.
	 * 
	 * @return
	 */
	public String getDate() {
		return String.format("%04d", Year) + "-" + String.format("%02d", Month + 1) + "-" + String.format("%02d", Day);
	}

	/**
	 * A text version of the date, formatted for display in the UI.
	 * 
	 * @return a localized date.
	 */
	public CharSequence getDateForDisplay() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Year, Month, Day);
		return df.format(calendar.getTime());
	}

	public void setDate(int year, int month, int day) {
		Year = year;
		Month = month;
		Day = day;
	}

	/**
	 * Sets the play's date
	 * 
	 * @param date
	 *            in the yyyy-MM-dd format
	 */
	public void setDate(String date) {
		Year = Integer.parseInt(date.substring(0, 4));
		Month = Integer.parseInt(date.substring(5, 7)) - 1;
		Day = Integer.parseInt(date.substring(8, 10));
	}

	public void clearPlayers() {
		mPlayers.clear();
	}

	public void addPlayer(Player player) {
		mPlayers.add(player);
	}

	public List<NameValuePair> toNameValuePairs() {
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("ajax", "1"));
		nvps.add(new BasicNameValuePair("action", "save"));
		nvps.add(new BasicNameValuePair("version", "2"));
		nvps.add(new BasicNameValuePair("objecttype", "thing"));
		if (hasBeenSynced()) {
			nvps.add(new BasicNameValuePair("playid", String.valueOf(PlayId)));
		}
		nvps.add(new BasicNameValuePair("objectid", String.valueOf(GameId)));
		nvps.add(new BasicNameValuePair("playdate", getDate()));
		// TODO: ask Aldie what this is
		nvps.add(new BasicNameValuePair("dateinput", getDate()));
		nvps.add(new BasicNameValuePair("length", String.valueOf(Length)));
		nvps.add(new BasicNameValuePair("location", Location));
		nvps.add(new BasicNameValuePair("quantity", String.valueOf(Quantity)));
		nvps.add(new BasicNameValuePair("incomplete", Incomplete ? "1" : "0"));
		nvps.add(new BasicNameValuePair("nowinstats", NoWinStats ? "1" : "0"));
		nvps.add(new BasicNameValuePair("comments", Comments));

		for (int i = 0; i < mPlayers.size(); i++) {
			nvps.addAll(mPlayers.get(i).toNameValuePairs(i));
		}

		LOGD(TAG, nvps.toString());
		return nvps;
	}

	public String toShortDescription(Context context) {
		Resources r = context.getResources();
		StringBuilder sb = new StringBuilder();
		sb.append(r.getString(R.string.share_play_played)).append(" ").append(GameName);
		sb.append(" ").append(r.getString(R.string.on)).append(" ").append(getDate());
		return sb.toString();
	}

	public String toLongDescription(Context context) {
		Resources r = context.getResources();
		StringBuilder sb = new StringBuilder();
		sb.append(r.getString(R.string.share_play_played)).append(" ").append(GameName);
		if (Quantity > 1) {
			sb.append(" ").append(Quantity).append(" ").append(r.getString(R.string.times));
		}
		sb.append(" ").append(r.getString(R.string.on)).append(" ").append(getDate());
		if (!TextUtils.isEmpty(Location)) {
			sb.append(" ").append(r.getString(R.string.at)).append(" ").append(Location);
		}
		if (mPlayers.size() > 0) {
			sb.append(" ").append(r.getString(R.string.with)).append(" ").append(mPlayers.size()).append(" ")
				.append(r.getString(R.string.players));
		}
		sb.append(" (www.boardgamegeek.com/boardgame/").append(GameId).append(")");
		return sb.toString();
	}

	/**
	 * @return plays/#
	 */
	public Uri uri() {
		return Plays.buildPlayUri(PlayId);
	}

	/**
	 * @return plays/#/items
	 */
	public Uri itemUri() {
		return Plays.buildItemUri(PlayId);
	}

	/**
	 * @return plays/#/items/#
	 */
	public Uri itemIdUri() {
		return Plays.buildItemUri(PlayId, GameId);
	}

	/**
	 * @return plays/#/players
	 */
	public Uri playerUri() {
		return Plays.buildPlayerUri(PlayId);
	}

	/**
	 * Determines if this plays has been synced by examining it's ID. It must be a valid ID the Geek would assign.
	 */
	public boolean hasBeenSynced() {
		return (PlayId > 0 && PlayId < UNSYNCED_PLAY_ID);
	}

	/**
	 * Determines if this play appears to have ended.
	 * 
	 * @return true, if the length has been entered or at least one of the players has won.
	 */
	public boolean hasEnded() {
		if (Length > 0) {
			return true;
		}
		if (mPlayers != null && mPlayers.size() > 0) {
			for (Player player : mPlayers) {
				if (player.Win) {
					return true;
				}
			}
		}
		return false;
	}
}
