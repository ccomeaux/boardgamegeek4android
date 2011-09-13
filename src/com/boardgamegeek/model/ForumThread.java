package com.boardgamegeek.model;

import android.os.Parcel;
import android.os.Parcelable;

public class ForumThread implements Parcelable {

	public String id;
	public String subject;
	public String author;
	public String numarticles;
	public String postdate;
	public String lastpostdate;
	
	public ForumThread() {
	}
	
	public ForumThread(Parcel in) {
		id = in.readString();
		subject = in.readString();
		author = in.readString();
		numarticles = in.readString();
		postdate = in.readString();
		lastpostdate = in.readString();
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
		dest.writeString(numarticles);
		dest.writeString(postdate);
		dest.writeString(lastpostdate);
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
