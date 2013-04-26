package com.boardgamegeek.model;

import android.os.Parcel;
import android.os.Parcelable;

public class BuddyGame implements Parcelable {
	public String Id;
	public String Name;
	public String Year;
	public String SortName;

	public BuddyGame() {
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(Id);
		dest.writeString(Name);
		dest.writeString(Year);
		dest.writeString(SortName);
	}

	public static final Parcelable.Creator<BuddyGame> CREATOR = new Parcelable.Creator<BuddyGame>() {
		public BuddyGame createFromParcel(Parcel in) {
			return new BuddyGame(in);
		}

		public BuddyGame[] newArray(int size) {
			return new BuddyGame[size];
		}
	};

	private BuddyGame(Parcel in) {
		Id = in.readString();
		Name = in.readString();
		Year = in.readString();
		SortName = in.readString();
	}
}
