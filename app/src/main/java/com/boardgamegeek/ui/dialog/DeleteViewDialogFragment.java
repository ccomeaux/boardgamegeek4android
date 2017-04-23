package com.boardgamegeek.ui.dialog;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog.Builder;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract.CollectionViews;

public class DeleteViewDialogFragment extends DialogFragment {
	private Context context;
	private OnViewDeletedListener listener;

	public static DeleteViewDialogFragment newInstance(Context context) {
		DeleteViewDialogFragment dialogFragment = new DeleteViewDialogFragment();
		dialogFragment.context = context;
		return dialogFragment;
	}

	public interface OnViewDeletedListener {
		void onDeleteRequested(long viewId);
	}

	public void setOnViewDeletedListener(OnViewDeletedListener listener) {
		this.listener = listener;
	}

	@Override
	@NonNull
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final ContentResolver resolver = context.getContentResolver();
		final Cursor cursor = resolver.query(CollectionViews.CONTENT_URI,
			new String[] { CollectionViews._ID, CollectionViews.NAME },
			null,
			null,
			null);

		return new Builder(context, R.style.Theme_bgglight_Dialog_Alert)
			.setTitle(R.string.title_delete_view)
			.setCursor(cursor, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, final int which) {
					new Builder(context)
						.setTitle(R.string.are_you_sure_title)
						.setMessage(R.string.are_you_sure_delete_collection_view)
						.setCancelable(true)
						.setPositiveButton(R.string.yes, new OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								if (cursor != null) {
									cursor.moveToPosition(which);
									long viewId = cursor.getLong(0);
									int count = resolver.delete(CollectionViews.buildViewUri(viewId), null, null);
									if (count == 1 && listener != null) {
										listener.onDeleteRequested(viewId);
									}
								}
							}
						})
						.setNegativeButton(R.string.no, null)
						.create()
						.show();
				}
			}, CollectionViews.NAME)
			.create();
	}
}
