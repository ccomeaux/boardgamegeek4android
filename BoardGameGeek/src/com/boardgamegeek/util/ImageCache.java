package com.boardgamegeek.util;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.boardgamegeek.provider.BggContract;

public class ImageCache {
	private static final String TAG = "ImageCache";
	private static final String INVALID_URL = "N/A";

	private static HttpClient sHttpClient;

	public static Drawable getImage(Context context, String url) throws OutOfMemoryError {
		return getImage(context, url, false);
	}

	public static Drawable getImage(Context context, String url, boolean useTempCache) throws OutOfMemoryError {
		if (INVALID_URL.equals(url)) {
			return null;
		}

		Drawable drawable = getDrawableFromCache(url, useTempCache, context);
		if (drawable != null) {
			Log.i(TAG, url + " found in cache!");
			return drawable;
		}

		try {
			final HttpClient client = getHttpClient(context);
			final HttpGet get = new HttpGet(url);
			final HttpResponse response = client.execute(get);
			final HttpEntity entity = response.getEntity();

			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK || entity == null) {
				Log.w(TAG, "Didn't find thumbnail");
				return null;
			}

			final byte[] imageData = EntityUtils.toByteArray(entity);
			Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);

			addImageToCache(url, bitmap, useTempCache, context);

			return new BitmapDrawable(bitmap);

		} catch (Exception e) {
			Log.e(TAG, "Problem loading thumbnail", e);
		}
		return null;
	}

	public static Drawable getDrawableFromCache(String url) {
		return getDrawableFromCache(url, false, null);
	}

	public static Drawable getDrawableFromCache(String url, boolean useTempCache, Context context) {
		final String fileName = getFileNameFromUrl(url);
		final File file = getExistingImageFile(fileName, useTempCache, context);
		if (file != null) {
			return Drawable.createFromPath(file.getAbsolutePath());
		}
		return null;
	}

	public static File getExistingImageFile(String fileName) {
		return getExistingImageFile(fileName, false, null);
	}

	public static File getExistingImageFile(String fileName, boolean useTempCache, Context context) {
		if (!TextUtils.isEmpty(fileName)) {
			final File file = new File(useTempCache ? context.getCacheDir() : getCacheDirectory(), fileName);
			if (file.exists()) {
				return file;
			}
		}
		return null;
	}

	public static boolean clear() {
		File cacheDirectory = getCacheDirectory();
		if (cacheDirectory.exists()) {
			File[] files = cacheDirectory.listFiles();
			for (File file : files) {
				file.delete();
			}
			return cacheDirectory.delete();
		}
		return true;
	}

	private static boolean addImageToCache(String url, Bitmap bitmap, boolean useTempCache, Context context) {
		if (!useTempCache && !ensureCache()) {
			return false;
		}

		if (useTempCache) {
			cleanTempCache(context);
		}

		File imageFile = new File(useTempCache ? context.getCacheDir() : getCacheDirectory(), getFileNameFromUrl(url));
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(imageFile);
			bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
		} catch (FileNotFoundException e) {
			return false;
		} finally {
			closeStream(out);
		}

		return true;
	}

	private static String getFileNameFromUrl(String url) {
		if (TextUtils.isEmpty(url)) {
			return null;
		}
		Uri uri = Uri.parse(url);
		if (uri == null) {
			return null;
		}
		return uri.getLastPathSegment();
	}

	private static File getCacheDirectory() {
		if (!isExternalStorageAvailable()) {
			return null;
		}
		final File file = new File(Environment.getExternalStorageDirectory(), BggContract.CONTENT_AUTHORITY);
		return new File(file, ".imagecache");
	}

	private static boolean ensureCache() {
		if (!isExternalStorageAvailable()) {
			return false;
		}
		try {
			File cacheDirectory = getCacheDirectory();
			if (!cacheDirectory.exists()) {
				cacheDirectory.mkdirs();
				new File(cacheDirectory, ".nomedia").createNewFile();
			}
		} catch (IOException e) {
			Log.e(TAG, "Could not create cache directory", e);
			return false;
		}
		return true;
	}

	private static boolean isExternalStorageAvailable() {
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			return true;
		}
		return false;
	}

	private static synchronized HttpClient getHttpClient(Context context) {
		if (sHttpClient == null) {
			sHttpClient = HttpUtils.createHttpClient(context, true);
		}
		return sHttpClient;
	}

	private static void closeStream(Closeable stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (IOException e) {
				Log.e(TAG, "Could not close stream", e);
			}
		}
	}

	private static void cleanTempCache(Context context) {
		File dir = context.getCacheDir();
		File[] files = dir.listFiles();
		if (files.length > 10) {
			Arrays.sort(files, new ComparatorImplementation());
			files[0].delete();
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
