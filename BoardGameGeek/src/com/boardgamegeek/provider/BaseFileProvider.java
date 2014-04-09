package com.boardgamegeek.provider;

import java.io.File;
import java.io.FileNotFoundException;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;

import com.boardgamegeek.util.FileUtils;

public abstract class BaseFileProvider extends BaseProvider {
	// private static final String TAG = makeLogTag(BaseFileProvider.class);

	protected abstract String getContentPath();

	/** Generates a file name based on the URI **/
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
		if (file == null || !file.exists()) {
			throw new FileNotFoundException("Couldn't get the file at the specified path.");
		}

		int parcelMode = calculateParcelMode(uri, mode);
		return ParcelFileDescriptor.open(file, parcelMode);
	}

	/** Get a {@code File} representing the locally stored results of the URI. **/
	private File getFile(Context context, Uri uri) {
		String fileName = generateFileName(context, uri);
		if (!TextUtils.isEmpty(fileName)) {
			return new File(FileUtils.generateContentPath(context, getContentPath()), fileName);
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
