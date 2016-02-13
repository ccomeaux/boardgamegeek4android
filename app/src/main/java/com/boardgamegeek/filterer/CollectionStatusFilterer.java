package com.boardgamegeek.filterer;

import android.content.Context;
import android.support.annotation.NonNull;

import com.boardgamegeek.R;

import java.util.ArrayList;
import java.util.List;

public class CollectionStatusFilterer extends CollectionFilterer {
	private boolean[] selectedStatuses;
	private boolean shouldJoinWithOr;

	public CollectionStatusFilterer(Context context) {
		super(context);
	}

	public CollectionStatusFilterer(@NonNull Context context, boolean[] selectedStatuses, boolean shouldJoinWithOr) {
		super(context);
		this.selectedStatuses = selectedStatuses;
		this.shouldJoinWithOr = shouldJoinWithOr;
	}

	@Override
	public void setData(@NonNull String data) {
		String[] d = data.split(DELIMITER);
		shouldJoinWithOr = (d[0].equals("1"));
		selectedStatuses = new boolean[d.length - 1];
		for (int i = 0; i < d.length - 1; i++) {
			selectedStatuses[i] = (d[i + 1].equals("1"));
		}
	}

	@Override
	public int getType() {
		return CollectionFiltererFactory.TYPE_COLLECTION_STATUS;
	}

	@Override
	public String getDisplayText() {
		String[] entries = context.getResources().getStringArray(R.array.collection_status_filter_entries);
		String displayText = "";

		for (int i = 0; i < selectedStatuses.length; i++) {
			if (selectedStatuses[i]) {
				if (displayText.length() > 0) {
					displayText += " " + (shouldJoinWithOr ? "|" : "&") + " ";
				}
				displayText += entries[i];
			}
		}

		return displayText;
	}

	@Override
	public String getSelection() {
		String[] values = context.getResources().getStringArray(R.array.collection_status_filter_values);
		String selection = "";

		for (int i = 0; i < selectedStatuses.length; i++) {
			if (selectedStatuses[i]) {
				if (selection.length() > 0) {
					selection += " " + (shouldJoinWithOr ? "OR" : "AND") + " ";
				}
				selection += values[i] + "=?";
			}
		}
		return selection;
	}

	public boolean[] getSelectedStatuses() {
		return selectedStatuses;
	}

	@NonNull
	@Override
	public String flatten() {
		String s = (shouldJoinWithOr ? "1" : "0");
		for (boolean selected : selectedStatuses) {
			if (s.length() > 0) {
				s += DELIMITER;
			}
			s += (selected ? "1" : "0");
		}
		return s;
	}

	@Override
	public String[] getSelectionArgs() {
		List<String> selectionArgs = new ArrayList<>(selectedStatuses.length);

		for (int i = 0; i < selectedStatuses.length; i++) {
			if (selectedStatuses[i]) {
				selectionArgs.add("1");
			}
		}
		return selectionArgs.toArray(new String[selectionArgs.size()]);
	}
}
