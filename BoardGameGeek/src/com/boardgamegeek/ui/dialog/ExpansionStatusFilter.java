package com.boardgamegeek.ui.dialog;

import android.app.AlertDialog;
import android.content.DialogInterface;

import com.boardgamegeek.R;
import com.boardgamegeek.data.ExpansionStatusFilterData;
import com.boardgamegeek.ui.CollectionActivity;

public class ExpansionStatusFilter {
	private int mSelected = 0;

	public void createDialog(final CollectionActivity activity, ExpansionStatusFilterData filter) {
		init(filter);
		AlertDialog.Builder builder = new AlertDialog.Builder(activity)
				.setTitle(R.string.menu_expansion_status)
				.setSingleChoiceItems(R.array.expansion_status_filter, mSelected,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								mSelected = which;
							}
						}).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						activity.addFilter(new ExpansionStatusFilterData(activity, mSelected));
					}
				}).setNegativeButton(R.string.cancel, null);
		builder.create().show();
	}

	private void init(ExpansionStatusFilterData filter) {
		if (filter == null) {
			mSelected = 0;
		} else {
			mSelected = filter.getSelected();
		}
	}
}
