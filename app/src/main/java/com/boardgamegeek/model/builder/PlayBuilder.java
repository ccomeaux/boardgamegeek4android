package com.boardgamegeek.model.builder;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;

import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.CursorUtils;
import com.boardgamegeek.util.StringUtils;

import java.util.ArrayList;

public class PlayBuilder {
	public static final String[] PLAY_PROJECTION = {
		Plays.PLAY_ID,
		Plays.ITEM_NAME,
		Plays.OBJECT_ID,
		Plays.DATE,
		Plays.LOCATION,
		Plays.LENGTH,
		Plays.QUANTITY,
		Plays.INCOMPLETE,
		Plays.NO_WIN_STATS,
		Plays.COMMENTS,
		Plays.SYNC_TIMESTAMP,
		Plays.START_TIME,
		Plays.PLAYER_COUNT,
		Plays.DELETE_TIMESTAMP,
		Plays.UPDATE_TIMESTAMP,
		Plays.DIRTY_TIMESTAMP
	};

	public static final String[] PLAY_PROJECTION_WITH_ID = StringUtils.concatenate(
		new String[] { Plays._ID }, PLAY_PROJECTION);

	public static final String[] PLAYER_PROJECTION = {
		PlayPlayers.USER_ID,
		PlayPlayers.USER_NAME,
		PlayPlayers.NAME,
		PlayPlayers.START_POSITION,
		PlayPlayers.COLOR,
		PlayPlayers.SCORE,
		PlayPlayers.RATING,
		PlayPlayers.NEW,
		PlayPlayers.WIN
	};

	public static final String KEY_PLAY_ID = "PLAY_ID";
	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_DATE = "DATE";
	public static final String KEY_QUANTITY = "QUANTITY";
	public static final String KEY_LENGTH = "LENGTH";
	public static final String KEY_LOCATION = "LOCATION";
	public static final String KEY_INCOMPLETE = "INCOMPLETE";
	public static final String KEY_NOWINSTATS = "NO_WIN_STATS";
	public static final String KEY_COMMENTS = "COMMENTS";
	public static final String KEY_PLAYERS = "PLAYERS";
	public static final String KEY_SYNC_TIMESTAMP = "SYNC_TIMESTAMP";
	public static final String KEY_START_TIME = "START_TIME";
	public static final String KEY_DELETE_TIMESTAMP = "DELETE_TIMESTAMP";
	public static final String KEY_UPDATE_TIMESTAMP = "UPDATE_TIMESTAMP";
	public static final String KEY_DIRTY_TIMESTAMP = "DIRTY_TIMESTAMP";

	public static Play fromCursor(Cursor cursor) {
		Play play = new Play();
		play.playId = CursorUtils.getInt(cursor, Plays.PLAY_ID, BggContract.INVALID_ID);
		play.gameId = CursorUtils.getInt(cursor, Plays.OBJECT_ID, BggContract.INVALID_ID);
		play.gameName = CursorUtils.getString(cursor, Plays.ITEM_NAME);
		play.setDate(CursorUtils.getString(cursor, Plays.DATE));
		play.quantity = CursorUtils.getInt(cursor, Plays.QUANTITY, Play.QUANTITY_DEFAULT);
		play.length = CursorUtils.getInt(cursor, Plays.LENGTH, Play.LENGTH_DEFAULT);
		play.location = CursorUtils.getString(cursor, Plays.LOCATION);
		play.setIncomplete(CursorUtils.getBoolean(cursor, Plays.INCOMPLETE));
		play.setNoWinStats(CursorUtils.getBoolean(cursor, Plays.NO_WIN_STATS));
		play.comments = CursorUtils.getString(cursor, Plays.COMMENTS);
		play.syncTimestamp = CursorUtils.getLong(cursor, Plays.SYNC_TIMESTAMP);
		play.startTime = CursorUtils.getLong(cursor, Plays.START_TIME);
		play.playerCount = CursorUtils.getInt(cursor, Plays.PLAYER_COUNT);
		play.deleteTimestamp = CursorUtils.getLong(cursor, Plays.DELETE_TIMESTAMP);
		play.updateTimestamp = CursorUtils.getLong(cursor, Plays.UPDATE_TIMESTAMP);
		play.dirtyTimestamp = CursorUtils.getLong(cursor, Plays.DIRTY_TIMESTAMP);
		return play;
	}

	public static Player playerFromCursor(Cursor cursor) {
		Player player = new Player();
		player.userid = CursorUtils.getInt(cursor, PlayPlayers.USER_ID);
		player.username = CursorUtils.getString(cursor, PlayPlayers.USER_NAME);
		player.name = CursorUtils.getString(cursor, PlayPlayers.NAME);
		player.color = CursorUtils.getString(cursor, PlayPlayers.COLOR);
		player.setStartingPosition(CursorUtils.getString(cursor, PlayPlayers.START_POSITION));
		player.score = CursorUtils.getString(cursor, PlayPlayers.SCORE);
		player.rating = CursorUtils.getDouble(cursor, PlayPlayers.RATING, Player.DEFAULT_RATING);
		player.New(CursorUtils.getBoolean(cursor, PlayPlayers.NEW));
		player.Win(CursorUtils.getBoolean(cursor, PlayPlayers.WIN));
		return player;
	}

