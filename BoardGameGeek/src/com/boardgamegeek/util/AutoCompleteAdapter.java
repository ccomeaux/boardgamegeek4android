package com.boardgamegeek.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;

import com.boardgamegeek.R;

public class AutoCompleteAdapter extends SimpleCursorAdapter {
	private Context mContext;
	private String mColumnName;
	private Uri mUri;

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
}
