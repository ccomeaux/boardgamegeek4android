package com.boardgamegeek.util;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

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
	private static final String TAG = "ThumbnailCache";
	private static final String INVALID_URL = "N/A";

	private static HttpClient sHttpClient;

	public static Drawable getImage(Context context, String url) throws OutOfMemoryError {
		if (INVALID_URL.equals(url)) {
			return null;
		}

		Drawable drawable = getDrawableFromCache(url);
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
			addImageToCache(url, bitmap);
			return new BitmapDrawable(bitmap);

		} catch (Exception e) {
			Log.e(TAG, "Problem loading thumbnail", e);
		}
		return null;
	}

	public static Drawable getDrawableFromCache(String url) {
		final String fileName = getFileNameFromUrl(url);
		final File file = getExistingImageFile(fileName);
		if (file != null) {
			return Drawable.createFromPath(file.getAbsolutePath());
		}
		return null;
	}

	public static File getExistingImageFile(String fileName) {
		if (!TextUtils.isEmpty(fileName)) {
			final File file = new File(getCacheDirectory(), fileName);
			if (file.exists()) {
				return file;
			}
		}
		return null;
	}

	public static boolean clear() {
		// TODO: this isn't working, may have to delete each file instead of the folder
		File cacheDirectory = getCacheDirectory();
		if (cacheDirectory.exists()) {
			return cacheDirectory.delete();
		}
		return true;
	}

	private static boolean addImageToCache(String url, Bitmap bitmap) {
		if (!ensureCache()) {
			return false;
		}

		File imageFile = new File(getCacheDirectory(), getFileNameFromUrl(url));
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
		final File file = new File(Environment.getExternalStorageDirectory(), BggContract.CONTENT_AUTHORITY);
		return new File(file, ".imagecache");
	}

	private static boolean ensureCache() {
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
}
