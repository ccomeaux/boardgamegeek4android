/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.boardgamegeek.util;

import static com.boardgamegeek.util.LogUtils.LOGD;
import static com.boardgamegeek.util.LogUtils.LOGE;
import static com.boardgamegeek.util.LogUtils.makeLogTag;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.ImageView;

/**
 * A subclass of {@link ImageWorker} that fetches images from a URL.
 */
public class ImageFetcher extends ImageWorker {
	private static final String TAG = makeLogTag(ImageFetcher.class);

	public static final int IO_BUFFER_SIZE_BYTES = 4 * 1024; // 4KB

	// Default fetcher params
	private static final int HTTP_CACHE_SIZE = 5 * 1024 * 1024; // 5MB
	private static final String HTTP_CACHE_DIR = "http";
	private static final int DEFAULT_IMAGE_HEIGHT = 1024;
	private static final int DEFAULT_IMAGE_WIDTH = 1024;

	protected int mImageWidth;
	protected int mImageHeight;
	private Context mContext;
	private DiskLruCache mHttpDiskCache;
	private File mHttpCacheDir;
	private boolean mHttpDiskCacheStarting = true;
	private final Object mHttpDiskCacheLock = new Object();
	private static final int DISK_CACHE_INDEX = 0;

	/**
	 * Create an ImageFetcher specifying max image loading width/height.
	 */
	public ImageFetcher(Context context, int imageWidth, int imageHeight) {
		super(context);
		init(context, imageWidth, imageHeight);
	}

	/**
	 * Create an ImageFetcher using defaults.
	 */
	public ImageFetcher(Context context) {
		super(context);
		init(context, DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT);
	}

	private void init(Context context, int imageWidth, int imageHeight) {
		mContext = context;
		mImageWidth = imageWidth;
		mImageHeight = imageHeight;
		mHttpCacheDir = ImageCache.getDiskCacheDir(context, HTTP_CACHE_DIR);
		if (!mHttpCacheDir.exists()) {
			mHttpCacheDir.mkdirs();
		}
	}

	public void loadAvatarImage(String url, Uri uri, ImageView imageView) {
		safelyLoadImage(url, uri, imageView, ImageData.IMAGE_TYPE_AVATAR);
	}

	public void loadThumnailImage(String url, Uri uri, ImageView imageView) {
		safelyLoadImage(url, uri, imageView, ImageData.IMAGE_TYPE_THUMBNAIL);
	}

	public void loadImage(String url, ImageView imageView) {
		safelyLoadImage(url, null, imageView, ImageData.IMAGE_TYPE_NORMAL);
	}

	private void safelyLoadImage(String url, Uri uri, ImageView imageView, int imageType) {
		if (TextUtils.isEmpty(url)) {
			if (imageView != null) {
				imageView.setImageBitmap(mLoadingBitmap);
			}
		} else {
			loadImage(new ImageData(url, imageType, uri), imageView);
		}
	}

	/**
	 * Set the target image width and height.
	 */
	public void setImageSize(int width, int height) {
		mImageWidth = width;
		mImageHeight = height;
	}

	/**
	 * Set the target image size (width and height will be the same).
	 */
	public void setImageSize(int size) {
		setImageSize(size, size);
	}

	@Override
	protected Bitmap processBitmap(Object key) {
		final ImageData imageData = (ImageData) key;
		return processBitmap(imageData.mUrl, imageData.mType, imageData.mUri);
	}

	/**
	 * The main process method, which will be called by the ImageWorker in the AsyncTask background thread.
	 * 
	 * @param key
	 *            The key to load the bitmap, in this case, a regular http URL
	 * @return The downloaded and resized bitmap
	 */
	private Bitmap processBitmap(String key, int type, Uri uri) {
		LOGD(TAG, "processBitmap - " + key);

		if (type == ImageData.IMAGE_TYPE_NORMAL) {
			return processNormalBitmap(key); // Process a regular, full sized bitmap
		} else if (type == ImageData.IMAGE_TYPE_THUMBNAIL) {
			return ImageUtils.processBitmapFromResolver(mContext, uri, key);
		} else if (type == ImageData.IMAGE_TYPE_AVATAR) {
			return ImageUtils.processBitmapFromResolver(mContext, uri, key);
		}
		return null;
	}

