package com.boardgamegeek.data;

import android.os.Parcel;
import android.os.Parcelable;

public class CollectionFilter implements Parcelable {

	private String displayText;
	private String selection;
	private String[] selectionArgs = {};
	private int id;

	public CollectionFilter() {
	}

	public String getSelection() {
		return selection;
	}

	public String[] getSelectionArgs() {
		return selectionArgs;
	}

	public String getDisplayText() {
		return displayText;
	}

	public int getId() {
		return id;
	}

	public CollectionFilter name(String name) {
		this.displayText = name;
		return this;
	}

	public CollectionFilter selection(String selection) {
		this.selection = selection;
		return this;
	}

	public CollectionFilter selectionArgs(String... selectionArgs) {
		this.selectionArgs = selectionArgs;
		return this;
	}

	public CollectionFilter id(int id) {
		this.id = id;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof CollectionFilter)) {
			return false;
		}

		CollectionFilter other = (CollectionFilter) o;

		return other.getId() == this.getId();
	}

	@Override
	public int hashCode() {
		return this.getId();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(displayText);
		out.writeString(selection);
		out.writeStringArray(selectionArgs);
		out.writeInt(id);
	}

	public static final Parcelable.Creator<CollectionFilter> CREATOR = new Parcelable.Creator<CollectionFilter>() {
		public CollectionFilter createFromParcel(Parcel in) {
			return new CollectionFilter(in);
		}

		public CollectionFilter[] newArray(int size) {
			return new CollectionFilter[size];
		}
	};

	private CollectionFilter(Parcel in) {
		displayText = in.readString();
		selection = in.readString();
		in.readStringArray(selectionArgs);
		id = in.readInt();
	}
}
