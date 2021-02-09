package com.boardgamegeek.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.boardgamegeek.extensions.DoubleUtils;
import com.boardgamegeek.util.StringUtils;

import java.text.DecimalFormat;

public class Player implements Parcelable {
	public static final double DEFAULT_RATING = 0.0;
	public static final int SEAT_UNKNOWN = -1;
	public static final int SEAT_UNPARSED = -2;

	public Player() {
		name = "";
		username = "";
		color = "";
		setStartingPosition("");
		score = "";
	}

	public Player(Player player) {
		name = player.name;
		userId = player.userId;
		username = player.username;
		color = player.color;
		setStartingPosition(player.startposition);
		score = player.score;
		rating = player.rating;
		isNew = player.isNew;
		isWin = player.isWin;
	}

	public String username;
	public int userId;
	public String name;
	private String startposition;
	public String color;
	public String score;
	public boolean isNew;
	public double rating;
	public boolean isWin;

	private int seat = SEAT_UNPARSED;

	public String getStartingPosition() {
		return startposition;
	}

	public void setStartingPosition(String value) {
		seat = SEAT_UNPARSED;
		startposition = value;
	}

	public int getSeat() {
		if (seat == SEAT_UNPARSED) {
			seat = StringUtils.parseInt(startposition, SEAT_UNKNOWN);
		}
		return seat;
	}

	public void setSeat(int value) {
		setStartingPosition(String.valueOf(value));
	}

	public String getRatingDescription() {
		if (rating > 1.0 && rating <= 10.0) {
			return new DecimalFormat("0.#").format(rating);
		}
		return "";
	}

	public String getScoreDescription() {
		if (StringUtils.isNumeric(score)) {
			double s = StringUtils.parseDouble(score);
			return DoubleUtils.asScore(s, null, 0, new DecimalFormat("#,##0.###"));
		} else {
			return score;
		}
	}

	public String getDescription() {
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

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (o == null || o.getClass() != this.getClass()) {
			return false;
		}

		Player p = (Player) o;
		return ((name == null && p.name == null) || (name != null && name.equals(p.name)))
			&& (userId == p.userId)
			&& ((username == null && p.username == null) || (username != null && username.equals(p.username)))
			&& ((color == null && p.color == null) || (color != null && color.equals(p.color)))
			&& ((startposition == null && p.startposition == null) || (startposition != null && startposition.equals(p.startposition)))
			&& ((score == null && p.score == null) || (score != null && score.equals(p.score)))
			&& (rating == p.rating) && (isNew == p.isNew) && (isWin == p.isWin);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + userId;
		result = prime * result + ((username == null) ? 0 : username.hashCode());
		result = prime * result + ((color == null) ? 0 : color.hashCode());
		result = prime * result + ((startposition == null) ? 0 : startposition.hashCode());
		result = prime * result + ((score == null) ? 0 : score.hashCode());
		long r = Double.doubleToLongBits(rating);
		result = prime * result + (int) (r ^ (r >>> 32));
		result = prime * result + (isNew ? 1231 : 1237);
		result = prime * result + (isWin ? 1231 : 1237);
		return result;
	}

	@Override
	public String toString() {
		return String.format("%1$s (%2$s) - %3$s", name, username, color);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(name);
		out.writeInt(userId);
		out.writeString(username);
		out.writeString(color);
		out.writeString(startposition);
		out.writeString(score);
		out.writeDouble(rating);
		out.writeInt(isNew ? 1 : 0);
		out.writeInt(isWin ? 1 : 0);
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
		userId = in.readInt();
		username = in.readString();
		color = in.readString();
		setStartingPosition(in.readString());
		score = in.readString();
		rating = in.readDouble();
		isNew = in.readInt() == 1;
		isWin = in.readInt() == 1;
	}
}