	/**
	 * Download and resize a normal sized remote bitmap from a HTTP URL using a HTTP cache.
	 * 
	 * @param urlString
	 *            The URL of the image to download
	 * @return The scaled bitmap
	 */
	private Bitmap processNormalBitmap(String urlString) {
		final String key = ImageCache.hashKeyForDisk(urlString);
		FileDescriptor fileDescriptor = null;
		FileInputStream fileInputStream = null;
		DiskLruCache.Snapshot snapshot;
		synchronized (mHttpDiskCacheLock) {
			// Wait for disk cache to initialize
			while (mHttpDiskCacheStarting) {
				try {
					mHttpDiskCacheLock.wait();
				} catch (InterruptedException e) {
				}
			}

			if (mHttpDiskCache != null) {
				try {
					snapshot = mHttpDiskCache.get(key);
					if (snapshot == null) {
						LOGD(TAG, "processBitmap, not found in http cache, downloading...");
						DiskLruCache.Editor editor = mHttpDiskCache.edit(key);
						if (editor != null) {
							if (downloadUrlToStream(urlString, editor.newOutputStream(DISK_CACHE_INDEX))) {
								editor.commit();
							} else {
								editor.abort();
							}
						}
						snapshot = mHttpDiskCache.get(key);
					}
					if (snapshot != null) {
						fileInputStream = (FileInputStream) snapshot.getInputStream(DISK_CACHE_INDEX);
						fileDescriptor = fileInputStream.getFD();
					}
				} catch (IOException e) {
					LOGE(TAG, "processBitmap - " + e);
				} catch (IllegalStateException e) {
					LOGE(TAG, "processBitmap - " + e);
				} finally {
					if (fileDescriptor == null && fileInputStream != null) {
						try {
							fileInputStream.close();
						} catch (IOException e) {
						}
					}
				}
			}
		}

		Bitmap bitmap = null;
		if (fileDescriptor != null) {
			bitmap = decodeSampledBitmapFromDescriptor(fileDescriptor, mImageWidth, mImageHeight);
		}
		if (fileInputStream != null) {
			try {
				fileInputStream.close();
			} catch (IOException e) {
			}
		}
		return bitmap;
	}

