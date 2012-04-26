package com.boardgamegeek.data;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;

import com.boardgamegeek.R;

public class CollectionStatusFilterData extends CollectionFilterData {

	private boolean[] mSelected;
	private boolean mOr;

	public CollectionStatusFilterData(Context context, boolean[] selected, boolean or) {
		mSelected = selected;
		mOr = or;

		id(R.id.menu_collection_status);
		createDisplayText(context.getResources());
		createSelection(context.getResources());
	}

	private void createDisplayText(Resources r) {
		String[] entries = r.getStringArray(R.array.collection_status_filter_entries);
		String displayText = "";

		for (int i = 0; i < mSelected.length; i++) {
			if (mSelected[i]) {
				if (displayText.length() > 0) {
					displayText += " " + (mOr ? "|" : "&") + " ";
				}
				displayText += entries[i];
			}
		}

		displayText(displayText);
	}

	private void createSelection(Resources r) {
		String[] values = r.getStringArray(R.array.collection_status_filter_values);
		String selection = "";
		List<String> selectionArgs = new ArrayList<String>(mSelected.length);

		for (int i = 0; i < mSelected.length; i++) {
			if (mSelected[i]) {
				if (selection.length() > 0) {
					selection += " " + (mOr ? "OR" : "AND") + " ";
				}
				selection += values[i] + ">=?";
				selectionArgs.add("1");
			}
		}
		selection(selection);
		selectionArgs(selectionArgs.toArray(new String[selectionArgs.size()]));
	}

	public boolean[] getSelected() {
		return mSelected;
	}

	public boolean getOr() {
		return mOr;
	}
}
