package com.boardgamegeek.provider;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.io.IOException;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.util.FileUtils;

public class ThumbnailsProvider extends BaseProvider {
	private static final String TAG = makeLogTag(ThumbnailsProvider.class);

	@Override
	protected String getPath() {
		return BggContract.PATH_THUMBNAILS;
	}

	protected int delete(Context context, SQLiteDatabase db, Uri uri, String selection, String[] selectionArgs) {
		try {
			return FileUtils.deleteContents(FileUtils.generateContentPath(context, BggContract.PATH_THUMBNAILS));
		} catch (IOException e) {
			LOGE(TAG, "Couldn't delete avatars", e);
			return 0;
		}
	}
}
