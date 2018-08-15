package com.boardgamegeek.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.util.FileUtils;

import java.io.IOException;

import timber.log.Timber;

public class ThumbnailsProvider extends BaseProvider {
	@Override
	protected String getPath() {
		return BggContract.PATH_THUMBNAILS;
	}

	protected int delete(Context context, SQLiteDatabase db, Uri uri, String selection, String[] selectionArgs) {
		try {
			return FileUtils.deleteContents(FileUtils.generateContentPath(context, BggContract.PATH_THUMBNAILS));
		} catch (IOException e) {
			Timber.e(e, "Couldn't delete thumbnails");
			return 0;
		}
	}
}
