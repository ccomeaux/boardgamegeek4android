package com.boardgamegeek.ui.dialog;

import android.app.AlertDialog;
import android.content.DialogInterface;

import com.boardgamegeek.R;
import com.boardgamegeek.data.CollectionStatusFilterData;
import com.boardgamegeek.ui.CollectionActivity;

public class CollectionStatusFilter {

	private String[] mStatusEntries;
	private boolean[] mSelected;

	public void createDialog(final CollectionActivity activity, CollectionStatusFilterData filter) {
		init(activity, filter);

		AlertDialog.Builder builder = new AlertDialog.Builder(activity).setTitle(R.string.menu_collection_status)
				.setMultiChoiceItems(mStatusEntries, mSelected, new DialogInterface.OnMultiChoiceClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which, boolean isChecked) {
						mSelected[which] = isChecked;
					}
				}).setNeutralButton(R.string.or, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						activity.addFilter(new CollectionStatusFilterData(activity, mSelected, true));
					}
				}).setPositiveButton(R.string.and, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						activity.addFilter(new CollectionStatusFilterData(activity, mSelected, false));
					}
				}).setNegativeButton(R.string.clear, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						activity.removeFilter(new CollectionStatusFilterData());
					}
				});

		builder.create().show();
	}

	private void init(final CollectionActivity activity, CollectionStatusFilterData filter) {
		mStatusEntries = activity.getResources().getStringArray(R.array.collection_status_filter_entries);
		if (filter == null) {
			mSelected = new boolean[mStatusEntries.length];
		} else {
			mSelected = filter.getSelected();
		}
	}
}
