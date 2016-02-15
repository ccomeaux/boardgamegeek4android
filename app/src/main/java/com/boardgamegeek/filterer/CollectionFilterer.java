package com.boardgamegeek.filterer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

public class CollectionFilterer {
	protected static final String DELIMITER = ":";
	protected Context context;
	private int type;

	public CollectionFilterer() {
	}

	public CollectionFilterer(@NonNull Context context) {
		this.context = context;
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
}
