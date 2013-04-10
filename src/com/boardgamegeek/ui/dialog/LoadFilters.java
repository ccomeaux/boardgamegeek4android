package com.boardgamegeek.ui.dialog;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;

import com.boardgamegeek.R;
import com.boardgamegeek.data.CollectionFilterData;
import com.boardgamegeek.data.CollectionFilterDataFactory;
import com.boardgamegeek.data.CollectionView;
import com.boardgamegeek.provider.BggContract.CollectionViewFilters;
import com.boardgamegeek.provider.BggContract.CollectionViews;

public class LoadFilters {

	public static void createDialog(final Context context, final CollectionView view) {

		final ContentResolver resolver = context.getContentResolver();
		final Cursor cursor = resolver.query(CollectionViews.CONTENT_URI, new String[] { CollectionViews._ID,
			CollectionViews.NAME, CollectionViews.SORT_TYPE }, null, null, null);

		new AlertDialog.Builder(context).setTitle(R.string.menu_collection_view_load)
			.setCursor(cursor, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					cursor.moveToPosition(which);
					view.setSort(cursor.getInt(2));
					view.setFilterName(cursor.getString(1), false);
					createFilterList(resolver, cursor.getLong(0));
				}

				private void createFilterList(final ContentResolver resolver, long filterId) {
					Cursor cursor = resolver.query(CollectionViews.buildViewFilterUri(filterId), new String[] {
						CollectionViewFilters.TYPE, CollectionViewFilters.DATA }, null, null, null);
					if (cursor != null) {
						try {
							List<CollectionFilterData> filters = new ArrayList<CollectionFilterData>();
							while (cursor.moveToNext()) {
								filters.add(CollectionFilterDataFactory.create(context, cursor.getInt(0),
									cursor.getString(1)));
							}
							view.setFilters(filters);
						} finally {
							if (cursor != null && !cursor.isClosed()) {
								cursor.close();
							}
						}
					}
				}
			}, CollectionViews.NAME).create().show();
	}
}