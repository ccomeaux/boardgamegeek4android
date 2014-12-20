package com.boardgamegeek.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.boardgamegeek.R;
import com.boardgamegeek.data.CollectionStatusFilterData;
import com.boardgamegeek.data.CollectionView;

public class CollectionStatusFilter {

	private String[] mStatusEntries;
	private boolean[] mSelected;

	public void createDialog(final Context context, final CollectionView view, CollectionStatusFilterData filter) {
		init(context, filter);

		new AlertDialog.Builder(context).setTitle(R.string.menu_collection_status)
			.setMultiChoiceItems(mStatusEntries, mSelected, new DialogInterface.OnMultiChoiceClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which, boolean isChecked) {
					mSelected[which] = isChecked;
				}
			}).setNegativeButton(R.string.or, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					view.addFilter(new CollectionStatusFilterData(context, mSelected, true));
				}
			}).setPositiveButton(R.string.and, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					view.addFilter(new CollectionStatusFilterData(context, mSelected, false));
				}
			}).setNeutralButton(R.string.clear, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					view.removeFilter(new CollectionStatusFilterData());
				}
			}).create().show();
	}

	private void init(final Context context, CollectionStatusFilterData filter) {
		mStatusEntries = context.getResources().getStringArray(R.array.collection_status_filter_entries);
		if (filter == null) {
			mSelected = new boolean[mStatusEntries.length];
		} else {
			mSelected = filter.getSelected();
		}
	}
}
