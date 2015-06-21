package com.boardgamegeek.ui.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionStatusFilterer;
import com.boardgamegeek.interfaces.CollectionView;

public class CollectionStatusFilter {

	private String[] mStatusEntries;
	private boolean[] mSelected;

	public void createDialog(final Context context, final CollectionView view, CollectionStatusFilterer filter) {
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
				view.addFilter(new CollectionStatusFilterer(context, mSelected, true));
			}
		}).setPositiveButton(R.string.and, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				view.addFilter(new CollectionStatusFilterer(context, mSelected, false));
			}
		}).setNeutralButton(R.string.clear, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				view.removeFilter(new CollectionStatusFilterer());
			}
		}).create().show();
	}

	private void init(final Context context, CollectionStatusFilterer filter) {
		mStatusEntries = context.getResources().getStringArray(R.array.collection_status_filter_entries);
		if (filter == null) {
			mSelected = new boolean[mStatusEntries.length];
		} else {
			mSelected = filter.getSelected();
		}
	}
}
