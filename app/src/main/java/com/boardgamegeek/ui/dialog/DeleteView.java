package com.boardgamegeek.ui.dialog;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;

import com.boardgamegeek.R;
import com.boardgamegeek.interfaces.CollectionView;
import com.boardgamegeek.provider.BggContract.CollectionViews;

public class DeleteView {
	public static void createDialog(final Context context, final CollectionView view) {

		final ContentResolver cr = context.getContentResolver();
		final Cursor cursor = cr.query(CollectionViews.CONTENT_URI, new String[] { CollectionViews._ID,
			CollectionViews.NAME }, null, null, null);

		new AlertDialog.Builder(context).setTitle(R.string.title_delete_view)
			.setCursor(cursor, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, final int which) {
					new AlertDialog.Builder(context).setTitle(R.string.are_you_sure_title)
						.setMessage(R.string.are_you_sure_delete_collection_view).setCancelable(false)
						.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								cursor.moveToPosition(which);
								long filterId = cursor.getLong(0);
								deleteFilters(cr, filterId);
							}
						}).setNegativeButton(R.string.no, null).create().show();
				}

				private void deleteFilters(final ContentResolver cr, long viewId) {
					int count = cr.delete(CollectionViews.buildViewUri(viewId), null, null);
					if (count == 1) {
						view.deleteView(viewId);
					}
				}
			}, CollectionViews.NAME).create().show();
	}
}
