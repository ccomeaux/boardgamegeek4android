package com.boardgamegeek.provider;

import static com.boardgamegeek.util.LogUtils.makeLogTag;
import static com.boardgamegeek.util.LogUtils.LOGE;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.boardgamegeek.util.FileUtils;

public class AvatarsProvider extends BaseProvider {
	private static final String TAG = makeLogTag(AvatarsProvider.class);

	@Override
	protected String getPath() {
		return BggContract.PATH_AVATARS;
	}

	protected int delete(Context context, SQLiteDatabase db, Uri uri, String selection, String[] selectionArgs) {
		try {
			return FileUtils.deleteContents(new File(FileUtils.generateContentPath(context, BggContract.PATH_AVATARS)));
		} catch (IOException e) {
			LOGE(TAG, "Couldn't delete avatars", e);
			return 0;
		}
	}
}
