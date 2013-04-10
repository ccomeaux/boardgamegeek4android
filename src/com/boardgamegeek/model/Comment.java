package com.boardgamegeek.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Comment implements Parcelable {
	public String Username;
	public String Rating;
	public String Value;

	public Comment() {
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(Username);
		dest.writeString(Rating);
		dest.writeString(Value);
	}

	public static final Parcelable.Creator<Comment> CREATOR = new Parcelable.Creator<Comment>() {
		public Comment createFromParcel(Parcel in) {
			return new Comment(in);
		}

		public Comment[] newArray(int size) {
			return new Comment[size];
		}
	};

	private Comment(Parcel in) {
		Username = in.readString();
		Rating = in.readString();
		Value = in.readString();
	}
}
