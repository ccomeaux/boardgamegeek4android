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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.data.CollectionFilterData;
import com.boardgamegeek.data.CollectionView;
import com.boardgamegeek.provider.BggContract.CollectionViewFilters;
import com.boardgamegeek.provider.BggContract.CollectionViews;

public class SaveFilters {

	public static void createDialog(final Context context, final CollectionView view, String name,
		final int sortType, final List<CollectionFilterData> filters) {

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.dialog_save_filters, null);

		final EditText nameView = (EditText) layout.findViewById(R.id.name);
		nameView.setText(name);
		setDescription(filters, layout);

		AlertDialog.Builder builder = new AlertDialog.Builder(context).setTitle(R.string.menu_collection_view_save)
			.setView(layout).setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					final String name = nameView.getText().toString().trim();
					final ContentResolver resolver = context.getContentResolver();

					final int filterId = findFilterId(resolver, name);
					if (filterId > 0) {
						new AlertDialog.Builder(context).setTitle(R.string.title_collection_view_name_in_use)
							.setMessage(R.string.msg_collection_view_name_in_use)
							.setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									update(resolver, filterId, sortType, filters);
									updateDisplay(context, name);
								}
							}).setNegativeButton(R.string.create, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									insert(resolver, name, sortType, filters);
									updateDisplay(context, name);
								}
							}).create().show();

					} else {
						insert(resolver, name, sortType, filters);
						updateDisplay(context, name);
					}
				}

				private int findFilterId(ContentResolver resolver, String name) {
					Cursor c = resolver.query(CollectionViews.CONTENT_URI, new String[] { BaseColumns._ID },
						CollectionViews.NAME + "=?", new String[] { name }, null);
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

				private void insert(ContentResolver resolver, String name, int sortType,
					final List<CollectionFilterData> filters) {
					ContentValues values = new ContentValues();
					values.put(CollectionViews.NAME, name);
					values.put(CollectionViews.STARRED, false);
					values.put(CollectionViews.SORT_TYPE, sortType);
					Uri filterUri = resolver.insert(CollectionViews.CONTENT_URI, values);
					int filterId = CollectionViews.getViewId(filterUri);
					Uri uri = CollectionViews.buildViewFilterUri(filterId);
					insertDetails(resolver, uri, filters);
				}

				private void update(ContentResolver resolver, int viewId, int sortType,
					final List<CollectionFilterData> filters) {
					Uri uri = CollectionViews.buildViewFilterUri(viewId);
					resolver.delete(uri, null, null);
					insertDetails(resolver, uri, filters);
				}

				private void insertDetails(ContentResolver resolver, Uri viewFiltersUri,
					final List<CollectionFilterData> filters) {
					List<ContentValues> cvs = new ArrayList<ContentValues>(filters.size());
					for (CollectionFilterData filter : filters) {
						ContentValues cv = new ContentValues();
						cv.put(CollectionViewFilters.TYPE, filter.getType());
						cv.put(CollectionViewFilters.DATA, filter.flatten());
						cvs.add(cv);
					}
					resolver.bulkInsert(viewFiltersUri, cvs.toArray(new ContentValues[cvs.size()]));
				}

				private void updateDisplay(final Context context, String name) {
					Toast.makeText(context, R.string.msg_saved, Toast.LENGTH_SHORT).show();
					view.setViewName(name);
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
