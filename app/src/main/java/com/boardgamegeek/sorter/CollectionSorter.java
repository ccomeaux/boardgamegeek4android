package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.util.StringUtils;

public abstract class CollectionSorter extends Sorter {
	protected int subDescriptionId;

	public CollectionSorter(@NonNull Context context) {
		super(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		String description = super.getDescription();
		if (subDescriptionId > 0) {
			description += " - " + context.getString(subDescriptionId);
		}
		return description;
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
}
