package com.boardgamegeek.ui.dialog;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.database.Cursor;
import android.widget.Toast;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.CollectionFilters;
import com.boardgamegeek.ui.CollectionActivity;

public class DeleteFilters {
	public static void createDialog(final CollectionActivity activity) {

		final ContentResolver cr = activity.getContentResolver();
		final Cursor cursor = cr.query(CollectionFilters.CONTENT_URI, new String[] { CollectionFilters._ID,
				CollectionFilters.NAME }, null, null, null);

		AlertDialog.Builder builder = new AlertDialog.Builder(activity).setTitle(R.string.delete_filters).setCursor(
				cursor, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, final int which) {
						AlertDialog ad = new AlertDialog.Builder(activity).setTitle(R.string.are_you_sure_title)
								.setMessage(R.string.are_you_sure_delete_collection_filter).setCancelable(false)
								.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										cursor.moveToPosition(which);
										Long filterId = cursor.getLong(0);
										deleteFilters(cr, filterId);
									}
								}).setNegativeButton(R.string.no, null).create();
						ad.show();
					}

					private void deleteFilters(final ContentResolver cr, Long filterId) {
						int count = cr.delete(CollectionFilters.buildFilterUri(filterId), null, null);
						if (count == 1) {
							Toast.makeText(activity, R.string.msg_collection_filter_deleted, Toast.LENGTH_LONG).show();
						}
					}
				}, CollectionFilters.NAME);

		builder.create().show();
	}
}
