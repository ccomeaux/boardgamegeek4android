package com.boardgamegeek.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class Player implements Parcelable {
	private static final String TAG = "Player";

	private static final String KEY_EXISTS = "EXISTS";
	private static final String KEY_NAME = "NAME";
	private static final String KEY_USERNAME = "USERNAME";
	private static final String KEY_TEAM_COLOR = "TEAM_COLOR";
	private static final String KEY_STARTING_POSITION = "STARTING_POSITION";
	private static final String KEY_SCORE = "SCORE";
	private static final String KEY_RATING = "RATING";
	private static final String KEY_NEW = "NEW";
	private static final String KEY_WIN = "WIN";

	public Player() {
	}

	public Player(Intent intent) {
		final Bundle bundle = intent.getExtras();
		if (bundle.getBoolean(KEY_EXISTS)) {
			Name = bundle.getString(KEY_NAME);
			Username = bundle.getString(KEY_USERNAME);
			TeamColor = bundle.getString(KEY_TEAM_COLOR);
			StartingPosition = bundle.getString(KEY_STARTING_POSITION);
			Score = bundle.getString(KEY_SCORE);
			Rating = bundle.getDouble(KEY_RATING);
			New = bundle.getBoolean(KEY_NEW);
			Win = bundle.getBoolean(KEY_WIN);
		}
	}

	public String Name;
	public String Username;
	public String TeamColor;
	public String StartingPosition;
	public String Score;
	public double Rating;
	public boolean New;
	public boolean Win;

	public Intent toIntent() {
		Intent intent = new Intent();
		intent.putExtra(KEY_EXISTS, true);
		intent.putExtra(KEY_NAME, Name);
		intent.putExtra(KEY_USERNAME, Username);
		intent.putExtra(KEY_TEAM_COLOR, TeamColor);
		intent.putExtra(KEY_STARTING_POSITION, StartingPosition);
		intent.putExtra(KEY_SCORE, Score);
		intent.putExtra(KEY_RATING, Rating);
		intent.putExtra(KEY_NEW, New);
		intent.putExtra(KEY_WIN, Win);
		return intent;
	}

	@Override
	public String toString() {
		return String.format("%1$s (%2$s) - %3$s", Name, Username, TeamColor);
	}

	public List<NameValuePair> toNameValuePairs(int index) {
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		addPair(nvps, index, "playerid", "player_" + index);
		addPair(nvps, index, "name", Name);
		addPair(nvps, index, "username", Username);
		addPair(nvps, index, "color", TeamColor);
		addPair(nvps, index, "position", StartingPosition);
		addPair(nvps, index, "score", Score);
		addPair(nvps, index, "rating", String.valueOf(Rating));
		addPair(nvps, index, "new", New ? "1" : "0");
		addPair(nvps, index, "win", Win ? "1" : "0");
		Log.d(TAG, nvps.toString());
		return nvps;
	}

	private void addPair(List<NameValuePair> nvps, int index, String key, String value) {
		nvps.add(new BasicNameValuePair("players[" + index + "][" + key + "]", value));
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(Name);
		out.writeString(Username);
		out.writeString(TeamColor);
		out.writeString(StartingPosition);
		out.writeString(Score);
		out.writeDouble(Rating);
		out.writeInt(New ? 1 : 0);
		out.writeInt(Win ? 1 : 0);
	}

	public static final Parcelable.Creator<Player> CREATOR = new Parcelable.Creator<Player>() {
		public Player createFromParcel(Parcel in) {
			return new Player(in);
		}

		public Player[] newArray(int size) {
			return new Player[size];
		}
	};

	private Player(Parcel in) {
		Name = in.readString();
		Username = in.readString();
		TeamColor = in.readString();
		StartingPosition = in.readString();
		Score = in.readString();
		Rating = in.readDouble();
		if (in.readInt() == 1) {
			New = true;
		}
		if (in.readInt() == 1) {
			Win = true;
		}
	}
}
