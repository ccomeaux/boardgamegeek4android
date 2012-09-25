package com.boardgamegeek.provider;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;

import com.boardgamegeek.database.ResolverUtils;

public abstract class BaseFileProvider extends BaseProvider {
	private static final String TAG = makeLogTag(BaseFileProvider.class);

	protected abstract Uri getFileUri(Uri uri);

	protected abstract String getColumnName();

	protected abstract String getContentPath();

	@Override
	protected ParcelFileDescriptor openFile(Context context, Uri uri, String mode) throws FileNotFoundException {
		File file = null;
		String fileName = fetchFileName(context, getFileUri(uri), getColumnName());
		if (!TextUtils.isEmpty(fileName)) {
			String path = generateContentPath(context, getContentPath());
			if (path == null) {
				return null;
			}
			file = new File(path, fileName);
		}
		if (file == null) {
			return null;
		}

		if (!file.exists()) {
			try {
				if (!file.createNewFile()) {
					throw new FileNotFoundException();
				}
			} catch (IOException e) {
				LOGE(TAG, "Error creating a new file.", e);
				throw new FileNotFoundException();
			}
		}

		int parcelMode = calculateParcelMode(uri, mode);
		return ParcelFileDescriptor.open(file, parcelMode);
	}

	protected String fetchFileName(Context context, Uri uri, String columnName) {
		String path = ResolverUtils.queryString(context.getContentResolver(), uri, columnName);
		if (!TextUtils.isEmpty(path)) {
			int i = path.lastIndexOf(File.separator);
			if (i > 0) {
				return path.substring(i + 1);
			}
		}
		return null;
	}

	protected String generateContentPath(Context context, String type) {
		File base = context.getExternalFilesDir(null);
		if (base == null) {
			return null;
		}
		String path = base.getPath() + File.separator + "content" + File.separator + type;
		File folder = new File(path);
		if (!folder.exists()) {
			folder.mkdirs();
		}
		return path;
	}

	// from Android ContentResolver.modeToMode
	protected static int calculateParcelMode(Uri uri, String mode) throws FileNotFoundException {
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
