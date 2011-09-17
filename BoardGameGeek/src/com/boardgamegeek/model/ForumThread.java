package com.boardgamegeek.model;

import android.os.Parcel;
import android.os.Parcelable;

public class ForumThread implements Parcelable {

	public String id;
	public String subject;
	public String author;
	public int numarticles;
	public long postdate;
	public long lastpostdate;
	
	public ForumThread() {
	}
	
	public ForumThread(Parcel in) {
		id = in.readString();
		subject = in.readString();
		author = in.readString();
		numarticles = in.readInt();
		postdate = in.readLong();
		lastpostdate = in.readLong();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(id);
		dest.writeString(subject);
		dest.writeString(author);
		dest.writeInt(numarticles);
		dest.writeLong(postdate);
		dest.writeLong(lastpostdate);
	}

	public static final Parcelable.Creator<ForumThread> CREATOR = new Parcelable.Creator<ForumThread>() {
		public ForumThread createFromParcel(Parcel in) {
			return new ForumThread(in);
		}

		public ForumThread[] newArray(int size) {
			return new ForumThread[size];
		}
	};	
}
