package com.boardgamegeek.model;

import static com.boardgamegeek.util.LogUtils.LOGD;
import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.PlayItems;
import com.boardgamegeek.provider.BggContract.Plays;
import com.boardgamegeek.util.CursorUtils;

public class Play {
	/**
	 * The play has not been synced and isn't stored in the database
	 */
	public static final int SYNC_STATUS_NOT_STORED = -1;
	/**
	 * The play has been synced with the 'Geek
	 */
	public static final int SYNC_STATUS_SYNCED = 0;
	/**
	 * The play is ready to be synced, but doesn't exist on the 'Geek
	 */
	public static final int SYNC_STATUS_PENDING = 1;
	/**
	 * The play is currently being edited and will not sync until the user manually tries to sync it
	 */
	public static final int SYNC_STATUS_IN_PROGRESS = 2;

	public static final int UNSYNCED_PLAY_ID = 100000000;

	private static final String TAG = makeLogTag(Play.class);
	private DateFormat df = DateFormat.getDateInstance(DateFormat.FULL);
	private List<Player> mPlayers = new ArrayList<Player>();

	public Play() {
		init(0, -1, "");
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

	public Play populate(Cursor c) {
		PlayId = CursorUtils.getInt(c, Plays.PLAY_ID);
		GameId = CursorUtils.getInt(c, PlayItems.OBJECT_ID);
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

	public String getFormattedDate() {
		return String.format("%04d", Year) + "-" + String.format("%02d", Month + 1) + "-" + String.format("%02d", Day);
	}

	public CharSequence getDateText() {
		return df.format(new Date(Year - 1900, Month, Day));
	}

	public void setDate(int year, int month, int day) {
		Year = year;
		Month = month;
		Day = day;
	}

	public void setDate(String date) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		try {
			Date d = sdf.parse(date);
			Year = d.getYear() + 1900;
			Month = d.getMonth();
			Day = d.getDate();
		} catch (ParseException e) {
			LOGE(TAG, "Couldn't parse " + date, e);
		}
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
		nvps.add(new BasicNameValuePair("playdate", getFormattedDate()));
		// TODO: ask Aldie what this is
		nvps.add(new BasicNameValuePair("dateinput", getFormattedDate()));
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
		sb.append(" ").append(r.getString(R.string.share_play_on)).append(" ").append(getFormattedDate());
		return sb.toString();
	}

	public String toLongDescription(Context context) {
		Resources r = context.getResources();
		StringBuilder sb = new StringBuilder();
		sb.append(r.getString(R.string.share_play_played)).append(" ").append(GameName);
		if (Quantity > 1) {
			sb.append(" ").append(Quantity).append(" ").append(r.getString(R.string.share_play_times));
		}
		sb.append(" ").append(r.getString(R.string.share_play_on)).append(" ").append(getFormattedDate());
		if (!TextUtils.isEmpty(Location)) {
			sb.append(" ").append(r.getString(R.string.share_play_at)).append(" ").append(Location);
		}
		if (mPlayers.size() > 0) {
			sb.append(" ").append(r.getString(R.string.share_play_with)).append(" ").append(mPlayers.size())
					.append(" ").append(r.getString(R.string.share_play_players));
		}
		sb.append(" (www.boardgamegeek.com/boardgame/").append(GameId).append(")");
		return sb.toString();
	}

	public Uri getUri() {
		return Plays.buildPlayUri(PlayId);
	}

	public boolean hasBeenSynced() {
		return (PlayId > 0 && PlayId < UNSYNCED_PLAY_ID);
	}

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
