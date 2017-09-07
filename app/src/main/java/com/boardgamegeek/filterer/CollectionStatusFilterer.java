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
	public int getTypeResourceId() {
		return R.string.collection_filter_type_collection_status;
	}

	@Override
	public String getDisplayText() {
		String[] entries = context.getResources().getStringArray(R.array.collection_status_filter_entries);
		StringBuilder displayText = new StringBuilder();

		for (int i = 0; i < selectedStatuses.length; i++) {
			if (selectedStatuses[i]) {
				if (displayText.length() > 0) {
					displayText.append(" ").append(shouldJoinWithOr ? "|" : "&").append(" ");
				}
				displayText.append(entries[i]);
			}
		}

		return displayText.toString();
	}

	@Override
	public String getSelection() {
		String[] values = context.getResources().getStringArray(R.array.collection_status_filter_values);
		StringBuilder selection = new StringBuilder();

		for (int i = 0; i < selectedStatuses.length; i++) {
			if (selectedStatuses[i]) {
				if (selection.length() > 0) {
					selection.append(" ").append((shouldJoinWithOr ? "OR" : "AND")).append(" ");
				}
				selection.append(values[i]).append("=?");
			}
		}
		return selection.toString();
	}

	public boolean[] getSelectedStatuses() {
		return selectedStatuses;
	}

	@NonNull
	@Override
	public String flatten() {
		StringBuilder sb = new StringBuilder(shouldJoinWithOr ? "1" : "0");
		for (boolean selected : selectedStatuses) {
			if (sb.length() > 0) sb.append(DELIMITER);
			sb.append(selected ? "1" : "0");
		}
		return sb.toString();
	}

	@Override
	public String[] getSelectionArgs() {
		List<String> selectionArgs = new ArrayList<>(selectedStatuses.length);

		for (boolean selectedStatus : selectedStatuses) {
			if (selectedStatus) {
				selectionArgs.add("1");
			}
		}
		return selectionArgs.toArray(new String[selectionArgs.size()]);
	}
}
