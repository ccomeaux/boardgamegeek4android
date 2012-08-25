package com.boardgamegeek.util;

import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.LOGI;
import static com.boardgamegeek.util.LogUtils.LOGW;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

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

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import com.boardgamegeek.database.ResolverUtils;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;

public class ImageCache {
	private static final String TAG = makeLogTag(ImageCache.class);
	private static final String INVALID_URL = "N/A";

	private static HttpClient sHttpClient;

	public static Drawable getDrawable(Context context, Uri uri) {
		return getDrawable(context, uri, null, null, null);
	}

	public static Drawable getDrawable(Context context, Uri uri, String url) {
		return getDrawable(context, uri, null, null, url);
	}

	public static Drawable getGameThumbnail(Context context, Uri uri) {
		return getDrawable(context, uri, Games.buildGameUri(Games.getGameId(uri)), Games.THUMBNAIL_URL, null);
	}

	public static Drawable getCollectionThumbnail(Context context, Uri uri) {
		return getDrawable(context, uri, Collection.buildItemUri(Collection.getItemId(uri)), Collection.THUMBNAIL_URL,
			null);
	}

	public static Drawable getAvatar(Context context, Uri uri) {
		return getDrawable(context, uri, Buddies.buildBuddyUri(Buddies.getBuddyId(uri)), Buddies.AVATAR_URL, null);
	}

	private static Drawable getDrawable(Context context, Uri drawableUri, Uri fetchUri, String columnName,
		String fetchUrl) {
		Bitmap bitmap = null;
		ContentResolver resolver = context.getContentResolver();
		if (drawableUri != null) {
			bitmap = ResolverUtils.getBitmapFromContentProvider(resolver, drawableUri);
		}
		if (bitmap == null) {
			String url = null;
			if (!TextUtils.isEmpty(fetchUrl)) {
				url = fetchUrl;
			} else if (fetchUri != null && !TextUtils.isEmpty(columnName)) {
				url = ResolverUtils.queryString(resolver, fetchUri, columnName);
			}
			if (!TextUtils.isEmpty(url) && !INVALID_URL.equals(url)) {
				bitmap = fetchBitmap(context, url);
				if (bitmap != null) {
					ResolverUtils.putBitmapInContentProvider(resolver, drawableUri, bitmap);
				}
			}
		}

		if (bitmap == null) {
			return null;
		}
		return new BitmapDrawable(bitmap);
	}

	public static Drawable getImage(Context context, String url) throws OutOfMemoryError {
		return getImage(context, url, false);
	}

	public static Drawable getImage(Context context, String url, boolean useTempCache) throws OutOfMemoryError {
		if (INVALID_URL.equals(url)) {
			return null;
		}

		Drawable drawable = getDrawableFromCache(url, useTempCache, context);
		if (drawable != null) {
			LOGI(TAG, url + " found in cache!");
			return drawable;
		}

		Bitmap bitmap = fetchBitmap(context, url);
		if (bitmap != null) {
			addImageToCache(url, bitmap, useTempCache, context);
			return new BitmapDrawable(bitmap);
		}
		return null;
	}

	private static Bitmap fetchBitmap(Context context, String url) {
		Bitmap bitmap = null;
		try {
			final HttpClient client = getHttpClient(context);
			final HttpGet get = new HttpGet(url);
			final HttpResponse response = client.execute(get);
			final HttpEntity entity = response.getEntity();

			final int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK || entity == null) {
				LOGW(TAG, "Didn't find thumbnail");
				return null;
			}

			final byte[] imageData = EntityUtils.toByteArray(entity);
			bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);

		} catch (Exception e) {
			LOGE(TAG, "Problem loading thumbnail", e);
		}
		return bitmap;
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
			LOGE(TAG, "Could not create cache directory", e);
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
				LOGE(TAG, "Could not close stream", e);
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