	/**
	 * Download a bitmap from a URL and write the content to an output stream.
	 * 
	 * @param urlString
	 *            The URL to fetch
	 * @param outputStream
	 *            The outputStream to write to
	 * @return true if successful, false otherwise
	 */
	public boolean downloadUrlToStream(String urlString, OutputStream outputStream) {
		HttpUtils.disableConnectionReuseIfNecessary();
		HttpURLConnection urlConnection = null;
		BufferedOutputStream out = null;
		BufferedInputStream in = null;

		try {
			final URL url = new URL(urlString);
			urlConnection = (HttpURLConnection) url.openConnection();
			in = new BufferedInputStream(urlConnection.getInputStream(), IO_BUFFER_SIZE_BYTES);
			out = new BufferedOutputStream(outputStream, IO_BUFFER_SIZE_BYTES);

			int b;
			while ((b = in.read()) != -1) {
				out.write(b);
			}
			return true;
		} catch (final IOException e) {
			LOGE(TAG, "Error in downloadBitmap - " + e);
		} finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
			try {
				if (out != null) {
					out.close();
				}
				if (in != null) {
					in.close();
				}
			} catch (final IOException e) {
			}
		}
		return false;
	}

	/**
	 * Decode and sample down a bitmap from a file to the requested width and height.
	 * 
	 * @param filename
	 *            The full path of the file to decode
	 * @param reqWidth
	 *            The requested width of the resulting bitmap
	 * @param reqHeight
	 *            The requested height of the resulting bitmap
	 * @return A bitmap sampled down from the original with the same aspect ratio and dimensions that are equal to or
	 *         greater than the requested width and height
	 */
	public static Bitmap decodeSampledBitmapFromFile(String filename, int reqWidth, int reqHeight) {

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filename, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeFile(filename, options);
	}

	/**
	 * Decode and sample down a bitmap from a file input stream to the requested width and height.
	 * 
	 * @param fileDescriptor
	 *            The file descriptor to read from
	 * @param reqWidth
	 *            The requested width of the resulting bitmap
	 * @param reqHeight
	 *            The requested height of the resulting bitmap
	 * @return A bitmap sampled down from the original with the same aspect ratio and dimensions that are equal to or
	 *         greater than the requested width and height
	 */
	public static Bitmap decodeSampledBitmapFromDescriptor(FileDescriptor fileDescriptor, int reqWidth, int reqHeight) {

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
	}

	/**
	 * Calculate an inSampleSize for use in a {@link android.graphics.BitmapFactory.Options} object when decoding
	 * bitmaps using the decode* methods from {@link BitmapFactory}. This implementation calculates the closest
	 * inSampleSize that will result in the final decoded bitmap having a width and height equal to or larger than the
	 * requested width and height. This implementation does not ensure a power of 2 is returned for inSampleSize which
	 * can be faster when decoding but results in a larger bitmap which isn't as useful for caching purposes.
	 * 
	 * @param options
	 *            An options object with out* params already populated (run through a decode* method with
	 *            inJustDecodeBounds==true
	 * @param reqWidth
	 *            The requested width of the resulting bitmap
	 * @param reqHeight
	 *            The requested height of the resulting bitmap
	 * @return The value to be used for inSampleSize
	 */
	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			// Calculate ratios of height and width to requested height and width
			final int heightRatio = Math.round((float) height / (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);

			// Choose the smallest ratio as inSampleSize value, this will guarantee
			// a final image with both dimensions larger than or equal to the
			// requested height and width.
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;

			// This offers some additional logic in case the image has a strange
			// aspect ratio. For example, a panorama may have a much larger
			// width than height. In these cases the total pixels might still
			// end up being too large to fit comfortably in memory, so we should
			// be more aggressive with sample down the image (=larger
			// inSampleSize).

			final float totalPixels = width * height;

			// Anything more than 2x the requested pixels we'll sample down
			// further.
			final float totalReqPixelsCap = reqWidth * reqHeight * 2;

			while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
				inSampleSize++;
			}
		}
		return inSampleSize;
	}

	@Override
	protected void initDiskCacheInternal() {
		super.initDiskCacheInternal();
		initHttpDiskCache();
	}

	private void initHttpDiskCache() {
		if (!mHttpCacheDir.exists()) {
			mHttpCacheDir.mkdirs();
		}
		synchronized (mHttpDiskCacheLock) {
			if (ImageCache.getUsableSpace(mHttpCacheDir) > HTTP_CACHE_SIZE) {
				try {
					mHttpDiskCache = DiskLruCache.open(mHttpCacheDir, 1, 1, HTTP_CACHE_SIZE);
					LOGD(TAG, "HTTP cache initialized");
				} catch (IOException e) {
					mHttpDiskCache = null;
				}
			}
			mHttpDiskCacheStarting = false;
			mHttpDiskCacheLock.notifyAll();
		}
	}

	@Override
	protected void clearCacheInternal() {
		super.clearCacheInternal();
		synchronized (mHttpDiskCacheLock) {
			if (mHttpDiskCache != null && !mHttpDiskCache.isClosed()) {
				try {
					mHttpDiskCache.delete();
					LOGD(TAG, "HTTP cache cleared");
				} catch (IOException e) {
					LOGE(TAG, "clearCacheInternal - " + e);
				}
				mHttpDiskCache = null;
				mHttpDiskCacheStarting = true;
				initHttpDiskCache();
			}
		}
	}

	@Override
	protected void flushCacheInternal() {
		super.flushCacheInternal();
		synchronized (mHttpDiskCacheLock) {
			if (mHttpDiskCache != null) {
				try {
					mHttpDiskCache.flush();
					LOGD(TAG, "HTTP cache flushed");
				} catch (IOException e) {
					LOGE(TAG, "flush - " + e);
				}
			}
		}
	}

	@Override
	protected void closeCacheInternal() {
		super.closeCacheInternal();
		synchronized (mHttpDiskCacheLock) {
			if (mHttpDiskCache != null) {
				try {
					if (!mHttpDiskCache.isClosed()) {
						mHttpDiskCache.close();
						mHttpDiskCache = null;
						LOGD(TAG, "HTTP cache closed");
					}
				} catch (IOException e) {
					LOGE(TAG, "closeCacheInternal - " + e);
				}
			}
		}
	}

	private static class ImageData {
		public static final int IMAGE_TYPE_THUMBNAIL = 0;
		public static final int IMAGE_TYPE_NORMAL = 1;
		public static final int IMAGE_TYPE_AVATAR = 2;
		public int mType;
		public String mUrl;
		public Uri mUri;

		public ImageData(String url, int type, Uri uri) {
			mUrl = url;
			mType = type;
			mUri = uri;
		}

		@Override
		public String toString() {
			return mUrl;
		}
	}
}