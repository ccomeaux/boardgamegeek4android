package com.boardgamegeek.model;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.util.StringUtils;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root(name = "player")
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
		userid = player.userid;
		username = player.username;
		color = player.color;
		setStartingPosition(player.startposition);
		score = player.score;
		rating = player.rating;
		new_ = player.new_;
		win = player.win;
	}

	@Attribute
	public String username;

	@Attribute
	public int userid;

	@Attribute
	public String name;

	@Attribute
	public String startposition;

	@Attribute
	public String color;

	@Attribute
	public String score;

	@Attribute(name = "new")
	public int new_;

	@Attribute
	public double rating;

	@Attribute
	public int win;

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

	private int mSeat = SEAT_UNPARSED;

	public String getStartingPosition() {
		return startposition;
	}

	public void setStartingPosition(String value) {
		mSeat = SEAT_UNPARSED;
		startposition = value;
	}

	public int getSeat() {
		if (mSeat == SEAT_UNPARSED) {
			mSeat = StringUtils.parseInt(startposition, SEAT_UNKNOWN);
		}
		return mSeat;
	}

	public void setSeat(int value) {
		setStartingPosition(String.valueOf(value));
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
			&& (userid == p.userid)
			&& ((username == null && p.username == null) || (username != null && username.equals(p.username)))
			&& ((color == null && p.color == null) || (color != null && color.equals(p.color)))
			&& ((startposition == null && p.startposition == null) || (startposition != null && startposition.equals(p.startposition)))
			&& ((score == null && p.score == null) || (score != null && score.equals(p.score)))
			&& (rating == p.rating) && (new_ == p.new_) && (win == p.win);
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

	public String toLongDescription(Context context) {
		StringBuilder sb = new StringBuilder();
		if (getSeat() != SEAT_UNKNOWN) {
			sb.append(context.getString(R.string.player_description_starting_position_segment, getSeat()));
		}
		sb.append(name);
		if (!TextUtils.isEmpty(username)) {
			sb.append(context.getString(R.string.player_description_username_segment, username));
		}
		if (New()) {
			sb.append(context.getString(R.string.player_description_new_segment));
		}
		if (!TextUtils.isEmpty(color)) {
			sb.append(context.getString(R.string.player_description_color_segment, color));
		}
		if (!TextUtils.isEmpty(score)) {
			sb.append(context.getString(R.string.player_description_score_segment, score));
		}
		if (Win()) {
			sb.append(context.getString(R.string.player_description_win_segment));
		}
		return sb.toString();
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
