package com.boardgamegeek.model.builder;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;

import com.boardgamegeek.model.Play;
import com.boardgamegeek.model.Player;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.CursorUtils;

import java.util.ArrayList;

public class PlayBuilder {
	public static final String[] PLAY_PROJECTION = {
		Plays.PLAY_ID,
		PlayItems.NAME,
		PlayItems.OBJECT_ID,
		Plays.DATE,
		Plays.LOCATION,
		Plays.LENGTH,
		Plays.QUANTITY,
		Plays.INCOMPLETE,
		Plays.NO_WIN_STATS,
		Plays.COMMENTS,
		Plays.UPDATED_LIST,
		Plays.SYNC_STATUS,
		Plays.UPDATED,
		Plays.START_TIME,
		Plays.PLAYER_COUNT
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
	public static final String KEY_UPDATED = "UPDATED";
	public static final String KEY_SYNC_STATUS = "SYNC_STATUS";
	public static final String KEY_SAVED = "SAVED";
	public static final String KEY_START_TIME = "START_TIME";

	public static Play fromCursor(Cursor cursor) {
		return fromCursor(cursor, null, false);
	}

	public static Play fromCursor(Cursor cursor, Context context, boolean includePlayers) {
		Play play = new Play();
		play.playId = CursorUtils.getInt(cursor, Plays.PLAY_ID, BggContract.INVALID_ID);
		play.gameId = CursorUtils.getInt(cursor, PlayItems.OBJECT_ID, BggContract.INVALID_ID);
		play.gameName = CursorUtils.getString(cursor, PlayItems.NAME);
		play.setDate(CursorUtils.getString(cursor, Plays.DATE));
		play.quantity = CursorUtils.getInt(cursor, Plays.QUANTITY, Play.QUANTITY_DEFAULT);
		play.length = CursorUtils.getInt(cursor, Plays.LENGTH, Play.LENGTH_DEFAULT);
		play.location = CursorUtils.getString(cursor, Plays.LOCATION);
		play.setIncomplete(CursorUtils.getBoolean(cursor, Plays.INCOMPLETE));
		play.setNoWinStats(CursorUtils.getBoolean(cursor, Plays.NO_WIN_STATS));
		play.comments = CursorUtils.getString(cursor, Plays.COMMENTS);
		play.updated = CursorUtils.getLong(cursor, Plays.UPDATED_LIST);
		play.syncStatus = CursorUtils.getInt(cursor, Plays.SYNC_STATUS);
		play.saved = CursorUtils.getLong(cursor, Plays.UPDATED);
		play.startTime = CursorUtils.getLong(cursor, Plays.START_TIME);
		play.playerCount = CursorUtils.getInt(cursor, Plays.PLAYER_COUNT);
		if (includePlayers && context != null) {
			Cursor c = null;
			try {
				c = context.getContentResolver().query(play.playerUri(), null, null, null, null);
				while (c != null && c.moveToNext()) {
					play.addPlayer(new Player(c));
				}
			} finally {
				if (c != null) {
					c.close();
				}
			}
		}
		return play;
	}

	/**
	 * Copy the semantic play information to a new play. Does not include data related to syncing.
	 */
	public static Play copy(Play play) {
		Play copy = new Play(play.playId, play.gameId, play.gameName);
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
		bundle.putLong(prefix + KEY_UPDATED, play.updated);
		bundle.putInt(prefix + KEY_SYNC_STATUS, play.syncStatus);
		bundle.putLong(prefix + KEY_SAVED, play.saved);
		bundle.putLong(prefix + KEY_START_TIME, play.startTime);
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
		play.updated = bundle.getLong(prefix + KEY_UPDATED);
		play.syncStatus = bundle.getInt(prefix + KEY_SYNC_STATUS);
		play.saved = bundle.getLong(prefix + KEY_SAVED);
		play.startTime = bundle.getLong(prefix + KEY_START_TIME);
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
