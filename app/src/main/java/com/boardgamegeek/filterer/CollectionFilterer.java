package com.boardgamegeek.filterer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.TextUtils;

import com.boardgamegeek.util.StringUtils;

public abstract class CollectionFilterer {
	protected static final String DELIMITER = ":";
	protected final Context context;

	public CollectionFilterer(@NonNull Context context) {
		this.context = context;
	}

	public abstract void setData(@NonNull String data);

	@StringRes
	public abstract int getTypeResourceId();

	public int getType() {
		return StringUtils.parseInt(context.getString(getTypeResourceId(), CollectionFiltererFactory.TYPE_UNKNOWN));
	}

	public abstract String getDisplayText();

	public String[] getColumns() {
		return null;
	}

	public abstract String getSelection();

	public abstract String[] getSelectionArgs();

	public String getHaving() {
		return null;
	}

	public abstract String flatten();

	public boolean isValid() {
		return !TextUtils.isEmpty(getDisplayText()) &&
			(!TextUtils.isEmpty(getSelection()) || !TextUtils.isEmpty(getHaving()));
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
