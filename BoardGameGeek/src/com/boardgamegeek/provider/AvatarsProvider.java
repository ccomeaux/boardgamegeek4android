package com.boardgamegeek.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public class AvatarsProvider extends BaseProvider {
	// private static final String TAG = makeLogTag(AvatarsProvider.class);

	@Override
	protected String getPath() {
		return BggContract.PATH_AVATARS;
	}

	protected int delete(Context context, SQLiteDatabase db, Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}
}
