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
	private static final String KEY_DAY = "DAY";
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
		Location = "";
		Comments = "";
	}

	/**
	 * Deep copy constructor.
	 */
	public Play(Play play) {
		PlayId = play.PlayId;
		GameId = play.GameId;
		GameName = play.GameName;
		Year = play.Year;
		Month = play.Month;
		Day = play.Day;
		Quantity = play.Quantity;
		Length = play.Length;
		Location = play.Location;
		Incomplete = play.Incomplete;
		NoWinStats = play.NoWinStats;
		Comments = play.Comments;
		Updated = play.Updated;
		SyncStatus = play.SyncStatus;
		Saved = play.Saved;
		for (Player player : play.getPlayers()) {
			mPlayers.add(new Player(player));
		}
	}

	public Play(Bundle bundle, String prefix) {
		PlayId = bundle.getInt(prefix + KEY_PLAY_ID);
		GameId = bundle.getInt(prefix + KEY_GAME_ID);
		GameName = getString(bundle, prefix + KEY_GAME_NAME);
		Year = bundle.getInt(prefix + KEY_YEAR);
		Month = bundle.getInt(prefix + KEY_MONTH);
		Day = bundle.getInt(prefix + KEY_DAY);
		Quantity = bundle.getInt(prefix + KEY_QUANTITY);
		Length = bundle.getInt(prefix + KEY_LENGTH);
		Location = getString(bundle, prefix + KEY_LOCATION);
		Incomplete = bundle.getBoolean(prefix + KEY_INCOMPLETE);
		NoWinStats = bundle.getBoolean(prefix + KEY_NOWINSTATS);
		Comments = getString(bundle, prefix + KEY_COMMENTS);
		Updated = bundle.getLong(prefix + KEY_UPDATED);
		SyncStatus = bundle.getInt(prefix + KEY_SYNC_STATUS);
		Saved = bundle.getLong(prefix + KEY_SAVED);
		mPlayers = bundle.getParcelableArrayList(prefix + KEY_PLAYERS);
	}

	private String getString(final Bundle bundle, String key) {
		String s = bundle.getString(key);
		if (s == null) {
			return "";
		}
		return s;
	}

	public void saveState(Bundle bundle, String prefix) {
		bundle.putInt(prefix + KEY_PLAY_ID, PlayId);
		bundle.putInt(prefix + KEY_GAME_ID, GameId);
		bundle.putString(prefix + KEY_GAME_NAME, GameName);
		bundle.putInt(prefix + KEY_YEAR, Year);
		bundle.putInt(prefix + KEY_MONTH, Month);
		bundle.putInt(prefix + KEY_DAY, Day);
		bundle.putInt(prefix + KEY_QUANTITY, Quantity);
		bundle.putInt(prefix + KEY_LENGTH, Length);
		bundle.putString(prefix + KEY_LOCATION, Location);
		bundle.putBoolean(prefix + KEY_INCOMPLETE, Incomplete);
		bundle.putBoolean(prefix + KEY_NOWINSTATS, NoWinStats);
		bundle.putString(prefix + KEY_COMMENTS, Comments);
		bundle.putLong(prefix + KEY_UPDATED, Updated);
		bundle.putInt(prefix + KEY_SYNC_STATUS, SyncStatus);
		bundle.putLong(prefix + KEY_SAVED, Saved);
		bundle.putParcelableArrayList(prefix + KEY_PLAYERS, (ArrayList<? extends Parcelable>) mPlayers);
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

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (o == null || o.getClass() != this.getClass()) {
			return false;
		}

		Play p = (Play) o;
		boolean eq = (PlayId == p.PlayId) && (GameId == p.GameId) && (Year == p.Year) && (Month == p.Month)
			&& (Day == p.Day) && (Quantity == p.Quantity) && (Length == p.Length)
			&& (Location == p.Location || (Location != null && Location.equals(p.Location)))
			&& (Incomplete == p.Incomplete) && (NoWinStats == p.NoWinStats)
			&& (Comments == p.Comments || (Comments != null && Comments.equals(p.Comments))) && (Updated == p.Updated)
			&& (SyncStatus == p.SyncStatus) && (Saved == p.Saved) && (mPlayers.size() == p.mPlayers.size());
		if (eq) {
			for (int i = 0; i < mPlayers.size(); i++) {
				if (!mPlayers.get(i).equals(p.getPlayers().get(i))) {
					return false;
				}
			}
		}
		return eq;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + PlayId;
		result = prime * result + GameId;
		result = prime * result + ((GameName == null) ? 0 : GameName.hashCode());
		result = prime * result + Year;
		result = prime * result + Month;
		result = prime * result + Day;
		result = prime * result + Quantity;
		result = prime * result + Length;
		result = prime * result + ((Location == null) ? 0 : Location.hashCode());
		result = prime * result + (Incomplete ? 1231 : 1237);
		result = prime * result + (NoWinStats ? 1231 : 1237);
		result = prime * result + ((Comments == null) ? 0 : Comments.hashCode());
		long u = Double.doubleToLongBits(Updated);
		result = prime * result + (int) (u ^ (u >>> 32));
		result = prime * result + SyncStatus;
		long s = Double.doubleToLongBits(Saved);
		result = prime * result + (int) (s ^ (s >>> 32));
		return result;
	}
}
