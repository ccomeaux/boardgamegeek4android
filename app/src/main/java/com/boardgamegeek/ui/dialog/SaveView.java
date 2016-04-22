package com.boardgamegeek.ui.dialog;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.filterer.CollectionFilterer;
import com.boardgamegeek.interfaces.CollectionView;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.CollectionViewFilters;
import com.boardgamegeek.provider.BggContract.CollectionViews;
import com.boardgamegeek.sorter.Sorter;
import com.boardgamegeek.util.PreferencesUtils;
import com.boardgamegeek.util.ResolverUtils;
import com.boardgamegeek.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class SaveView {

	public static void createDialog(final Context context, final CollectionView view, String name, final Sorter sort, final List<CollectionFilterer> filters) {

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		@SuppressLint("InflateParams") View layout = inflater.inflate(R.layout.dialog_save_view, null);

		final EditText nameView = (EditText) layout.findViewById(R.id.name);
		final CheckBox defaultView = (CheckBox) layout.findViewById(R.id.default_view);

		nameView.setText(name);
		if (!TextUtils.isEmpty(name)) {
			nameView.setSelection(0, name.length());
		}
		if (findViewId(context.getContentResolver(), name) == PreferencesUtils.getViewDefaultId(context)) {
			defaultView.setChecked(true);
		}
		setDescription(layout, sort, filters);

		AlertDialog.Builder builder = new AlertDialog.Builder(context).setTitle(R.string.title_save_view)
			.setView(layout).setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					final String name = nameView.getText().toString().trim();
					final boolean isDefault = defaultView.isChecked();

					final ContentResolver resolver = context.getContentResolver();

					final long viewId = findViewId(resolver, name);
					if (viewId > 0) {
						new AlertDialog.Builder(context).setTitle(R.string.title_collection_view_name_in_use)
							.setMessage(R.string.msg_collection_view_name_in_use)
							.setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									update(resolver, viewId, sort.getType(), filters);
									setDefault(viewId, isDefault);
									view.createView(viewId, name);
								}
							}).setNegativeButton(R.string.create, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								long id = insert(resolver, name, sort.getType(), filters);
								setDefault(id, isDefault);
								view.createView(id, name);
							}
						}).create().show();

					} else {
						long id = insert(resolver, name, sort.getType(), filters);
						setDefault(id, isDefault);
						view.createView(id, name);
					}
				}

				private void setDefault(long viewId, boolean isDefault) {
					if (isDefault) {
						// TODO: prompt the user if replacing a default
						PreferencesUtils.putViewDefaultId(context, viewId);
					} else {
						if (viewId == PreferencesUtils.getViewDefaultId(context)) {
							PreferencesUtils.removeViewDefaultId(context);
						}
					}
				}

				private long insert(ContentResolver resolver, String name, int sortType, final List<CollectionFilterer> filters) {
					ContentValues values = new ContentValues();
					values.put(CollectionViews.NAME, name);
					values.put(CollectionViews.STARRED, false);
					values.put(CollectionViews.SORT_TYPE, sortType);
					Uri filterUri = resolver.insert(CollectionViews.CONTENT_URI, values);

					int filterId = CollectionViews.getViewId(filterUri);
					Uri uri = CollectionViews.buildViewFilterUri(filterId);
					insertDetails(resolver, uri, filters);
					if (filterUri == null) {
						return BggContract.INVALID_ID;
					}
					return StringUtils.parseLong(filterUri.getLastPathSegment());
				}

				private void update(ContentResolver resolver, long viewId, int sortType, final List<CollectionFilterer> filters) {
					ContentValues values = new ContentValues();
					values.put(CollectionViews.SORT_TYPE, sortType);
					resolver.update(CollectionViews.buildViewUri(viewId), values, null, null);

					Uri uri = CollectionViews.buildViewFilterUri(viewId);
					resolver.delete(uri, null, null);
					insertDetails(resolver, uri, filters);
				}

				private void insertDetails(ContentResolver resolver, Uri viewFiltersUri, final List<CollectionFilterer> filters) {
					List<ContentValues> cvs = new ArrayList<>(filters.size());
					for (CollectionFilterer filter : filters) {
						if (filter != null) {
							ContentValues cv = new ContentValues();
							cv.put(CollectionViewFilters.TYPE, filter.getType());
							cv.put(CollectionViewFilters.DATA, filter.flatten());
							cvs.add(cv);
						}
					}
					if (cvs.size() > 0) {
						resolver.bulkInsert(viewFiltersUri, cvs.toArray(new ContentValues[cvs.size()]));
					}
				}
			}).setNegativeButton(R.string.cancel, null).setCancelable(true);
		final AlertDialog dialog = builder.create();
		dialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		dialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialogInterface) {
				enableSaveButton(dialog, nameView);
			}
		});
		dialog.show();
	}

	private static long findViewId(ContentResolver resolver, String name) {
		return ResolverUtils.queryLong(resolver, CollectionViews.CONTENT_URI, CollectionViews._ID, 0,
			CollectionViews.NAME + "=?", new String[] { name });
	}

	private static void enableSaveButton(final AlertDialog dialog, final EditText nameView) {
		dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(nameView.getText().toString().trim().length() > 0);
		nameView.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(
					nameView.getText().toString().trim().length() > 0);
			}
		});
	}

	private static void setDescription(View layout, Sorter sort, List<CollectionFilterer> filters) {
		TextView description = (TextView) layout.findViewById(R.id.description);
		StringBuilder text = new StringBuilder();
		for (CollectionFilterer filter : filters) {
			if (filter != null) {
				if (text.length() > 0) {
					text.append("\n");
				}
				text.append(filter.getDisplayText());
			}
		}
		if (text.length() > 0) {
			text.append("\n");
		}
		text.append(sort.getDescription());
		description.setText(text.toString());
	}
}
