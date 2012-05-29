package com.boardgamegeek.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class CollectionFilterData implements Parcelable {
	private int type;
	private String displayText;
	private String selection;
	private String[] selectionArgs = {};

	public CollectionFilterData() {
	}

	public CollectionFilterData(int type) {
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

	public CollectionFilterData displayText(String displayText) {
		this.displayText = displayText;
		return this;
	}

	public CollectionFilterData selection(String selection) {
		this.selection = selection;
		return this;
	}

	public CollectionFilterData selectionArgs(String... selectionArgs) {
		this.selectionArgs = selectionArgs;
		return this;
	}

	public boolean isValid() {
		return !TextUtils.isEmpty(displayText) && !TextUtils.isEmpty(selection);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof CollectionFilterData)) {
			return false;
		}
		CollectionFilterData other = (CollectionFilterData) o;
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

	public static final Parcelable.Creator<CollectionFilterData> CREATOR = new Parcelable.Creator<CollectionFilterData>() {
		public CollectionFilterData createFromParcel(Parcel in) {
			return new CollectionFilterData(in);
		}

		public CollectionFilterData[] newArray(int size) {
			return new CollectionFilterData[size];
		}
	};

	private CollectionFilterData(Parcel in) {
		type = in.readInt();
		displayText = in.readString();
		selection = in.readString();
		selectionArgs = in.createStringArray();
	}

	public String flatten() {
		return "";
	}
}
