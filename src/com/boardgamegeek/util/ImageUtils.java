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
import com.boardgamegeek.provider.BggContract.Buddies;
import com.boardgamegeek.provider.BggContract.Collection;
import com.boardgamegeek.provider.BggContract.Games;

public class ImageUtils {
	private static final String TAG = makeLogTag(ImageUtils.class);
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
				bitmap = downloadBitmap(context, url);
				if (bitmap != null) {
					ResolverUtils.putBitmapInContentProvider(resolver, drawableUri, bitmap);
				}
			}
		}

		if (bitmap == null) {
			return null;
		}
		return new BitmapDrawable(context.getResources(), bitmap);
	}

	private static Bitmap downloadBitmap(Context context, String url) {
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

	public static Drawable getImage(Context context, String url) throws OutOfMemoryError {
		if (TextUtils.isEmpty(url) || INVALID_URL.equals(url)) {
			return null;
		}

		Drawable drawable = getImageFromCache(context, url);
		if (drawable != null) {
			LOGI(TAG, url + " found in cache!");
			return drawable;
		}

		Bitmap bitmap = downloadBitmap(context, url);
		if (bitmap != null) {
			addImageToCache(url, bitmap, context);
			return new BitmapDrawable(context.getResources(), bitmap);
		}
		return null;
	}

	public static Drawable getImageFromCache(Context context, String url) {
		final String fileName = FileUtils.getFileNameFromUrl(url);
		final File file = getExistingImageFile(fileName, context);
		if (file != null) {
			return Drawable.createFromPath(file.getAbsolutePath());
		}
		return null;
	}

	private static File getExistingImageFile(String fileName, Context context) {
		if (!TextUtils.isEmpty(fileName)) {
			final File file = new File(getCacheDirectory(context), fileName);
			if (file.exists()) {
				return file;
			}
		}
		return null;
	}

	public static boolean clearCache(Context context) {
		try {
			FileUtils.deleteContents(getCacheDirectory(context));
		} catch (IOException e) {
			LOGE(TAG, "Error clearing the cache", e);
			return false;
		}
		return true;
	}

	private static boolean addImageToCache(String url, Bitmap bitmap, Context context) {
		File cacheDir = getCacheDirectory(context);
		if (cacheDir == null) {
			return false;
		}

		FileUtils.trimDirectory(cacheDir, 10);

		File imageFile = new File(cacheDir, FileUtils.getFileNameFromUrl(url));
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

	private static File getCacheDirectory(Context context) {
		File file;
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			file = context.getExternalCacheDir();
		} else {
			file = context.getCacheDir();
		}
		return file;
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
}
