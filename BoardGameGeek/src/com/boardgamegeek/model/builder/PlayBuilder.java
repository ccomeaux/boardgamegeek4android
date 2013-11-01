package com.boardgamegeek.model.builder;

import java.util.ArrayList;

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

public class PlayBuilder {
	public static final String KEY_PLAY_ID = "PLAY_ID";
	public static final String KEY_GAME_ID = "GAME_ID";
	public static final String KEY_GAME_NAME = "GAME_NAME";
	public static final String KEY_YEAR = "YEAR";
	public static final String KEY_MONTH = "MONTH";
	public static final String KEY_DAY = "DAY";
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
		play.PlayId = CursorUtils.getInt(cursor, Plays.PLAY_ID, BggContract.INVALID_ID);
		play.GameId = CursorUtils.getInt(cursor, PlayItems.OBJECT_ID, BggContract.INVALID_ID);
		play.GameName = CursorUtils.getString(cursor, PlayItems.NAME);
		play.setDate(CursorUtils.getString(cursor, Plays.DATE));
		play.Quantity = CursorUtils.getInt(cursor, Plays.QUANTITY, Play.QUANTITY_DEFAULT);
		play.Length = CursorUtils.getInt(cursor, Plays.LENGTH, Play.LENGTH_DEFAULT);
		play.Location = CursorUtils.getString(cursor, Plays.LOCATION);
		play.Incomplete = CursorUtils.getBoolean(cursor, Plays.INCOMPLETE);
		play.NoWinStats = CursorUtils.getBoolean(cursor, Plays.NO_WIN_STATS);
		play.Comments = CursorUtils.getString(cursor, Plays.COMMENTS);
		play.Updated = CursorUtils.getLong(cursor, Plays.UPDATED_LIST);
		play.SyncStatus = CursorUtils.getInt(cursor, Plays.SYNC_STATUS);
		play.Saved = CursorUtils.getLong(cursor, Plays.UPDATED);
		play.StartTime = CursorUtils.getLong(cursor, Plays.START_TIME);
		if (includePlayers && context != null) {
			Cursor c = null;
			try {
				c = context.getContentResolver().query(play.playerUri(), null, null, null, null);
				while (c.moveToNext()) {
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

	public static Play copy(Play play) {
		Play copy = new Play(play.PlayId, play.GameId, play.GameName);
		copy.Year = play.Year;
		copy.Month = play.Month;
		copy.Day = play.Day;
		copy.Quantity = play.Quantity;
		copy.Length = play.Length;
		copy.Location = play.Location;
		copy.Incomplete = play.Incomplete;
		copy.NoWinStats = play.NoWinStats;
		copy.Comments = play.Comments;
		copy.Updated = play.Updated;
		copy.SyncStatus = play.SyncStatus;
		copy.Saved = play.Saved;
		copy.StartTime = play.StartTime;
		for (Player player : play.getPlayers()) {
			copy.addPlayer(new Player(player));
		}
		return copy;
	}

	public static Play playAgain(Play play) {
		Play copy = new Play(play.GameId, play.GameName);
		copy.Location = play.Location;
		copy.NoWinStats = play.NoWinStats;
		for (Player player : play.getPlayers()) {
			Player p = new Player();
			p.UserId = player.UserId;
			p.Name = player.Name;
			p.TeamColor = player.TeamColor;
			p.Rating = player.Rating;
			p.Score = "";
			p.Win = false;
			p.New = false;
			copy.addPlayer(p);
		}
		return copy;
	}

	public static void toBundle(Play play, Bundle bundle, String prefix) {
		if (play == null) {
			return;
		}
		bundle.putInt(prefix + KEY_PLAY_ID, play.PlayId);
		bundle.putInt(prefix + KEY_GAME_ID, play.GameId);
		bundle.putString(prefix + KEY_GAME_NAME, play.GameName);
		bundle.putInt(prefix + KEY_YEAR, play.Year);
		bundle.putInt(prefix + KEY_MONTH, play.Month);
		bundle.putInt(prefix + KEY_DAY, play.Day);
		bundle.putInt(prefix + KEY_QUANTITY, play.Quantity);
		bundle.putInt(prefix + KEY_LENGTH, play.Length);
		bundle.putString(prefix + KEY_LOCATION, play.Location);
		bundle.putBoolean(prefix + KEY_INCOMPLETE, play.Incomplete);
		bundle.putBoolean(prefix + KEY_NOWINSTATS, play.NoWinStats);
		bundle.putString(prefix + KEY_COMMENTS, play.Comments);
		bundle.putLong(prefix + KEY_UPDATED, play.Updated);
		bundle.putInt(prefix + KEY_SYNC_STATUS, play.SyncStatus);
		bundle.putLong(prefix + KEY_SAVED, play.Saved);
		bundle.putParcelableArrayList(prefix + KEY_PLAYERS, (ArrayList<? extends Parcelable>) play.getPlayers());
	}

	public static Play fromBundle(Bundle bundle, String prefix) {
		Play play = new Play();
		play.PlayId = bundle.getInt(prefix + KEY_PLAY_ID);
		play.GameId = bundle.getInt(prefix + KEY_GAME_ID);
		play.GameName = getString(bundle, prefix + KEY_GAME_NAME);
		play.Year = bundle.getInt(prefix + KEY_YEAR);
		play.Month = bundle.getInt(prefix + KEY_MONTH);
		play.Day = bundle.getInt(prefix + KEY_DAY);
		play.Quantity = bundle.getInt(prefix + KEY_QUANTITY);
		play.Length = bundle.getInt(prefix + KEY_LENGTH);
		play.Location = getString(bundle, prefix + KEY_LOCATION);
		play.Incomplete = bundle.getBoolean(prefix + KEY_INCOMPLETE);
		play.NoWinStats = bundle.getBoolean(prefix + KEY_NOWINSTATS);
		play.Comments = getString(bundle, prefix + KEY_COMMENTS);
		play.Updated = bundle.getLong(prefix + KEY_UPDATED);
		play.SyncStatus = bundle.getInt(prefix + KEY_SYNC_STATUS);
		play.Saved = bundle.getLong(prefix + KEY_SAVED);
		play.StartTime = bundle.getLong(prefix + KEY_START_TIME);
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
