package com.boardgamegeek.ui.dialog;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;

import com.boardgamegeek.R;
import com.boardgamegeek.data.CollectionFilter;
import com.boardgamegeek.ui.CollectionActivity;

public class CollectionStatusFilter {

	private String[] statusEntries;
	private String[] items;
	private boolean[] mSelected;

	public void createDialog(final CollectionActivity activity) {
		if (mSelected == null) {
			statusEntries = activity.getResources().getStringArray(R.array.collection_status_filter_entries);
			items = activity.getResources().getStringArray(R.array.collection_status_filter_values);
			mSelected = new boolean[items.length];
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(R.string.menu_collection_status);
		builder.setMultiChoiceItems(statusEntries, mSelected, new DialogInterface.OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				mSelected[which] = isChecked;
			}
		});
		builder.setNeutralButton("Or", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				CollectionFilter filter = createFilter("OR", "|");
				activity.addFilter(filter);
			}
		}).setPositiveButton("And", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				CollectionFilter filter = createFilter("AND", "&");
				activity.addFilter(filter);
			}
		}).setNegativeButton("Clear", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				CollectionFilter filter = new CollectionFilter().id(R.id.menu_collection_status);
				activity.removeFilter(filter);
			}
		});

		AlertDialog alert = builder.create();
		alert.show();
	}

	private CollectionFilter createFilter(String selectionConjunction, String nameDelimiter) {
		CollectionFilter filter = new CollectionFilter();
		String name = "";
		String selection = "";
		List<String> selectionArgs = new ArrayList<String>(mSelected.length);

		for (int i = 0; i < mSelected.length; i++) {
			if (mSelected[i]) {
				name += statusEntries[i] + " " + nameDelimiter + " ";
				selection += items[i] + ">=? " + selectionConjunction + " ";
				selectionArgs.add("1");
			}
		}

		// Remove the last trailing combiners
		name = name.substring(0, name.length() - (2 + nameDelimiter.length()));
		selection = selection.substring(0, selection.length() - (2 + selectionConjunction.length()));

		filter.name(name).selection(selection).selectionargs(selectionArgs.toArray(new String[selectionArgs.size()]))
				.id(R.id.menu_collection_status);
		return filter;
	}
}
