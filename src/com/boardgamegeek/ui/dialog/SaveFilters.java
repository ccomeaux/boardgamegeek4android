package com.boardgamegeek.ui.dialog;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.data.CollectionFilterData;
import com.boardgamegeek.provider.BggContract.CollectionFilterDetails;
import com.boardgamegeek.provider.BggContract.CollectionFilters;
import com.boardgamegeek.ui.CollectionActivity;

public class SaveFilters {

	public static void createDialog(final CollectionActivity activity, final List<CollectionFilterData> filters) {

		LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.dialog_save_filters,
				(ViewGroup) activity.findViewById(R.id.layout_root));

		final EditText nameView = (EditText) layout.findViewById(R.id.name);
		setDescription(filters, layout);

		AlertDialog.Builder builder = new AlertDialog.Builder(activity).setTitle(R.string.save_filters).setView(layout)
				.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						String name = nameView.getText().toString().trim();
						ContentResolver resolver = activity.getContentResolver();
						// TODO: check name for uniqueness?
						insert(name, filters, resolver);
						Toast.makeText(activity, R.string.msg_saved, Toast.LENGTH_SHORT).show();
						activity.setFilterName(name);
					}

					private void insert(String name, final List<CollectionFilterData> filters, ContentResolver resolver) {
						ContentValues values = new ContentValues();
						values.put(CollectionFilters.NAME, name);
						values.put(CollectionFilters.STARRED, false);
						Uri filterUri = resolver.insert(CollectionFilters.CONTENT_URI, values);
						int filterId = CollectionFilters.getFilterId(filterUri);

						List<ContentValues> cvs = new ArrayList<ContentValues>(filters.size());
						for (CollectionFilterData filter : filters) {
							ContentValues cv = new ContentValues();
							cv.put(CollectionFilterDetails.TYPE, filter.getType());
							cv.put(CollectionFilterDetails.DATA, filter.flatten());
							cvs.add(cv);
						}
						resolver.bulkInsert(CollectionFilters.buildFilterDetailUri(filterId),
								cvs.toArray(new ContentValues[cvs.size()]));
					}
				}).setNegativeButton(R.string.cancel, null).setCancelable(true);

		final AlertDialog dialog = builder.create();
		enableSaveButton(dialog, nameView);
		dialog.show();
	}

	private static void enableSaveButton(final AlertDialog dialog, final EditText nameView) {
		nameView.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(
						nameView.getText().toString().trim().length() > 0);
				return false;
			}
		});
	}

	private static void setDescription(List<CollectionFilterData> filters, View layout) {
		TextView description = (TextView) layout.findViewById(R.id.description);
		StringBuilder text = new StringBuilder();
		for (CollectionFilterData filter : filters) {
			if (text.length() > 0) {
				text.append("\n");
			}
			text.append(filter.getDisplayText());
		}
		description.setText(text.toString());
	}
}
