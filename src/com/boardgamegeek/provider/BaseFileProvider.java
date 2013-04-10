package com.boardgamegeek.provider;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.boardgamegeek.util.FileUtils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;

public abstract class BaseFileProvider extends BaseProvider {
	private static final String TAG = makeLogTag(BaseFileProvider.class);

	protected abstract String getContentPath();

	protected abstract String generateFileName(Context context, Uri uri);

	@Override
	protected int delete(Context context, SQLiteDatabase db, Uri uri, String selection, String[] selectionArgs) {
		File file = getFile(context, uri);
		if (file != null && file.exists()) {
			boolean deleted = file.delete();
			return deleted ? 1 : 0;
		}
		return 0;
	}

	@Override
	protected ParcelFileDescriptor openFile(Context context, Uri uri, String mode) throws FileNotFoundException {
		File file = getFile(context, uri);
		if (file == null) {
			throw new FileNotFoundException("Couldn't get the file at the specified path.");
		}

		if (!file.exists()) {
			try {
				if (!file.createNewFile()) {
					throw new FileNotFoundException("Couldn't create a new file for " + file.getAbsolutePath());
				}
			} catch (IOException e) {
				LOGE(TAG, "Error creating a new file.", e);
				throw new FileNotFoundException(e.getMessage());
			}
		}

		int parcelMode = calculateParcelMode(uri, mode);
		return ParcelFileDescriptor.open(file, parcelMode);
	}

	private File getFile(Context context, Uri uri) {
		String fileName = generateFileName(context, uri);
		if (!TextUtils.isEmpty(fileName)) {
			String path = FileUtils.generateContentPath(context, getContentPath());
			if (path == null) {
				return null;
			}
			return new File(path, fileName);
		}
		return null;
	}

	// from Android ContentResolver.modeToMode
	private static int calculateParcelMode(Uri uri, String mode) throws FileNotFoundException {
		int modeBits;
		if ("r".equals(mode)) {
			modeBits = ParcelFileDescriptor.MODE_READ_ONLY;
		} else if ("w".equals(mode) || "wt".equals(mode)) {
			modeBits = ParcelFileDescriptor.MODE_WRITE_ONLY | ParcelFileDescriptor.MODE_CREATE
				| ParcelFileDescriptor.MODE_TRUNCATE;
		} else if ("wa".equals(mode)) {
			modeBits = ParcelFileDescriptor.MODE_WRITE_ONLY | ParcelFileDescriptor.MODE_CREATE
				| ParcelFileDescriptor.MODE_APPEND;
		} else if ("rw".equals(mode)) {
			modeBits = ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE;
		} else if ("rwt".equals(mode)) {
			modeBits = ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE
				| ParcelFileDescriptor.MODE_TRUNCATE;
		} else {
			throw new FileNotFoundException("Bad mode for " + uri + ": " + mode);
		}
		return modeBits;
	}
}
