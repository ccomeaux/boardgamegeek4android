package com.boardgamegeek.ui.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;

import com.boardgamegeek.R;

import java.util.ArrayList;
import java.util.List;

import hugo.weaving.DebugLog;

public class CollectionStatusDialogFragment extends DialogFragment {
	private String[] availableStatuses;
	private String[] availableStatusValues;

	public interface CollectionStatusDialogListener {
		void onSelectStatuses(List<String> selectedStatuses);
	}

	private CollectionStatusDialogListener listener;
	private List<String> selectedStatuses;

	@NonNull
	public static CollectionStatusDialogFragment newInstance(CollectionStatusDialogListener listener) {
		CollectionStatusDialogFragment fragment = new CollectionStatusDialogFragment();
		fragment.listener = listener;
		return fragment;
	}

	@DebugLog
	public void setSelectedStatuses(List<String> selectedStatuses) {
		this.selectedStatuses = selectedStatuses;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		availableStatuses = getResources().getStringArray(R.array.collection_statuses);
		availableStatusValues = getResources().getStringArray(R.array.collection_status_values);
		final boolean[] checkedItems = new boolean[availableStatuses.length];
		for (int i = 0; i < availableStatuses.length; i++) {
			if (selectedStatuses.contains(availableStatuses[i])) {
				checkedItems[i] = true;
			}
		}

		AlertDialog.Builder builder = new Builder(getContext())
			.setTitle(R.string.menu_collection_status)
			.setMultiChoiceItems(availableStatuses, checkedItems, new OnMultiChoiceClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which, boolean isChecked) {
					checkedItems[which] = isChecked;
				}
			})
			.setPositiveButton(R.string.ok, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (listener != null) {
						List<String> statuses = new ArrayList<>();
						for (int i = 0; i < checkedItems.length; i++) {
							if (checkedItems[i]) {
								statuses.add(availableStatusValues[i]);
							}
						}
						listener.onSelectStatuses(statuses);
					}
				}
			})
			.setNegativeButton(R.string.cancel, null);
		return builder.create();
	}
}
