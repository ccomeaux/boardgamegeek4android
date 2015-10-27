package com.boardgamegeek.filterer;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class CollectionFilterer implements Parcelable {
	protected static final String DELIMITER = ":";
	private int type;
	private String displayText;
	private String selection;
	private String[] selectionArgs = {};

	public CollectionFilterer() {
	}

	public CollectionFilterer(int type) {
		this.type = type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getType() {
		return type;
	}

	public String getDisplayText() {
		return displayText;
	}

	public String getSelection() {
		return selection;
	}

	public String[] getSelectionArgs() {
		return selectionArgs;
	}

	public CollectionFilterer displayText(String displayText) {
		this.displayText = displayText;
		return this;
	}

	public CollectionFilterer selection(String selection) {
		this.selection = selection;
		return this;
	}

	public CollectionFilterer selectionArgs(String... selectionArgs) {
		this.selectionArgs = selectionArgs;
		return this;
	}

	public boolean isValid() {
		return !TextUtils.isEmpty(displayText) && !TextUtils.isEmpty(selection);
	}

	@Override
	public boolean equals(Object o) {
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
	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(type);
		out.writeString(displayText);
		out.writeString(selection);
		out.writeStringArray(selectionArgs);
	}

	public static final Parcelable.Creator<CollectionFilterer> CREATOR = new Parcelable.Creator<CollectionFilterer>() {
		public CollectionFilterer createFromParcel(Parcel in) {
			return new CollectionFilterer(in);
		}

		public CollectionFilterer[] newArray(int size) {
			return new CollectionFilterer[size];
		}
	};

	private CollectionFilterer(Parcel in) {
		type = in.readInt();
		displayText = in.readString();
		selection = in.readString();
		selectionArgs = in.createStringArray();
	}

	public String flatten() {
		return "";
	}
}