	public static Cursor queryPlayers(Context context, long internalId) {
		return context.getContentResolver().query(Plays.buildPlayerUri(internalId), null, null, null, null);
	}

	public static void addPlayers(Cursor cursor, Play play) {
		play.clearPlayers();
		while (cursor != null && cursor.moveToNext()) {
			play.addPlayer(playerFromCursor(cursor));
		}
		if (play.getPlayerCount() > 9 && !play.arePlayersCustomSorted()) {
			play.sortPlayers();
		}
	}

	/**
	 * Copy the semantic play information to a new play. Does not include data related to syncing.
	 */
	public static Play copy(Play play) {
		Play copy = new Play(play.gameId, play.gameName);
		copy.setDate(play.getDate());
		copy.quantity = play.quantity;
		copy.length = play.length;
		copy.location = play.location;
		copy.setIncomplete(play.Incomplete());
		copy.setNoWinStats(play.NoWinStats());
		copy.comments = play.comments;
		copy.startTime = play.startTime;
		for (Player player : play.getPlayers()) {
			copy.addPlayer(new Player(player));
		}
		return copy;
	}

	public static Play rematch(Play play) {
		Play copy = new Play(play.gameId, play.gameName);
		copy.setCurrentDate();
		copy.location = play.location;
		copy.setNoWinStats(play.NoWinStats());
		boolean copyStartingPosition = !play.arePlayersCustomSorted();
		for (Player player : play.getPlayers()) {
			Player p = new Player();
			p.username = player.username;
			p.name = player.name;
			if (copyStartingPosition) {
				p.setStartingPosition(player.getStartingPosition());
			}
			p.color = player.color;
			p.rating = player.rating;
			p.score = "";
			p.Win(false);
			p.New(false);
			copy.addPlayer(p);
		}
		return copy;
	}

	public static void toBundle(Play play, Bundle bundle, String prefix) {
		if (play == null) {
			return;
		}
		bundle.putInt(prefix + KEY_PLAY_ID, play.playId);
		bundle.putInt(prefix + KEY_GAME_ID, play.gameId);
		bundle.putString(prefix + KEY_GAME_NAME, play.gameName);
		bundle.putString(prefix + KEY_DATE, play.getDate());
		bundle.putInt(prefix + KEY_QUANTITY, play.quantity);
		bundle.putInt(prefix + KEY_LENGTH, play.length);
		bundle.putString(prefix + KEY_LOCATION, play.location);
		bundle.putBoolean(prefix + KEY_INCOMPLETE, play.Incomplete());
		bundle.putBoolean(prefix + KEY_NOWINSTATS, play.NoWinStats());
		bundle.putString(prefix + KEY_COMMENTS, play.comments);
		bundle.putLong(prefix + KEY_SYNC_TIMESTAMP, play.syncTimestamp);
		bundle.putLong(prefix + KEY_START_TIME, play.startTime);
		bundle.putLong(prefix + KEY_DELETE_TIMESTAMP, play.deleteTimestamp);
		bundle.putLong(prefix + KEY_UPDATE_TIMESTAMP, play.updateTimestamp);
		bundle.putLong(prefix + KEY_DELETE_TIMESTAMP, play.dirtyTimestamp);
		bundle.putParcelableArrayList(prefix + KEY_PLAYERS, (ArrayList<? extends Parcelable>) play.getPlayers());
	}

	public static Play fromBundle(Bundle bundle, String prefix) {
		Play play = new Play();
		play.playId = bundle.getInt(prefix + KEY_PLAY_ID);
		play.gameId = bundle.getInt(prefix + KEY_GAME_ID);
		play.gameName = getString(bundle, prefix + KEY_GAME_NAME);
		play.setDate(bundle.getString(prefix + KEY_DATE));
		play.quantity = bundle.getInt(prefix + KEY_QUANTITY);
		play.length = bundle.getInt(prefix + KEY_LENGTH);
		play.location = getString(bundle, prefix + KEY_LOCATION);
		play.setIncomplete(bundle.getBoolean(prefix + KEY_INCOMPLETE));
		play.setNoWinStats(bundle.getBoolean(prefix + KEY_NOWINSTATS));
		play.comments = getString(bundle, prefix + KEY_COMMENTS);
		play.syncTimestamp = bundle.getLong(prefix + KEY_SYNC_TIMESTAMP);
		play.startTime = bundle.getLong(prefix + KEY_START_TIME);
		play.deleteTimestamp = bundle.getLong(prefix + KEY_DELETE_TIMESTAMP);
		play.updateTimestamp = bundle.getLong(prefix + KEY_UPDATE_TIMESTAMP);
		play.dirtyTimestamp = bundle.getLong(prefix + KEY_DIRTY_TIMESTAMP);
		ArrayList<Player> players = bundle.getParcelableArrayList(prefix + KEY_PLAYERS);
		play.setPlayers(players);
		return play;
	}

	private static String getString(final Bundle bundle, String key) {
		String s = bundle.getString(key);
		if (s == null) {
			return "";
		}
		return s;
	}
}
