package com.boardgamegeek.model;

import android.os.Parcel;
import android.os.Parcelable;

public class HotGame implements Parcelable {
	public int Id;
	public String Name;
	public int Rank;
	public String ThumbnailUrl;
	public int YearPublished;

	public HotGame() {
	}

	public HotGame(Parcel in) {
		Id = in.readInt();
		Name = in.readString();
		Rank = in.readInt();
		ThumbnailUrl = in.readString();
		YearPublished = in.readInt();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(Id);
		dest.writeString(Name);
		dest.writeInt(Rank);
		dest.writeString(ThumbnailUrl);
		dest.writeInt(YearPublished);
	}

	public static final Parcelable.Creator<HotGame> CREATOR = new Parcelable.Creator<HotGame>() {
		public HotGame createFromParcel(Parcel in) {
			return new HotGame(in);
		}

		public HotGame[] newArray(int size) {
			return new HotGame[size];
		}
	};
}
