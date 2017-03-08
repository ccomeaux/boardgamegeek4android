package com.boardgamegeek.ui.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.filterer.ExpansionStatusFilterer;

public class ExpansionStatusFilterDialog implements CollectionFilterDialog {
	private int selectedSubtype = 0;

	public void createDialog(final Context context, final OnFilterChangedListener listener, CollectionFilterer filter) {
		init((ExpansionStatusFilterer) filter);
		new AlertDialog.Builder(context)
			.setTitle(R.string.menu_expansion_status)
			.setSingleChoiceItems(R.array.expansion_status_filter, selectedSubtype, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					selectedSubtype = which;
				}
			})
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (listener != null) {
						if (selectedSubtype == 0) {
							listener.removeFilter(getType(context));
						} else {
							listener.addFilter(new ExpansionStatusFilterer(context, selectedSubtype));
						}
					}
				}
			})
			.setNegativeButton(R.string.cancel, null).create().show();
	}

	@Override
	public int getType(Context context) {
		return new ExpansionStatusFilterer(context).getType();
	}

	private void init(ExpansionStatusFilterer filter) {
		if (filter == null) {
			selectedSubtype = 0;
		} else {
			selectedSubtype = filter.getSelectedSubtype();
		}
	}
}
