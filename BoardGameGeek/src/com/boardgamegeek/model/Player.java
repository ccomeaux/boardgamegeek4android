package com.boardgamegeek.model;

import static com.boardgamegeek.util.LogUtils.LOGD;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.util.CursorUtils;
import com.boardgamegeek.util.StringUtils;

public class Player implements Parcelable {
	private static final String TAG = makeLogTag(Player.class);

	public static final double DEFAULT_RATING = 0.0;
	public static final int SEAT_UNKNOWN = -1;

	private static final String KEY_EXISTS = "EXISTS";
	private static final String KEY_NAME = "NAME";
	private static final String KEY_USER_ID = "USER_ID";
	private static final String KEY_USERNAME = "USERNAME";
	private static final String KEY_TEAM_COLOR = "TEAM_COLOR";
	private static final String KEY_STARTING_POSITION = "STARTING_POSITION";
	private static final String KEY_SCORE = "SCORE";
	private static final String KEY_RATING = "RATING";
	private static final String KEY_NEW = "NEW";
	private static final String KEY_WIN = "WIN";

	public Player() {
		Name = "";
		Username = "";
		TeamColor = "";
		setStartingPosition("");
		Score = "";
	}

	public Player(Player player) {
		Name = player.Name;
		UserId = player.UserId;
		Username = player.Username;
		TeamColor = player.TeamColor;
		setStartingPosition(player.mStartingPosition);
		Score = player.Score;
		Rating = player.Rating;
		New = player.New;
		Win = player.Win;
	}

	public Player(Intent intent) {
		final Bundle bundle = intent.getExtras();
		if (bundle.getBoolean(KEY_EXISTS)) {
			Name = getString(bundle, KEY_NAME);
			UserId = bundle.getInt(KEY_USER_ID);
			Username = getString(bundle, KEY_USERNAME);
			TeamColor = getString(bundle, KEY_TEAM_COLOR);
			setStartingPosition(getString(bundle, KEY_STARTING_POSITION));
			Score = getString(bundle, KEY_SCORE);
			Rating = bundle.getDouble(KEY_RATING);
			New = bundle.getBoolean(KEY_NEW);
			Win = bundle.getBoolean(KEY_WIN);
		}
	}

	private String getString(final Bundle bundle, String key) {
		String s = bundle.getString(key);
		if (s == null) {
			return "";
		}
		return s;
	}

	public Player(Cursor cursor) {
		UserId = CursorUtils.getInt(cursor, PlayPlayers.USER_ID);
		Username = CursorUtils.getString(cursor, PlayPlayers.USER_NAME);
		Name = CursorUtils.getString(cursor, PlayPlayers.NAME);
		TeamColor = CursorUtils.getString(cursor, PlayPlayers.COLOR);
		setStartingPosition(CursorUtils.getString(cursor, PlayPlayers.START_POSITION));
		Score = CursorUtils.getString(cursor, PlayPlayers.SCORE);
		Rating = CursorUtils.getDouble(cursor, PlayPlayers.RATING, DEFAULT_RATING);
		New = CursorUtils.getBoolean(cursor, PlayPlayers.NEW);
		Win = CursorUtils.getBoolean(cursor, PlayPlayers.WIN);
	}

	public String Name;
	public int UserId;
	public String Username;
	public String TeamColor;
	private String mStartingPosition;
	private int mSeat;
	public String Score;
	public double Rating;
	public boolean New;
	public boolean Win;

	public String getStartingPosition() {
		return mStartingPosition;
	}

	public void setStartingPosition(String value) {
		mStartingPosition = value;
		if (StringUtils.isInteger(mStartingPosition)) {
			mSeat = Integer.parseInt(mStartingPosition);
		} else {
			mSeat = SEAT_UNKNOWN;
		}
	}

	public int getSeat() {
		return mSeat;
	}

	public void setSeat(int value) {
		setStartingPosition(String.valueOf(value));
	}

	public String getDescsription() {
		String description = "";
		if (TextUtils.isEmpty(Name)) {
			if (TextUtils.isEmpty(Username)) {
				if (!TextUtils.isEmpty(TeamColor)) {
					description = TeamColor;
				}
			} else {
				description = Username;
			}
		} else {
			description = Name;
			if (!TextUtils.isEmpty(Username)) {
				description += " (" + Username + ")";
			}
		}
		return description;
	}

	public Intent toIntent() {
		Intent intent = new Intent();
		intent.putExtra(KEY_EXISTS, true);
		intent.putExtra(KEY_NAME, Name);
		intent.putExtra(KEY_USER_ID, UserId);
		intent.putExtra(KEY_USERNAME, Username);
		intent.putExtra(KEY_TEAM_COLOR, TeamColor);
		intent.putExtra(KEY_STARTING_POSITION, mStartingPosition);
		intent.putExtra(KEY_SCORE, Score);
		intent.putExtra(KEY_RATING, Rating);
		intent.putExtra(KEY_NEW, New);
		intent.putExtra(KEY_WIN, Win);
		return intent;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (o == null || o.getClass() != this.getClass()) {
			return false;
		}

		Player p = (Player) o;
		return (Name == p.Name || (Name != null && Name.equals(p.Name)))
			&& (UserId == p.UserId)
			&& (Username == p.Username || (Username != null && Username.equals(p.Username)))
			&& (TeamColor == p.TeamColor || (TeamColor != null && TeamColor.equals(p.TeamColor)))
			&& (mStartingPosition == p.mStartingPosition || (mStartingPosition != null && mStartingPosition
				.equals(p.mStartingPosition))) && (Score == p.Score || (Score != null && Score.equals(p.Score)))
			&& (Rating == p.Rating) && (New == p.New) && (Win == p.Win);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((Name == null) ? 0 : Name.hashCode());
		result = prime * result + UserId;
		result = prime * result + ((Username == null) ? 0 : Username.hashCode());
		result = prime * result + ((TeamColor == null) ? 0 : TeamColor.hashCode());
		result = prime * result + ((mStartingPosition == null) ? 0 : mStartingPosition.hashCode());
		result = prime * result + ((Score == null) ? 0 : Score.hashCode());
		long r = Double.doubleToLongBits(Rating);
		result = prime * result + (int) (r ^ (r >>> 32));
		result = prime * result + (New ? 1231 : 1237);
		result = prime * result + (Win ? 1231 : 1237);
		return result;
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
		addPair(nvps, index, "position", mStartingPosition);
		addPair(nvps, index, "score", Score);
		addPair(nvps, index, "rating", String.valueOf(Rating));
		addPair(nvps, index, "new", New ? "1" : "0");
		addPair(nvps, index, "win", Win ? "1" : "0");
		LOGD(TAG, nvps.toString());
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
		out.writeInt(UserId);
		out.writeString(Username);
		out.writeString(TeamColor);
		out.writeString(mStartingPosition);
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
		UserId = in.readInt();
		Username = in.readString();
		TeamColor = in.readString();
		setStartingPosition(in.readString());
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
