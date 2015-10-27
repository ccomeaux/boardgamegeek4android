package com.boardgamegeek.filterer;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

	public void displayText(String displayText) {
		this.displayText = displayText;
	}

	public void selection(String selection) {
		this.selection = selection;
	}

	public void selectionArgs(String... selectionArgs) {
		this.selectionArgs = selectionArgs;
	}

	public boolean isValid() {
		return !TextUtils.isEmpty(displayText) && !TextUtils.isEmpty(selection);
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
		out.writeString(displayText);
		out.writeString(selection);
		out.writeStringArray(selectionArgs);
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
		displayText = in.readString();
		selection = in.readString();
		selectionArgs = in.createStringArray();
	}

	public String flatten() {
		return "";
	}
}
