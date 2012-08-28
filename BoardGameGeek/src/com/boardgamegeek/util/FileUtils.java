package com.boardgamegeek.util;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import android.net.Uri;
import android.text.TextUtils;

public class FileUtils {

	private FileUtils() {
	}

	/*
	 * Returns a usable filename from the specified URL.
	 */
	public static String getFileNameFromUrl(String url) {
		if (TextUtils.isEmpty(url)) {
			return null;
		}
		Uri uri = Uri.parse(url);
		if (uri == null) {
			return null;
		}
		return uri.getLastPathSegment();
	}

	// public static boolean clear(File directory) {
	// if (directory != null) {
	// if (directory.exists()) {
	// File[] files = directory.listFiles();
	// for (File file : files) {
	// file.delete();
	// }
	// return directory.delete();
	// }
	// return true;
	// }
	// return false;
	// }

	// from libcore.io.IoUtils and com.google.android.apps.iosched
	/**
	 * Recursively delete everything in {@code dir}.
	 */
	public static void deleteContents(File directory) throws IOException {
		// TODO: this should specify paths as Strings rather than as Files
		if (directory == null || !directory.exists()) {
			return;
		}
		final File[] files = directory.listFiles();
		if (files == null) {
			throw new IllegalArgumentException("not a directory: " + directory);
		}
		for (final File file : files) {
			if (file.isDirectory()) {
				deleteContents(file);
			}
			if (!file.delete()) {
				throw new IOException("failed to delete file: " + file);
			}
		}
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
