package com.boardgamegeek.ui.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.ExpansionStatusFilterer;
import com.boardgamegeek.interfaces.CollectionView;

public class ExpansionStatusFilter {
	private int mSelected = 0;

	public void createDialog(final Context context, final CollectionView view, ExpansionStatusFilterer filter) {
		init(filter);
		new AlertDialog.Builder(context).setTitle(R.string.menu_expansion_status)
			.setSingleChoiceItems(R.array.expansion_status_filter, mSelected, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mSelected = which;
				}
			}).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (mSelected == 0) {
					view.removeFilter(new ExpansionStatusFilterer());
				} else {
					view.addFilter(new ExpansionStatusFilterer(context, mSelected));
				}
			}
		}).setNegativeButton(R.string.cancel, null).create().show();
	}

	private void init(ExpansionStatusFilterer filter) {
		if (filter == null) {
			mSelected = 0;
		} else {
			mSelected = filter.getSelected();
		}
	}
}
