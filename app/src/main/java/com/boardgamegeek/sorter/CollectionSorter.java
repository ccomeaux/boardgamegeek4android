package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.util.StringUtils;

public abstract class CollectionSorter extends Sorter {
	public CollectionSorter(@NonNull Context context) {
		super(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		String description = super.getDescription();
		if (getSubDescriptionId() > 0) {
			description += " - " + context.getString(getSubDescriptionId());
		}
		return description;
	}

	@StringRes
	protected int getSubDescriptionId() {
		return 0;
	}

	@Override
	public int getType() {
		return StringUtils.parseInt(context.getString(getTypeResource()), CollectionSorterFactory.TYPE_DEFAULT);
	}

	@StringRes
	protected abstract int getTypeResource();

	@Override
	protected String getDefaultSort() {
		return Collection.DEFAULT_SORT;
	}

	/**
	 * Gets the text to display on each row.
	 */
	public String getDisplayInfo(Cursor cursor) {
		return getHeaderText(cursor);
	}

	public long getTimestamp(Cursor cursor) {
		return 0;
	}
}
