package com.boardgamegeek.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class CollectionFilterData implements Parcelable {
	private String displayText;
	private String selection;
	private String[] selectionArgs = {};
	private int id;

	public CollectionFilterData() {
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

	public CollectionFilterData id(int id) {
		this.id = id;
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

	public static final Parcelable.Creator<CollectionFilterData> CREATOR = new Parcelable.Creator<CollectionFilterData>() {
		public CollectionFilterData createFromParcel(Parcel in) {
			return new CollectionFilterData(in);
		}

		public CollectionFilterData[] newArray(int size) {
			return new CollectionFilterData[size];
		}
	};

	private CollectionFilterData(Parcel in) {
		displayText = in.readString();
		selection = in.readString();
		in.readStringArray(selectionArgs);
		id = in.readInt();
	}
}
