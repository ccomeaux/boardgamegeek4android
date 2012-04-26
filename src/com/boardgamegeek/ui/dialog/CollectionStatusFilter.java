package com.boardgamegeek.ui.dialog;

import android.app.AlertDialog;
import android.content.DialogInterface;

import com.boardgamegeek.R;
import com.boardgamegeek.data.CollectionFilterData;
import com.boardgamegeek.data.CollectionStatusFilterData;
import com.boardgamegeek.ui.CollectionActivity;

public class CollectionStatusFilter {

	private String[] mStatusEntries;
	private boolean[] mSelected;

	public void createDialog(final CollectionActivity activity, CollectionStatusFilterData filter) {
		init(activity, filter);

		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(R.string.menu_collection_status);
		builder.setMultiChoiceItems(mStatusEntries, mSelected, new DialogInterface.OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				mSelected[which] = isChecked;
			}
		});
		builder.setNeutralButton(R.string.or, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				CollectionFilterData filter = new CollectionStatusFilterData(activity, mSelected, true);
				activity.addFilter(filter);
			}
		}).setPositiveButton(R.string.and, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				CollectionFilterData filter = new CollectionStatusFilterData(activity, mSelected, false);
				activity.addFilter(filter);
			}
		}).setNegativeButton(R.string.clear, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				CollectionFilterData filter = new CollectionFilterData().id(R.id.menu_collection_status);
				activity.removeFilter(filter);
			}
		});

		AlertDialog alert = builder.create();
		alert.show();
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
