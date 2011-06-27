package com.boardgamegeek.model;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.os.Bundle;
import android.util.Log;

public class Play {
	private static final String TAG = "Play";

	private static final String KEY_GAME_ID = "GAME_ID";
	private static final String KEY_YEAR = "YEAR";
	private static final String KEY_MONTH = "MONTH";
	private static final String KEY_DATY = "DAY";
	private static final String KEY_QUANTITY = "QUANTITY";
	private static final String KEY_LENGTH = "LENGTH";
	private static final String KEY_LOCATION = "LOCATION";
	private static final String KEY_INCOMPLETE = "INCOMPLETE";
	private static final String KEY_NOWINSTATS = "NO_WIN_STATS";
	private static final String KEY_COMMENTS = "COMMENTS";

	private DateFormat df = DateFormat.getDateInstance(DateFormat.FULL);

	private List<Player> mPlayers = new ArrayList<Player>();

	public Play(int gameId) {
		GameId = gameId;
		Quantity = 1;
		// set current date
		final Calendar c = Calendar.getInstance();
		Year = c.get(Calendar.YEAR);
		Month = c.get(Calendar.MONTH);
		Day = c.get(Calendar.DAY_OF_MONTH);
	}

	public Play(Bundle bundle) {
		GameId = bundle.getInt(KEY_GAME_ID);
		Year = bundle.getInt(KEY_YEAR);
		Month = bundle.getInt(KEY_MONTH);
		Day = bundle.getInt(KEY_DATY);
		Quantity = bundle.getInt(KEY_QUANTITY);
		Length = bundle.getInt(KEY_LENGTH);
		Location = bundle.getString(KEY_LOCATION);
		Incomplete = bundle.getBoolean(KEY_INCOMPLETE);
		NoWinStats = bundle.getBoolean(KEY_NOWINSTATS);
		Comments = bundle.getString(KEY_COMMENTS);
	}

	public int GameId;
	public int Year;
	public int Month;
	public int Day;
	public int Quantity;
	public int Length;
	public String Location;
	public boolean Incomplete;
	public boolean NoWinStats;
	public String Comments;

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

	public void clearPlayers() {
		mPlayers.clear();
	}

	public void addPlayer(Player player) {
		mPlayers.add(player);
	}

	public void saveState(Bundle bundle) {
		bundle.putInt(KEY_GAME_ID, GameId);
		bundle.putInt(KEY_YEAR, Year);
		bundle.putInt(KEY_MONTH, Month);
		bundle.putInt(KEY_DATY, Day);
		bundle.putInt(KEY_QUANTITY, Quantity);
		bundle.putInt(KEY_LENGTH, Length);
		bundle.putString(KEY_LOCATION, Location);
		bundle.putBoolean(KEY_INCOMPLETE, Incomplete);
		bundle.putBoolean(KEY_NOWINSTATS, NoWinStats);
		bundle.putString(KEY_COMMENTS, Comments);
	}

	public List<NameValuePair> toNameValuePairs() {
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("ajax", "1"));
		nvps.add(new BasicNameValuePair("action", "save"));
		nvps.add(new BasicNameValuePair("version", "2"));
		nvps.add(new BasicNameValuePair("objecttype", "thing"));
		nvps.add(new BasicNameValuePair("objectid", "" + GameId));
		nvps.add(new BasicNameValuePair("playdate", getFormattedDate()));
		// TODO: ask Aldie what this is
		nvps.add(new BasicNameValuePair("dateinput", getFormattedDate()));
		nvps.add(new BasicNameValuePair("length", "" + Length));
		nvps.add(new BasicNameValuePair("location", Location));
		nvps.add(new BasicNameValuePair("quantity", "" + Quantity));
		nvps.add(new BasicNameValuePair("incomplete", Incomplete ? "1" : "0"));
		nvps.add(new BasicNameValuePair("nowinstats", NoWinStats ? "1" : "0"));
		nvps.add(new BasicNameValuePair("comments", Comments));

		for (int i = 0; i < mPlayers.size(); i++) {
			nvps.addAll(mPlayers.get(i).toNameValuePairs(i));
		}

		Log.d(TAG, nvps.toString());
		return nvps;
	}
}
