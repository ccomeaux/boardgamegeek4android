package com.boardgamegeek.filterer;

import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.Collection;

public class CollectionNameFilter extends CollectionFilterer {
	private String filterText;
	private boolean startsWith;

	public CollectionNameFilter(Context context) {
		super(context);
	}

	public CollectionNameFilter(Context context, CharSequence filterText, boolean startsWith) {
		super(context);
		this.filterText = filterText.toString();
		this.startsWith = startsWith;
	}

	@Override
	public void setData(@NonNull String data) {
		int lastIndex = data.lastIndexOf(DELIMITER);
		if (lastIndex == -1) {
			filterText = data;
			startsWith = false;
		} else {
			filterText = data.substring(0, lastIndex);
			startsWith = data.substring(lastIndex).equals("1");
		}
	}

	@Override
	public int getTypeResourceId() {
		return R.string.collection_filter_type_collection_name;
	}

	@Override
	public String getDisplayText() {
		if (startsWith) return filterText + "*";
		return "*" + filterText + "*";
	}

	@Override
	public String getSelection() {
		return Collection.COLLECTION_NAME + " LIKE ?";
	}

	@Override
	public String[] getSelectionArgs() {
		if (startsWith) return new String[] { filterText + "%" };
		return new String[] { "%" + filterText + "%" };
	}

	@Override
	public String flatten() {
		return filterText + DELIMITER + (startsWith ? "1" : "0");
	}

	public String getFilterText() {
		return filterText;
	}

	public boolean startsWith() {
		return startsWith;
	}
}
