package com.boardgamegeek.model;

import android.os.Parcel;
import android.os.Parcelable;

public class ThreadArticle implements Parcelable {

	public String username;
	public String subject;
	public String body;
	public long postDate;
	public long editDate;
	public int numberOfEdits;

	public ThreadArticle() {
	}

	public ThreadArticle(Parcel in) {
		username = in.readString();
		subject = in.readString();
		body = in.readString();
		postDate = in.readLong();
		editDate = in.readLong();
		numberOfEdits = in.readInt();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(username);
		dest.writeString(subject);
		dest.writeString(body);
		dest.writeLong(postDate);
		dest.writeLong(editDate);
		dest.writeInt(numberOfEdits);
	}

	public static final Parcelable.Creator<ThreadArticle> CREATOR = new Parcelable.Creator<ThreadArticle>() {
		public ThreadArticle createFromParcel(Parcel in) {
			return new ThreadArticle(in);
		}

		public ThreadArticle[] newArray(int size) {
			return new ThreadArticle[size];
		}
	};
}
