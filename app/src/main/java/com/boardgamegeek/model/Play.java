package com.boardgamegeek.model;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.extensions.StringKt;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class Play {
	public static final int QUANTITY_DEFAULT = 1;
	public static final int LENGTH_DEFAULT = 0;

	public int playId;
	public long dateInMillis;
	public int quantity;
	public int length;
	public boolean incomplete;
	public boolean noWinStats;
	public String location;
	public String gameName;
	public int gameId;
	public List<String> subtypes;
	public String comments;
	@NonNull private List<Player> players = new ArrayList<>();

	public long syncTimestamp;
	public long startTime;
	public int playerCount;
	public long deleteTimestamp;
	public long updateTimestamp;
	public long dirtyTimestamp;

	public Play() {
		init(BggContract.INVALID_ID, "");
	}

	public Play(int gameId, String gameName) {
		init(gameId, gameName);
	}

	private void init(int gameId, String gameName) {
		this.gameId = gameId;
		this.gameName = gameName;
		quantity = QUANTITY_DEFAULT;
		length = LENGTH_DEFAULT;
		location = "";
		comments = "";
		startTime = 0;
		players = new ArrayList<>();
	}

	// DATE

	public String getDateForApi() {
		return DateTimeUtils.formatDateForApi(dateInMillis);
	}

	public String getDateForDatabase() {
		return DateTimeUtils.formatDateForDatabase(dateInMillis);
	}

	public CharSequence getDateForDisplay(Context context) {
		return DateUtils.formatDateTime(context, dateInMillis, DateUtils.FORMAT_SHOW_DATE);
	}

	public void setDate(int year, int month, int day) {
		final Calendar c = Calendar.getInstance();
		c.set(Calendar.DAY_OF_MONTH, day);
		c.set(Calendar.MONTH, month);
		c.set(Calendar.YEAR, year);
		dateInMillis = c.getTimeInMillis();
	}

	public void setDateFromDatabase(String date) {
		dateInMillis = StringKt.toMillis(date, DateTimeUtils.FORMAT_DATABASE);
	}

	public void setCurrentDate() {
		final Calendar c = Calendar.getInstance();
		dateInMillis = c.getTimeInMillis();
	}

	// PLAYERS
	@NonNull
	public List<Player> getPlayers() {
		return players;
	}

	public int getPlayerCount() {
		return players.size();
	}

	public void setPlayers(List<Player> players) {
		this.players.addAll(players);
	}

	public void clearPlayers() {
		players.clear();
	}

	public void addPlayer(Player player) {
		// if player has seat, bump down other players
		if (!arePlayersCustomSorted() && player.getSeat() != Player.SEAT_UNKNOWN) {
			for (int i = players.size(); i >= player.getSeat(); i--) {
				Player p = getPlayerAtSeat(i);
				if (p != null) {
					p.setSeat(i + 1);
				}
			}
		}
		players.add(player);
		sortPlayers();
	}

	public void removePlayer(Player player, boolean resort) {
		if (players.size() == 0) return;
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

	/**
	 * Replaces a player at the position with a new player. If the position doesn't exists, the player is added instead.
	 */
	public void replaceOrAddPlayer(Player player, int position) {
		if (position >= 0 && position < players.size()) {
			players.set(position, player);
		} else {
			players.add(player);
		}
	}

	public Player getPlayerAtSeat(int seat) {
		for (Player player : players) {
			if (player != null && player.getSeat() == seat) {
				return player;
			}
		}
		return null;
	}

	public boolean reorderPlayers(int fromSeat, int toSeat) {
		if (players.size() == 0) return false;
		if (arePlayersCustomSorted()) return false;
		Player player = getPlayerAtSeat(fromSeat);
		if (player == null) return false;
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
		int playerCount = players.size();
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
		if (players.size() == 0) return;
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
	public void sortPlayers() {
		int index = 0;
		for (int i = 1; i <= players.size(); i++) {
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
		if (players.size() == 0) return false;
		if (!hasStartingPositions()) return true;
		int seat = 1;
		do {
			boolean foundSeat = false;
			for (Player player : players) {
				if (player != null && player.getSeat() == seat) {
					foundSeat = true;
					break;
				}
			}
			if (!foundSeat) {
				return true;
			}
			seat++;
			if (seat > players.size()) {
				return false;
			}
		} while (seat < 100);
		return true;
	}

	/**
	 * Determine if any player has a starting position.
	 */
	public boolean hasStartingPositions() {
		if (players.size() == 0) return false;
		for (Player player : players) {
			if (player != null && !TextUtils.isEmpty(player.getStartingPosition())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Remove the starting position for all players.
	 */
	public void clearPlayerPositions() {
		if (players.size() == 0) return;
		for (Player player : players) {
			if (player != null) {
				player.setStartingPosition(null);
			}
		}
	}

	// MISC

	/**
	 * Determine if any player has a team/color.
	 */
	public boolean hasColors() {
		if (players.size() == 0) return false;

		for (Player player : players) {
			if (player != null && !TextUtils.isEmpty(player.color)) {
				return true;
			}
		}

		return false;
	}

	public double getHighScore() {
		if (players.size() == 0) return 0.0;

		double highScore = -Double.MAX_VALUE;
		for (Player player : players) {
			if (player == null) continue;
			double score = StringUtils.parseDouble(player.score, Double.NaN);
			if (score > highScore) {
				highScore = score;
			}
		}

		return highScore;
	}

	/**
	 * Determines if this play appears to have started.
	 *
	 * @return true, if it's not ended and the start time has been set.
	 */
	public boolean hasStarted() {
		return length == 0 && startTime > 0;
	}

	public void start() {
		length = 0;
		startTime = System.currentTimeMillis();
	}

	public void resume() {
		startTime = System.currentTimeMillis() - length * DateUtils.MINUTE_IN_MILLIS;
		length = 0;
	}

	public void end() {
		if (startTime > 0) {
			length = DateTimeUtils.howManyMinutesOld(startTime);
			startTime = 0;
		} else {
			length = 0;
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
			&& (dateInMillis == p.dateInMillis)
			&& (quantity == p.quantity)
			&& (length == p.length)
			&& ((location == null && p.location == null) || (location != null && location.equals(p.location)))
			&& (incomplete == p.incomplete)
			&& (noWinStats == p.noWinStats)
			&& ((comments == null && p.comments == null) || (comments != null && comments.equals(p.comments)))
			&& (startTime == p.startTime)
			&& (players.size() == p.players.size());
		if (eq) {
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
		result = prime * result + (int) (dateInMillis ^ (dateInMillis >>> 32));
		result = prime * result + quantity;
		result = prime * result + length;
		result = prime * result + ((location == null) ? 0 : location.hashCode());
		result = prime * result + (incomplete ? 1 : 0);
		result = prime * result + (noWinStats ? 1 : 0);
		result = prime * result + ((comments == null) ? 0 : comments.hashCode());
		long u = Double.doubleToLongBits(syncTimestamp);
		result = prime * result + (int) (u ^ (u >>> 32));
		long t = Double.doubleToLongBits(startTime);
		result = prime * result + (int) (t ^ (t >>> 32));
		return result;
	}

	public String toShortDescription(Context context) {
		Resources r = context.getResources();
		return r.getString(R.string.play_description_game_segment, gameName) +
			r.getString(R.string.play_description_date_segment, getDateForDisplay(context));
	}

	public String toLongDescription(Context context) {
		Resources resources = context.getResources();
		StringBuilder sb = new StringBuilder();
		toLongDescriptionPrefix(context, sb);
		if (players.size() > 0) {
			sb.append(" ").append(resources.getString(R.string.with));
			if (arePlayersCustomSorted()) {
				for (Player player : players) {
					if (player != null) {
						sb.append("\n").append(player.toLongDescription(context));
					}
				}
			} else {
				for (int i = 0; i < players.size(); i++) {
					Player player = getPlayerAtSeat(i + 1);
					if (player != null) {
						sb.append("\n").append(player.toLongDescription(context));
					}
				}
			}
		}
		if (!TextUtils.isEmpty(comments)) {
			sb.append("\n").append(comments);
		}
		if (playId > 0) {
			sb.append("\n").append(resources.getString(R.string.play_description_play_url_segment, String.valueOf(playId)).trim());
		} else {
			sb.append("\n").append(resources.getString(R.string.play_description_game_url_segment, String.valueOf(gameId)).trim());
		}

		return sb.toString();
	}

	private void toLongDescriptionPrefix(Context context, StringBuilder sb) {
		Resources resources = context.getResources();
		sb.append(resources.getString(R.string.play_description_game_segment, gameName));
		if (quantity > 1) {
			sb.append(resources.getQuantityString(R.plurals.play_description_quantity_segment, quantity, quantity));
		}
		if (length > 0) {
			sb.append(resources.getString(R.string.play_description_length_segment, DateTimeUtils.describeMinutes(context, length)));
		}
		sb.append(resources.getString(R.string.play_description_date_segment, getDateForDisplay(context)));
		if (!TextUtils.isEmpty(location)) {
			sb.append(resources.getString(R.string.play_description_location_segment, location));
		}
	}
}
