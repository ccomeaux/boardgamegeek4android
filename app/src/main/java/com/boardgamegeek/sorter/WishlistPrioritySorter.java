package com.boardgamegeek.sorter;

import android.content.Context;
import android.database.Cursor;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class WishlistPrioritySorter extends CollectionSorter {
	private final String[] priorityText;

	public WishlistPrioritySorter(Context context) {
		super(context);
		priorityText = context.getResources().getStringArray(R.array.wishlist_priority);
		orderByClause = getClause(Collection.STATUS_WISHLIST_PRIORITY, false);
		descriptionId = R.string.menu_collection_sort_wishlist_priority;
	}

	@Override
	public int getType() {
		return CollectionSorterFactory.TYPE_WISHLIST_PRIORITY;
	}

	@Override
	public String[] getColumns() {
		return new String[] { Collection.STATUS_WISHLIST_PRIORITY };
	}

	@Override
	public String getHeaderText(Cursor cursor) {
		int level = getInt(cursor, Collection.STATUS_WISHLIST_PRIORITY);
		if (level >= priorityText.length) {
			level = 0;
		}
		return priorityText[level];
	}

	@Override
	public String getDisplayInfo(Cursor cursor) {
		return getIntAsString(cursor, Collection.STATUS_WISHLIST_PRIORITY, "?", true) + " - " + getHeaderText(cursor);
	}
}
