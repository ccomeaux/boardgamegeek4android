package com.boardgamegeek.ui.dialog;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.database.Cursor;

import com.boardgamegeek.R;
import com.boardgamegeek.data.CollectionFilterData;
import com.boardgamegeek.data.CollectionFilterDataFactory;
import com.boardgamegeek.provider.BggContract.CollectionFilterDetails;
import com.boardgamegeek.provider.BggContract.CollectionFilters;
import com.boardgamegeek.ui.CollectionActivity;

public class LoadFilters {

	public static void createDialog(final CollectionActivity activity) {

		final ContentResolver cr = activity.getContentResolver();
		final Cursor cursor = cr.query(CollectionFilters.CONTENT_URI, new String[] { CollectionFilters._ID,
				CollectionFilters.NAME }, null, null, null);

		AlertDialog.Builder builder = new AlertDialog.Builder(activity).setTitle(R.string.load_filters).setCursor(
				cursor, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						cursor.moveToPosition(which);
						activity.setFilterName(cursor.getString(1));
						createFilterList(cr, cursor.getLong(0));
					}

					private void createFilterList(final ContentResolver cr, Long filterId) {
						Cursor c = cr.query(CollectionFilters.buildFilterDetailUri(filterId), new String[] {
								CollectionFilterDetails.TYPE, CollectionFilterDetails.DATA }, null, null, null);
						if (c != null) {
							try {
								List<CollectionFilterData> filters = new ArrayList<CollectionFilterData>();
								while (c.moveToNext()) {
									filters.add(CollectionFilterDataFactory.create(activity, c.getInt(0),
											c.getString(1)));
								}
								activity.setFilters(filters);
							} finally {
								if (c != null && !c.isClosed()) {
									c.close();
								}
							}
						}
					}
				}, CollectionFilters.NAME);

		builder.create().show();
	}
}