package com.boardgamegeek.util;

import static com.boardgamegeek.util.LogUtils.LOGD;
import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.text.TextUtils;

import com.boardgamegeek.provider.BggContract;

public class ImageUtils {
	private static final String TAG = makeLogTag(ImageUtils.class);
	public static final int IO_BUFFER_SIZE_BYTES = 4 * 1024; // 4KB
	private static final int MAX_THUMBNAIL_BYTES = 130 * 1024; // 130KB

	public static BitmapDrawable processDrawableFromResolver(Context context, Uri uri, String urlString) {
		Bitmap bitmap = processBitmapFromResolver(context, uri, urlString);
		if (bitmap == null) {
			return null;
		}
		return new BitmapDrawable(context.getResources(), bitmap);
	}

	public static Bitmap processBitmapFromResolver(Context context, Uri uri, String urlString) {
		Bitmap bitmap = null;
		ContentResolver resolver = context.getContentResolver();
		if (uri != null) {
			bitmap = ResolverUtils.getBitmapFromContentProvider(resolver, uri);
		}
		if (bitmap == null && !TextUtils.isEmpty(urlString)) {
			final byte[] bitmapBytes = downloadBitmapToMemory(urlString, MAX_THUMBNAIL_BYTES);
			if (bitmapBytes != null) {
				bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
				if (bitmap != null && uri != null) {
					ResolverUtils.putBitmapInContentProvider(resolver, uri, bitmap);
				}
			}
		}
		return bitmap;
	}

	/**
	 * Download a bitmap from a URL, write it to a disk and return the File pointer. This implementation uses a simple
	 * disk cache.
	 * 
	 * @param urlString
	 *            The URL to fetch
	 * @param maxBytes
	 *            The maximum number of bytes to read before returning null to protect against OutOfMemory exceptions.
	 * @return A File pointing to the fetched bitmap
	 */
	public static byte[] downloadBitmapToMemory(String urlString, int maxBytes) {
		LOGD(TAG, "downloadBitmapToMemory - downloading - " + urlString);

		if (TextUtils.isEmpty(urlString) || urlString.equals(BggContract.INVALID_URL)) {
			return null;
		}

		HttpUtils.disableConnectionReuseIfNecessary();
		HttpURLConnection urlConnection = null;
		ByteArrayOutputStream out = null;
		InputStream in = null;

		try {
			final URL url = new URL(urlString);
			urlConnection = (HttpURLConnection) url.openConnection();
			if (urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				return null;
			}
			in = new BufferedInputStream(urlConnection.getInputStream(), IO_BUFFER_SIZE_BYTES);
			out = new ByteArrayOutputStream(IO_BUFFER_SIZE_BYTES);

			final byte[] buffer = new byte[128];
			int total = 0;
			int bytesRead;
			while ((bytesRead = in.read(buffer)) != -1) {
				total += bytesRead;
				if (total > maxBytes) {
					return null;
				}
				out.write(buffer, 0, bytesRead);
			}
			return out.toByteArray();
		} catch (final IOException e) {
			LOGE(TAG, "Error in downloadBitmapToMemory - " + e);
		} finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
			try {
				if (in != null) {
					in.close();
				}
				if (out != null) {
					out.close();
				}
			} catch (final IOException e) {
			}
		}
		return null;
	}
}
