package com.boardgamegeek.util;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Loading images? This is your huckleberry.
 */
public class ImageUtils {
	private static final String IMAGE_URL_PREFIX = "https://cf.geekdo-images.com/images/pic";
	public static final String SUFFIX_MEDIUM = "_md";
	private static final String SUFFIX_SQUARE = "_sq";
	private static final String SUFFIX_SMALL = "_t";
	private static final String SUFFIX_LARGE = "_lg";
	private static final float IMAGE_ASPECT_RATIO = 1.6777777f;

	private ImageUtils() {
	}

	/**
	 * Create a URL for a thumbnail image as a JPG.
	 */
	public static String createThumbnailJpgUrl(int imageId) {
		return IMAGE_URL_PREFIX + imageId + SUFFIX_SMALL + ".jpg";
	}

	/**
	 * Create a URL for a thumbnail image as a PNG.
	 */
	public static String createThumbnailPngUrl(int imageId) {
		return IMAGE_URL_PREFIX + imageId + SUFFIX_SMALL + ".png";
	}

	/**
	 * Loads an image into the {@link android.widget.ImageView} by attempting various sizes and image formats. Applies
	 * fit/center crop and will load a {@link android.support.v7.graphics.Palette}.
	 */
	public static void safelyLoadImage(ImageView imageView, int imageId, Callback callback) {
		Queue<String> imageUrls = new LinkedList<>();
		String imageUrl = IMAGE_URL_PREFIX + imageId + ".jpg";
		imageUrls.add(appendImageUrl(imageUrl, SUFFIX_MEDIUM));
		imageUrls.add(appendImageUrl(imageUrl, SUFFIX_SMALL));
		imageUrls.add(imageUrl);
		imageUrl = IMAGE_URL_PREFIX + imageId + ".png";
		imageUrls.add(appendImageUrl(imageUrl, SUFFIX_MEDIUM));
		imageUrls.add(appendImageUrl(imageUrl, SUFFIX_SMALL));
		imageUrls.add(imageUrl);
		safelyLoadImage(imageView, imageUrls, callback);
	}

	/**
	 * Loads an image into the {@link android.widget.ImageView} by attempting various sizes. Applies fit/center crop.
	 */
	public static void safelyLoadImage(ImageView imageView, String imageUrl) {
		safelyLoadImage(imageView, imageUrl, null);
	}

	/**
	 * Loads an image into the {@link android.widget.ImageView} by attempting various sizes. Applies fit/center crop and
	 * will load a {@link android.support.v7.graphics.Palette}.
	 */
	public static void safelyLoadImage(ImageView imageView, String imageUrl, Callback callback) {
		Queue<String> imageUrls = new LinkedList<>();
		imageUrls.add(appendImageUrl(imageUrl, SUFFIX_MEDIUM));
		imageUrls.add(appendImageUrl(imageUrl, SUFFIX_SMALL));
		imageUrls.add(imageUrl);
		safelyLoadImage(imageView, imageUrls, callback);
	}

	/**
	 * Loads an image into the {@link android.widget.ImageView} by attempting each URL in the {@link java.util.Queue}
	 * until one is successful. Applies fit/center crop and will load a {@link android.support.v7.graphics.Palette}.
	 */
	private static void safelyLoadImage(final ImageView imageView,
										final Queue<String> imageUrls,
										final Callback callback) {
		String imageUrl = imageUrls.poll();
		if (TextUtils.isEmpty(imageUrl)) {
			return;
		}
		Picasso
			.with(imageView.getContext())
			.load(HttpUtils.ensureScheme(imageUrl))
			.transform(PaletteTransformation.instance())
			.into(imageView, new com.squareup.picasso.Callback() {
				@Override
				public void onSuccess() {
					Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
					Palette palette = PaletteTransformation.getPalette(bitmap);
					if (callback != null) {
						callback.onPaletteGenerated(palette);
					}
				}

				@Override
				public void onError() {
					safelyLoadImage(imageView, imageUrls, callback);
				}
			});
	}

	/**
	 * Append a suffix to an image URL. Assumes the URL has no suffix (but may have an extension or not.
	 */
	public static String appendImageUrl(String imageUrl, String suffix) {
		if (TextUtils.isEmpty(imageUrl)) {
			return "";
		}
		if (TextUtils.isEmpty(suffix)) {
			return imageUrl;
		}
		int dot = imageUrl.lastIndexOf('.');
		if (dot == -1) {
			return imageUrl + suffix;
		} else {
			return imageUrl.substring(0, dot) + suffix + imageUrl.substring(dot, imageUrl.length());
		}
	}

	/**
	 * Resize the resizableView based on a standard aspect ratio, up to a maximum height
	 */
	public static void resizeImagePerAspectRatio(View image, int maxHeight, View resizableView) {
		int height = (int) (image.getWidth() / IMAGE_ASPECT_RATIO);
		height = Math.min(height, maxHeight);

		ViewGroup.LayoutParams lp;
		lp = resizableView.getLayoutParams();
		if (lp.height != height) {
			lp.height = height;
			resizableView.setLayoutParams(lp);
		}
	}

	/**
	 * Call back from loading an image.
	 */
	public interface Callback {
		void onPaletteGenerated(Palette palette);
	}
}
