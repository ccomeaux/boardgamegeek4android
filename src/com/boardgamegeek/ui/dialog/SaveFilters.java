package com.boardgamegeek.ui.dialog;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
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

	public static void createDialog(final CollectionActivity activity, String name,
			final List<CollectionFilterData> filters) {

		LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.dialog_save_filters,
				(ViewGroup) activity.findViewById(R.id.layout_root));

		final EditText nameView = (EditText) layout.findViewById(R.id.name);
		nameView.setText(name);
		setDescription(filters, layout);

		AlertDialog.Builder builder = new AlertDialog.Builder(activity).setTitle(R.string.menu_collection_filter_save)
				.setView(layout).setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						final String name = nameView.getText().toString().trim();
						final ContentResolver resolver = activity.getContentResolver();

						final int filterId = findFilterId(resolver, name);
						if (filterId > 0) {
							new AlertDialog.Builder(activity).setTitle(R.string.title_collection_filter_name_in_use)
									.setMessage(R.string.msg_collection_filter_name_in_use)
									.setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											update(resolver, filterId, filters);
											updateDisplay(activity, name);
										}
									}).setNegativeButton(R.string.create, new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
											insert(resolver, name, filters);
											updateDisplay(activity, name);
										}
									}).create().show();

						} else {
							insert(resolver, name, filters);
							updateDisplay(activity, name);
						}
					}

					private int findFilterId(ContentResolver resolver, String name) {
						Cursor c = resolver.query(CollectionFilters.CONTENT_URI, new String[] { BaseColumns._ID },
								CollectionFilters.NAME + "=?", new String[] { name }, null);
						if (c != null) {
							try {
								if (c.moveToFirst()) {
									return c.getInt(0);
								}
							} finally {
								c.close();
							}
						}
						return -1;
					}

					private void insert(ContentResolver resolver, String name, final List<CollectionFilterData> filters) {
						ContentValues values = new ContentValues();
						values.put(CollectionFilters.NAME, name);
						values.put(CollectionFilters.STARRED, false);
						Uri filterUri = resolver.insert(CollectionFilters.CONTENT_URI, values);
						int filterId = CollectionFilters.getFilterId(filterUri);
						Uri uri = CollectionFilters.buildFilterDetailUri(filterId);
						insertDetails(resolver, uri, filters);
					}

					private void update(ContentResolver resolver, int filterId, final List<CollectionFilterData> filters) {
						Uri uri = CollectionFilters.buildFilterDetailUri(filterId);
						resolver.delete(uri, null, null);
						insertDetails(resolver, uri, filters);
					}

					private void insertDetails(ContentResolver resolver, Uri filterUri,
							final List<CollectionFilterData> filters) {
						List<ContentValues> cvs = new ArrayList<ContentValues>(filters.size());
						for (CollectionFilterData filter : filters) {
							ContentValues cv = new ContentValues();
							cv.put(CollectionFilterDetails.TYPE, filter.getType());
							cv.put(CollectionFilterDetails.DATA, filter.flatten());
							cvs.add(cv);
						}
						resolver.bulkInsert(filterUri, cvs.toArray(new ContentValues[cvs.size()]));
					}

					private void updateDisplay(final CollectionActivity activity, String name) {
						Toast.makeText(activity, R.string.msg_saved, Toast.LENGTH_SHORT).show();
						activity.setFilterName(name, false);
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
