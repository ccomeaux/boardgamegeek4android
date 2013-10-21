package com.boardgamegeek.model;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.DateTimeUtils;

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
	public static final int QUANTITY_DEFAULT = 1;
	public static final int LENGTH_DEFAULT = 0;

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
	public long StartTime;
	private List<Player> mPlayers = new ArrayList<Player>();

	public Play() {
		init(BggContract.INVALID_ID, BggContract.INVALID_ID, "");
	}

	public Play(int gameId, String gameName) {
		init(BggContract.INVALID_ID, gameId, gameName);
	}

	public Play(int playId, int gameId, String gameName) {
		init(playId, gameId, gameName);
	}

	private void init(int playId, int gameId, String gameName) {
		PlayId = playId;
		GameId = gameId;
		GameName = gameName;
		Quantity = QUANTITY_DEFAULT;
		Length = LENGTH_DEFAULT;
		setCurrentDate();
		Location = "";
		Comments = "";
		StartTime = 0;
	}

	// URI

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

	// DATE

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
	public CharSequence getDateForDisplay(Context context) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Year, Month, Day);
		return DateUtils.formatDateTime(context, calendar.getTimeInMillis(), DateUtils.FORMAT_SHOW_DATE);
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

	private void setCurrentDate() {
		final Calendar c = Calendar.getInstance();
		Year = c.get(Calendar.YEAR);
		Month = c.get(Calendar.MONTH);
		Day = c.get(Calendar.DAY_OF_MONTH);
	}

	// PLAYERS

	public List<Player> getPlayers() {
		return mPlayers;
	}

	public void setPlayers(List<Player> players) {
		mPlayers = players;
	}

	public void clearPlayers() {
		mPlayers.clear();
	}

	public void addPlayer(Player player) {
		mPlayers.add(player);
	}

	public void removePlayer(Player player) {
		mPlayers.remove(player);
	}

	public void replacePlayer(Player player, int position) {
		mPlayers.set(position, player);
	}

	public Player getPlayerAtSeat(int seat) {
		for (Player player : mPlayers) {
			if (player.getSeat() == seat) {
				return player;
			}
		}
		return null;
	}

	/**
	 * Sets the start player based on the index, keeping the other players in order, assigns seats, then sorts
	 * 
	 * @param startPlayerIndex
	 *            The zero-based index of the new start player
	 */
	public void pickStartPlayer(int startPlayerIndex) {
		int playerCount = mPlayers.size();
		for (int i = 0; i < playerCount; i++) {
			Player p = mPlayers.get(i);
			p.setSeat((i - startPlayerIndex + playerCount) % playerCount + 1);
		}
		sortPlayers();
	}

	/**
	 * Randomizes the order of players, assigning seats to the new order.
	 */
	public void randomizePlayerOrder() {
		if (mPlayers == null || mPlayers.size() == 0) {
			return;
		}
		Collections.shuffle(mPlayers);
		int playerCount = mPlayers.size();
		for (int i = 0; i < playerCount; i++) {
			Player p = mPlayers.get(i);
			p.setSeat(i + 1);
		}
	}

	/**
	 * Sort the players by seat; unseated players left unsorted at the bottom of the list.
	 */
	public void sortPlayers() {
		int index = 0;
		for (int i = 1; i <= mPlayers.size(); i++) {
			Player p = getPlayerAtSeat(i);
			if (p != null) {
				mPlayers.remove(p);
				mPlayers.add(index, p);
				index++;
			}
		}
	}

	// MISC

	/**
	 * Determines if this plays has been synced by examining it's ID. It must be a valid ID the Geek would assign.
	 */
	public boolean hasBeenSynced() {
		return (PlayId > 0 && PlayId < UNSYNCED_PLAY_ID);
	}

	/**
	 * Determines if this play appears to have started.
	 * 
	 * @return true, if it's not ended and the start time has been set.
	 */
	public boolean hasStarted() {
		if (!hasEnded() && StartTime > 0) {
			return true;
		}
		return false;
	}

	public void start() {
		Length = 0;
		StartTime = System.currentTimeMillis();
	}

	public void end() {
		if (StartTime > 0) {
			Length = DateTimeUtils.howManyMinutesOld(StartTime);
			StartTime = 0;
		}
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

	public void start() {
		Length = 0;
		StartTime = System.currentTimeMillis();
	}

	public void end() {
		Length = DateTimeUtils.howManyMinutesOld(StartTime);
		StartTime = 0;
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
			&& (SyncStatus == p.SyncStatus) && (Saved == p.Saved) && (StartTime == p.StartTime)
			&& (mPlayers.size() == p.mPlayers.size());
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
		long t = Double.doubleToLongBits(StartTime);
		result = prime * result + (int) (t ^ (t >>> 32));
		return result;
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
}
