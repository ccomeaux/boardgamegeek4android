package com.boardgamegeek.util;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.boardgamegeek.provider.BggContract;

public class ImageCache {
	private static final String TAG = "ThumbnailCache";

	private static HttpClient sHttpClient;

	public static Bitmap getImage(Context context, String url) {

		Bitmap bitmap = loadFromDisk(url);
		if (bitmap != null) {
			Log.i(TAG, url + " found in cache!");
			return bitmap;
		}

		try {
			final HttpClient client = getHttpClient(context);
			final HttpGet get = new HttpGet(url);
			final HttpResponse response = client.execute(get);
			final HttpEntity entity = response.getEntity();

			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK || entity == null) {
				Log.w(TAG, "Didn't find thumbnail");
			}

			final byte[] imageData = EntityUtils.toByteArray(entity);
			bitmap = BitmapFactory.decodeByteArray(imageData, 0,
					imageData.length);
			addThumbnailToCache(url, bitmap);
			return bitmap;

		} catch (Exception e) {
			Log.e(TAG, "Problem loading thumbnail", e);
		}
		return null;
	}

	public static Drawable getDrawableFromCache(String url) {
		final String fileName = getFileNameFromUrl(url);
		if (!TextUtils.isEmpty(fileName)) {
			final File file = new File(getCacheDirectory(), fileName);
			return Drawable.createFromPath(file.getAbsolutePath());
		}
		return null;
	}
	
	private static Bitmap loadFromDisk(String url) {
		final File file = new File(getCacheDirectory(), getFileNameFromUrl(url));
		if (file.exists()) {
			InputStream stream = null;
			try {
				stream = new FileInputStream(file);
				return BitmapFactory.decodeStream(stream, null, null);
			} catch (FileNotFoundException e) {
				// Ignore
			} finally {
				closeStream(stream);
			}
		}
		return null;
	}

	private static boolean addThumbnailToCache(String url, Bitmap bitmap) {
		File cacheDirectory;
		try {
			cacheDirectory = ensureCache();
		} catch (IOException e) {
			return false;
		}

		File coverFile = new File(cacheDirectory, getFileNameFromUrl(url));
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(coverFile);
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
		final File file = new File(Environment.getExternalStorageDirectory(),
				BggContract.CONTENT_AUTHORITY);
		return new File(file, ".imagecache");
	}

	private static synchronized HttpClient getHttpClient(Context context) {
		if (sHttpClient == null) {
			sHttpClient = HttpUtils.createHttpClient(context, true);
		}
		return sHttpClient;
	}

	private static File ensureCache() throws IOException {
		File cacheDirectory = getCacheDirectory();
		if (!cacheDirectory.exists()) {
			cacheDirectory.mkdirs();
			new File(cacheDirectory, ".nomedia").createNewFile();
		}
		return cacheDirectory;
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
