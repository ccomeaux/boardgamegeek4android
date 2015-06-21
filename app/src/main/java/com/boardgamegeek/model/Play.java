package com.boardgamegeek.model;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.DateTimeUtils;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Root(name = "play")
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
	/**
	 * The play is ready to be updated or deleted
	 */
	public static final int SYNC_STATUS_PENDING = 4;

	public static final int UNSYNCED_PLAY_ID = 100000000;
	public static final int QUANTITY_DEFAULT = 1;
	public static final int LENGTH_DEFAULT = 0;
	private static final DateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

	@Attribute(name = "id")
	public int playId;

	@Attribute
	private String date;
	private long playDate = DateTimeUtils.UNPARSED_DATE;

	@Attribute
	public int quantity;

	@Attribute
	public int length;

	@Attribute
	private int incomplete;

	public boolean Incomplete() {
		return incomplete == 1;
	}

	public void setIncomplete(boolean value) {
		incomplete = value ? 1 : 0;
	}

	@Attribute
	private int nowinstats;

	public boolean NoWinStats() {
		return nowinstats == 1;
	}

	public void setNoWinStats(boolean value) {
		nowinstats = value ? 1 : 0;
	}

	@Attribute
	public String location;

	@Path("item")
	@Attribute(name = "name")
	public String gameName;

	@Path("item")
	@Attribute(name = "objectid")
	public int gameId;

	@Path("item")
	@Attribute
	public String objecttype;

	@Path("item")
	@ElementList
	List<Subtype> subtypes;

	@Root(name = "subtype")
	static class Subtype {
		@Attribute
		String value;
	}

	@Element(required = false)
	public String comments;

	public long updated;
	public int syncStatus;
	public long saved;
	public long startTime;
	public int playerCount;

	@ElementList(required = false)
	private List<Player> players;

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
		this.playId = playId;
		this.gameId = gameId;
		this.gameName = gameName;
		quantity = QUANTITY_DEFAULT;
		length = LENGTH_DEFAULT;
		location = "";
		comments = "";
		startTime = 0;
		players = new ArrayList<>();
	}

	// URI

	/**
	 * @return plays/#
	 */
	public Uri uri() {
		return Plays.buildPlayUri(playId);
	}

	/**
	 * @return plays/#/items
	 */
	public Uri itemUri() {
		return Plays.buildItemUri(playId);
	}

	/**
	 * @return plays/#/items/#
	 */
	public Uri itemIdUri() {
		return Plays.buildItemUri(playId, gameId);
	}

	/**
	 * @return plays/#/players
	 */
	public Uri playerUri() {
		return Plays.buildPlayerUri(playId);
	}

	// DATE

	/**
	 * The date of the play in the yyyy-MM-dd format. This is the format the 'Geek uses and how it's stored in the
	 * Content DB.
	 *
	 * @return The formatted date
	 */
	public String getDate() {
		playDate = DateTimeUtils.tryParseDate(playDate, date, FORMAT);
		return DateTimeUtils.formatDateForApi(playDate);
	}

	public long getDateInMillis() {
		playDate = DateTimeUtils.tryParseDate(playDate, date, FORMAT);
		return playDate;
	}

	/**
	 * A text version of the date, formatted for display in the UI.
	 *
	 * @return a localized date.
	 */
	public CharSequence getDateForDisplay(Context context) {
		playDate = DateTimeUtils.tryParseDate(playDate, date, FORMAT);
		return DateUtils.formatDateTime(context, playDate, DateUtils.FORMAT_SHOW_DATE);
	}

	public void setDate(int year, int month, int day) {
		playDate = DateTimeUtils.UNPARSED_DATE;
		date = DateTimeUtils.formatDateForApi(year, month, day);
		playDate = DateTimeUtils.tryParseDate(playDate, date, FORMAT);
	}

	/**
	 * Sets the play's date
	 *
	 * @param date in the yyyy-MM-dd format
	 */
	public void setDate(String date) {
		playDate = DateTimeUtils.UNPARSED_DATE;
		this.date = date;
		playDate = DateTimeUtils.tryParseDate(playDate, date, FORMAT);
	}

	public void setCurrentDate() {
		final Calendar c = Calendar.getInstance();
		setDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
	}

	// PLAYERS

	public List<Player> getPlayers() {
		if (players == null) {
			return new ArrayList<>();
		}
		return players;
	}

	public int getPlayerCount() {
		if (players == null) {
			return 0;
		}
		return players.size();
	}

	public void setPlayers(List<Player> players) {
		if (players != null) {
			this.players.addAll(players);
		}
	}

	public void setPlayers(Cursor cursor) {
		clearPlayers();
		while (cursor.moveToNext()) {
			Player player = new Player(cursor);
			addPlayer(player);
		}
		// When player count is in the double digits, numeric starting positions aren't sorted correctly
		// TODO don't assume we should be sorting by starting positions
		if (getPlayerCount() > 9 && !arePlayersCustomSorted()) {
			sortPlayers();
		}
	}

	public void clearPlayers() {
		if (players != null) {
			players.clear();
		}
	}

	public void addPlayer(Player player) {
		if (players == null) {
			players = new ArrayList<>();
		}
		players.add(player);
	}

	public void removePlayer(Player player, boolean resort) {
		if (players == null || getPlayerCount() == 0) {
			return;
		}
		if (resort && !arePlayersCustomSorted()) {
			for (int i = player.getSeat(); i < players.size(); i++) {
				Player p = getPlayerAtSeat(i + 1);
				if (p != null) {
					p.setSeat(i);
				}
			}
		}
		players.remove(player);
	}

	public void replacePlayer(Player player, int position) {
		// TODO
		players.set(position, player);
	}

	public Player getPlayerAtSeat(int seat) {
		if (players == null) {
			return null;
		}
		for (Player player : players) {
			if (player.getSeat() == seat) {
				return player;
			}
		}
		return null;
	}

	public boolean reorderPlayers(int fromSeat, int toSeat) {
		if (players == null || getPlayerCount() == 0) {
			return false;
		}
		if (arePlayersCustomSorted()) {
			return false;
		}
		Player player = getPlayerAtSeat(fromSeat);
		if (player == null) {
			return false;
		}
		player.setSeat(Player.SEAT_UNKNOWN);
		try {
			if (fromSeat > toSeat) {
				for (int i = fromSeat - 1; i >= toSeat; i--) {
					getPlayerAtSeat(i).setSeat(i + 1);
				}
			} else {
				for (int i = fromSeat + 1; i <= toSeat; i++) {
					getPlayerAtSeat(i).setSeat(i - 1);
				}
			}
		} catch (NullPointerException e) {
			return false;
		}
		player.setSeat(toSeat);
		sortPlayers();
		return true;
	}

	/**
	 * Sets the start player based on the index, keeping the other players in order, assigns seats, then sorts
	 *
	 * @param startPlayerIndex The zero-based index of the new start player
	 */
	public void pickStartPlayer(int startPlayerIndex) {
		int playerCount = getPlayerCount();
		for (int i = 0; i < playerCount; i++) {
			Player p = players.get(i);
			p.setSeat((i - startPlayerIndex + playerCount) % playerCount + 1);
		}
		sortPlayers();
	}

	/**
	 * Randomizes the order of players, assigning seats to the new order.
	 */
	public void randomizePlayerOrder() {
		if (players == null || players.size() == 0) {
			return;
		}
		Collections.shuffle(players);
		int playerCount = players.size();
		for (int i = 0; i < playerCount; i++) {
			Player p = players.get(i);
			p.setSeat(i + 1);
		}
	}

	/**
	 * Sort the players by seat; unseated players left unsorted at the bottom of the list.
	 */
	private void sortPlayers() {
		int index = 0;
		for (int i = 1; i <= getPlayerCount(); i++) {
			Player p = getPlayerAtSeat(i);
			if (p != null) {
				players.remove(p);
				players.add(index, p);
				index++;
			}
		}
	}

	/**
	 * Determine if the starting positions indicate the players are custom sorted.
	 */
	public boolean arePlayersCustomSorted() {
		if (!hasStartingPositions()) {
			return true;
		}

		int seat = 1;
		do {
			boolean foundSeat = false;
			for (Player player : players) {
				if (player.getSeat() == seat) {
					foundSeat = true;
					break;
				}
			}
			if (!foundSeat) {
				return true;
			}
			seat++;
			if (seat > getPlayerCount()) {
				return false;
			}
		} while (seat < 100);
		return true;
	}

	/**
	 * Determine if any player has a starting position.
	 */
	public boolean hasStartingPositions() {
		if (getPlayerCount() == 0) {
			return false;
		}

		for (Player player : players) {
			if (!TextUtils.isEmpty(player.getStartingPosition())) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Remove the starting position for all players.
	 */
	public void clearPlayerPositions() {
		if (getPlayerCount() == 0) {
			return;
		}

		for (Player player : players) {
			player.setStartingPosition(null);
		}
	}

	// MISC

    /**
     * Determine if any player has a team/color.
     */
    public boolean hasColors() {
        if (getPlayerCount() == 0) {
            return false;
        }

        for (Player player : players) {
            if (!TextUtils.isEmpty(player.color)) {
                return true;
            }
        }

        return false;
    }

    /**
	 * Determines if this plays has been synced by examining it's ID. It must be a valid ID the Geek would assign.
	 */
	public boolean hasBeenSynced() {
		return Play.hasBeenSynced(playId);
	}

	public static boolean hasBeenSynced(int playId) {
		return (playId > 0 && playId < UNSYNCED_PLAY_ID);
	}

	/**
	 * Determines if this play appears to have started.
	 *
	 * @return true, if it's not ended and the start time has been set.
	 */
	public boolean hasStarted() {
		return length == 0 && startTime > 0;
	}

	/**
	 * Determines if this play appears to have ended.
	 *
	 * @return true, if the length has been entered or at least one of the players has won.
	 */
	public boolean hasEnded() {
		if (length > 0) {
			return true;
		}
		if (players != null && players.size() > 0) {
			for (Player player : players) {
				if (player.Win()) {
					return true;
				}
			}
		}
		return false;
	}

	public void start() {
		length = 0;
		startTime = System.currentTimeMillis();
	}

	public void end() {
		if (startTime > 0) {
			length = DateTimeUtils.howManyMinutesOld(startTime);
			startTime = 0;
		}
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
		boolean eq = (playId == p.playId)
			&& (gameId == p.gameId)
			&& (playDate == p.playDate)
			&& (quantity == p.quantity)
			&& (length == p.length)
			&& ((location == null && p.location == null) || (location != null && location.equals(p.location)))
			&& (incomplete == p.incomplete)
			&& (nowinstats == p.nowinstats)
			&& ((comments == null && p.comments == null) || (comments != null && comments.equals(p.comments)))
			&& (startTime == p.startTime)
			&& ((players == null && p.players == null) || (players != null && p.players != null && players.size() == p.players
			.size()));
		if (eq && players != null) {
			for (int i = 0; i < players.size(); i++) {
				if (!players.get(i).equals(p.getPlayers().get(i))) {
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
		result = prime * result + playId;
		result = prime * result + gameId;
		result = prime * result + ((gameName == null) ? 0 : gameName.hashCode());
		result = prime * result + (int) (playDate ^ (playDate >>> 32));
		result = prime * result + quantity;
		result = prime * result + length;
		result = prime * result + ((location == null) ? 0 : location.hashCode());
		result = prime * result + incomplete;
		result = prime * result + nowinstats;
		result = prime * result + ((comments == null) ? 0 : comments.hashCode());
		long u = Double.doubleToLongBits(updated);
		result = prime * result + (int) (u ^ (u >>> 32));
		result = prime * result + syncStatus;
		long s = Double.doubleToLongBits(saved);
		result = prime * result + (int) (s ^ (s >>> 32));
		long t = Double.doubleToLongBits(startTime);
		result = prime * result + (int) (t ^ (t >>> 32));
		return result;
	}

	public String toShortDescription(Context context) {
		Resources r = context.getResources();
		return r.getString(R.string.share_play_played) + " " + gameName + " " + r.getString(R.string.on) + " " + getDate();
	}

	public String toLongDescription(Context context) {
		Resources r = context.getResources();
		StringBuilder sb = new StringBuilder();
		sb.append(r.getString(R.string.share_play_played)).append(" ").append(gameName);
		if (quantity > 1) {
			sb.append(" ").append(quantity).append(" ").append(r.getString(R.string.times));
		}
		sb.append(" ").append(r.getString(R.string.on)).append(" ").append(getDate());
		if (!TextUtils.isEmpty(location)) {
			sb.append(" ").append(r.getString(R.string.at)).append(" ").append(location);
		}
		if (players.size() > 0) {
			sb.append(" ").append(r.getString(R.string.with)).append(" ").append(players.size()).append(" ")
				.append(r.getString(R.string.players));
		}
		sb.append(" (www.boardgamegeek.com/boardgame/").append(gameId).append(")");
		return sb.toString();
	}
}
