package com.boardgamegeek.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;

import com.boardgamegeek.R;

public class AutoCompleteAdapter extends SimpleCursorAdapter {

	Context mContext;
	String mColumnName;
	Uri mUri;

	public AutoCompleteAdapter(Context context, String columnName, Uri uri) {
		super(context, R.layout.autocomplete_item, null, new String[] { BaseColumns._ID, columnName }, new int[] { 0,
			R.id.autocomplete_item }, 0);
		mContext = context;
		mColumnName = columnName;
		mUri = uri;
	}

	@Override
	public int getStringConversionColumn() {
		return 1;
	}

	@Override
	public CharSequence convertToString(Cursor cursor) {
		return cursor.getString(1);
	}

	// SimpleCursorAdapter sca = new SimpleCursorAdapter(this, R.layout.autocomplete_item, null,
	// PlayerNameQuery.PROJECTION, new int[] { 0, R.id.autocomplete_item }, 0);
	// mName.setAdapter(sca);

	// // Set the CursorToStringConverter, to provide the labels for the
	// // choices to be displayed in the AutoCompleteTextView.
	// sca.setCursorToStringConverter(new SimpleCursorAdapter.CursorToStringConverter() {
	// @Override
	// public CharSequence convertToString(Cursor cursor) {
	// return cursor.getString(PlayerNameQuery.NAME);
	// }
	// });

	@Override
	public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
		String selection = null;
		String[] selectionArgs = null;
		if (!TextUtils.isEmpty(constraint)) {
			selection = mColumnName + " LIKE ?";
			selectionArgs = new String[] { constraint + "%" };
		}
		return mContext.getContentResolver().query(mUri, new String[] { BaseColumns._ID, mColumnName }, selection,
			selectionArgs, null);
	}

	// // Set the FilterQueryProvider, to run queries for choices
	// // that match the specified input.
	// sca.setFilterQueryProvider(new FilterQueryProvider() {
	// @Override
	// public Cursor runQuery(CharSequence constraint) {
	// String selection = null;
	// String[] selectionArgs = null;
	// if (!TextUtils.isEmpty(constraint)) {
	// selection = PlayPlayers.NAME + " LIKE ?";
	// selectionArgs = new String[] { constraint + "%" };
	// }
	// return getContentResolver().query(Plays.buildPlayersUri(), PlayerNameQuery.PROJECTION, selection,
	// selectionArgs, null);
	// }
	// });
}
