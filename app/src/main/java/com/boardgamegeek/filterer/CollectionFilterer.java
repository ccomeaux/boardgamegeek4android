package com.boardgamegeek.filterer;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

public class CollectionFilterer implements Parcelable {
	protected static final String DELIMITER = ":";
	protected Context context;
	private int type;

	public CollectionFilterer() {
	}

	public CollectionFilterer(@NonNull Context context) {
		this.context = context;
	}

	public CollectionFilterer(int type) {
		this.type = type;
	}

	public void setData(@NonNull String data) {
	}

	public int getType() {
		return -1;
	}

	public String getDisplayText() {
		return "";
	}

	public String getSelection() {
		return "";
	}

	public String[] getSelectionArgs() {
		return null;
	}

	public String flatten() {
		return "";
	}

	public boolean isValid() {
		return !TextUtils.isEmpty(getDisplayText()) && !TextUtils.isEmpty(getSelection());
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (o == null || !(o instanceof CollectionFilterer)) {
			return false;
		}
		CollectionFilterer other = (CollectionFilterer) o;
		return other.getType() == this.getType();
	}

	@Override
	public int hashCode() {
		return this.getType();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(@NonNull Parcel out, int flags) {
		out.writeInt(type);
		out.writeString(flatten());
	}

	public static final Parcelable.Creator<CollectionFilterer> CREATOR = new Parcelable.Creator<CollectionFilterer>() {
		@NonNull
		public CollectionFilterer createFromParcel(@NonNull Parcel in) {
			return new CollectionFilterer(in);
		}

		@NonNull
		public CollectionFilterer[] newArray(int size) {
			return new CollectionFilterer[size];
		}
	};

	private CollectionFilterer(@NonNull Parcel in) {
		type = in.readInt();
		setData(in.readString());
	}
}
