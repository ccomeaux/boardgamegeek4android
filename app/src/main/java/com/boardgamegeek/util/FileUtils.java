package com.boardgamegeek.util;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import com.boardgamegeek.provider.BggContract;

import java.io.File;
import java.io.IOException;

public class FileUtils {
	private static final String EXPORT_FOLDER = "bgg4android-export";
	private static final String EXPORT_FOLDER_AUTO = EXPORT_FOLDER + File.separator + "AutoBackup";

	private FileUtils() {
	}

	/*
	 * Returns a usable filename from the specified URL.
	 */
	public static String getFileNameFromUrl(String url) {
		if (!TextUtils.isEmpty(url) && !BggContract.INVALID_URL.equals(url)) {
			int index = url.lastIndexOf('/');
			if (index > 0) {
				return url.substring(index + 1);
			}
		}
		return null;
	}

	/**
	 * Find a path to store the specific type of content, ensuring that it exists. Returns null if none can be found or
	 * created.
	 */
	public static File generateContentPath(Context context, String type) {
		if (context == null) {
			return null;
		}
		File base = context.getExternalFilesDir(type);
		if (base == null) {
			return null;
		}
		if (!base.exists()) {
			if (!base.mkdirs()) {
				return null;
			}
		}
		return base;
	}

	/**
	 * Recursively delete everything in {@code dir}. From libcore.io.IoUtils and com.google.android.apps.iosched.
	 */
	public static int deleteContents(File directory) throws IOException {
		// TODO: this should specify paths as Strings rather than as Files
		if (directory == null || !directory.exists()) {
			return 0;
		}
		final File[] files = directory.listFiles();
		if (files == null) {
			throw new IllegalArgumentException("not a directory: " + directory);
		}

		int count = 0;
		for (final File file : files) {
			if (file.isDirectory()) {
				count += deleteContents(file);
			}
			if (!file.delete()) {
				throw new IOException("failed to delete file: " + file);
			}
			count++;
		}
		return count;
	}

	/**
	 * Checks if {@link Environment}.MEDIA_MOUNTED is returned by {@code getExternalStorageState()}
	 * and therefore external storage is read- and writeable.
	 */
	public static boolean isExtStorageAvailable() {
		return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
	}

	public static File getExportPath(boolean isAutoBackupMode) {
		return new File(
			Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
			isAutoBackupMode ? EXPORT_FOLDER_AUTO : EXPORT_FOLDER);
	}
}
