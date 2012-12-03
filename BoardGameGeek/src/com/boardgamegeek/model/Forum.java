package com.boardgamegeek.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Forum implements Parcelable {

	public String id;
	public String title;
	public int numberOfThreads;
	public long lastPostDate;

	public Forum() {
	}

	public Forum(Parcel in) {
		id = in.readString();
		title = in.readString();
		numberOfThreads = in.readInt();
		lastPostDate = in.readLong();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(id);
		dest.writeString(title);
		dest.writeInt(numberOfThreads);
		dest.writeLong(lastPostDate);
	}

	public static final Parcelable.Creator<Forum> CREATOR = new Parcelable.Creator<Forum>() {
		public Forum createFromParcel(Parcel in) {
			return new Forum(in);
		}

		public Forum[] newArray(int size) {
			return new Forum[size];
		}
	};
}
