package com.boardgamegeek.util;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import android.content.Context;
import android.text.TextUtils;

import com.boardgamegeek.provider.BggContract;

public class FileUtils {

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

	public static File generateContentPath(Context context, String type) {
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

	// from libcore.io.IoUtils and com.google.android.apps.iosched
	/**
	 * Recursively delete everything in {@code dir}.
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

	/*
	 * Remove all but the X most recently created files
	 */
	public static void trimDirectory(File directory, int fileCount) {
		if (directory != null) {
			File[] files = directory.listFiles();
			if (files.length > fileCount) {
				Arrays.sort(files, new ComparatorImplementation());
				files[0].delete();
			}
		}
	}

	private static final class ComparatorImplementation implements Comparator<File> {
		public int compare(File f1, File f2) {
			if (f1.lastModified() > f2.lastModified()) {
				return -1;
			} else if (f1.lastModified() < f2.lastModified()) {
				return 1;
			} else {
				return 0;
			}
		}
	}
}
