package com.boardgamegeek.model;

import static com.boardgamegeek.util.LogUtils.LOGD;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.boardgamegeek.provider.BggContract.PlayPlayers;
import com.boardgamegeek.util.CursorUtils;
import com.boardgamegeek.util.StringUtils;

@Root(name = "player")
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
		name = "";
		username = "";
		color = "";
		setStartingPosition("");
		score = "";
	}

	public Player(Player player) {
		name = player.name;
		userid = player.userid;
		username = player.username;
		color = player.color;
		setStartingPosition(player.startposition);
		score = player.score;
		rating = player.rating;
		new_ = player.new_;
		win = player.win;
	}

	public Player(Intent intent) {
		this();
		final Bundle bundle = intent.getExtras();
		if (bundle.getBoolean(KEY_EXISTS)) {
			name = getString(bundle, KEY_NAME);
			userid = bundle.getInt(KEY_USER_ID);
			username = getString(bundle, KEY_USERNAME);
			color = getString(bundle, KEY_TEAM_COLOR);
			setStartingPosition(getString(bundle, KEY_STARTING_POSITION));
			score = getString(bundle, KEY_SCORE);
			rating = bundle.getDouble(KEY_RATING);
			new_ = bundle.getInt(KEY_NEW);
			win = bundle.getInt(KEY_WIN);
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
		userid = CursorUtils.getInt(cursor, PlayPlayers.USER_ID);
		username = CursorUtils.getString(cursor, PlayPlayers.USER_NAME);
		name = CursorUtils.getString(cursor, PlayPlayers.NAME);
		color = CursorUtils.getString(cursor, PlayPlayers.COLOR);
		setStartingPosition(CursorUtils.getString(cursor, PlayPlayers.START_POSITION));
		score = CursorUtils.getString(cursor, PlayPlayers.SCORE);
		rating = CursorUtils.getDouble(cursor, PlayPlayers.RATING, DEFAULT_RATING);
		New(CursorUtils.getBoolean(cursor, PlayPlayers.NEW));
		Win(CursorUtils.getBoolean(cursor, PlayPlayers.WIN));
	}

	@Attribute
	public String username;

	@Attribute
	public int userid;

	@Attribute
	public String name;

	@Attribute
	private String startposition;

	@Attribute
	public String color;

	@Attribute
	public String score;

	@Attribute(name = "new")
	private int new_;

	@Attribute
	public double rating;

	@Attribute
	private int win;

	public boolean Win() {
		return win == 1;
	}

	public void Win(boolean value) {
		win = value ? 1 : 0;
	}

	public boolean New() {
		return new_ == 1;
	}

	public void New(boolean value) {
		new_ = value ? 1 : 0;
	}

	private int mSeat;

	public String getStartingPosition() {
		return startposition;
	}

	public void setStartingPosition(String value) {
		startposition = value;
		if (StringUtils.isInteger(startposition)) {
			mSeat = Integer.parseInt(startposition);
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
		if (TextUtils.isEmpty(name)) {
			if (TextUtils.isEmpty(username)) {
				if (!TextUtils.isEmpty(color)) {
					description = color;
				}
			} else {
				description = username;
			}
		} else {
			description = name;
			if (!TextUtils.isEmpty(username)) {
				description += " (" + username + ")";
			}
		}
		return description;
	}

	public Intent toIntent() {
		Intent intent = new Intent();
		intent.putExtra(KEY_EXISTS, true);
		intent.putExtra(KEY_NAME, name);
		intent.putExtra(KEY_USER_ID, userid);
		intent.putExtra(KEY_USERNAME, username);
		intent.putExtra(KEY_TEAM_COLOR, color);
		intent.putExtra(KEY_STARTING_POSITION, startposition);
		intent.putExtra(KEY_SCORE, score);
		intent.putExtra(KEY_RATING, rating);
		intent.putExtra(KEY_NEW, new_);
		intent.putExtra(KEY_WIN, win);
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
		return (name == p.name || (name != null && name.equals(p.name))) && (userid == p.userid)
			&& (username == p.username || (username != null && username.equals(p.username)))
			&& (color == p.color || (color != null && color.equals(p.color)))
			&& (startposition == p.startposition || (startposition != null && startposition.equals(p.startposition)))
			&& (score == p.score || (score != null && score.equals(p.score))) && (rating == p.rating)
			&& (new_ == p.new_) && (win == p.win);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + userid;
		result = prime * result + ((username == null) ? 0 : username.hashCode());
		result = prime * result + ((color == null) ? 0 : color.hashCode());
		result = prime * result + ((startposition == null) ? 0 : startposition.hashCode());
		result = prime * result + ((score == null) ? 0 : score.hashCode());
		long r = Double.doubleToLongBits(rating);
		result = prime * result + (int) (r ^ (r >>> 32));
		result = prime * result + (New() ? 1231 : 1237);
		result = prime * result + (Win() ? 1231 : 1237);
		return result;
	}

	@Override
	public String toString() {
		return String.format("%1$s (%2$s) - %3$s", name, username, color);
	}

	public List<NameValuePair> toNameValuePairs(int index) {
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		addPair(nvps, index, "playerid", "player_" + index);
		addPair(nvps, index, "name", name);
		addPair(nvps, index, "username", username);
		addPair(nvps, index, "color", color);
		addPair(nvps, index, "position", startposition);
		addPair(nvps, index, "score", score);
		addPair(nvps, index, "rating", String.valueOf(rating));
		addPair(nvps, index, "new", String.valueOf(new_));
		addPair(nvps, index, "win", String.valueOf(win));
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
		out.writeString(name);
		out.writeInt(userid);
		out.writeString(username);
		out.writeString(color);
		out.writeString(startposition);
		out.writeString(score);
		out.writeDouble(rating);
		out.writeInt(new_);
		out.writeInt(win);
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
		name = in.readString();
		userid = in.readInt();
		username = in.readString();
		color = in.readString();
		setStartingPosition(in.readString());
		score = in.readString();
		rating = in.readDouble();
		new_ = in.readInt();
		win = in.readInt();
	}
}
