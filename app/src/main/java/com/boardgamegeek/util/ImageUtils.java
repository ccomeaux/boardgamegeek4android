package com.boardgamegeek.util;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.widget.ImageView;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.model.Image;
import com.squareup.picasso.Picasso;

import java.util.LinkedList;
import java.util.Queue;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Loading images? This is your huckleberry.
 */
public class ImageUtils {
	private static final String IMAGE_URL_PREFIX = "https://cf.geekdo-images.com/images/pic";

	private ImageUtils() {
	}

	/**
	 * Loads an image into the {@link android.widget.ImageView} by attempting various sizes and image formats. Applies
	 * fit/center crop and will load a {@link android.support.v7.graphics.Palette}.
	 */
	public static void safelyLoadImage(ImageView imageView, int imageId, Callback callback) {
		safelyLoadImage(imageView, addDefaultImagesToQueue(imageId, null), callback);
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
		imageUrls.add(imageUrl);
		safelyLoadImage(imageView, imageUrls, callback);
	}

	/**
	 * Call back from loading an image.
	 */
	public interface Callback {
		void onSuccessfulImageLoad(Palette palette);

		void onFailedImageLoad();
	}

	public static void loadThumbnail(final int imageId, final ImageView target) {
		Call<Image> call = Adapter.createGeekdoApi().image(imageId);
		call.enqueue(new retrofit2.Callback<Image>() {
			@Override
			public void onResponse(@NonNull Call<Image> call, @NonNull Response<Image> response) {
				if (response.code() == 200 && response.body() != null) {
					Queue<String> queue = new LinkedList<>();
					queue.add(response.body().images.small.url);
					addDefaultImagesToQueue(imageId, queue);
					safelyLoadThumbnail(target, queue);
				} else {
					safelyLoadThumbnail(target, addDefaultImagesToQueue(imageId, null));
				}
			}

			@Override
			public void onFailure(@NonNull Call<Image> call, @NonNull Throwable t) {
				safelyLoadThumbnail(target, addDefaultImagesToQueue(imageId, null));
			}
		});
	}

	private static Queue<String> addDefaultImagesToQueue(int imageId, Queue<String> queue) {
		if (queue == null) queue = new LinkedList<>();
		queue.add(IMAGE_URL_PREFIX + imageId + ".jpg");
		queue.add(IMAGE_URL_PREFIX + imageId + ".png");
		return queue;
	}

	public static void loadThumbnail(String path, ImageView target) {
		Queue<String> queue = new LinkedList<>();
		queue.add(path);
		safelyLoadThumbnail(target, queue);
	}

	private static void safelyLoadThumbnail(final ImageView imageView, final Queue<String> imageUrls) {
		String savedUrl = (String) imageView.getTag(R.id.url);
		String url = null;
		if (!TextUtils.isEmpty(savedUrl)) {
			if (imageUrls.contains(savedUrl)) {
				url = savedUrl;
			} else {
				imageView.setTag(R.id.url, null);
			}
		}
		if (TextUtils.isEmpty(url)) {
			url = imageUrls.poll();
		}
		if (TextUtils.isEmpty(url)) {
			return;
		}
		final String imageUrl = url;
		Picasso.with(imageView.getContext())
				.load(HttpUtils.ensureScheme(imageUrl))
				.placeholder(R.drawable.thumbnail_image_empty)
				.error(R.drawable.thumbnail_image_empty)
				.resizeDimen(R.dimen.thumbnail_list_size, R.dimen.thumbnail_list_size)
				.centerCrop()
				.into(imageView, new com.squareup.picasso.Callback() {
					@Override
					public void onSuccess() {
						imageView.setTag(R.id.url, imageUrl);
					}

					@Override
					public void onError() {
						safelyLoadThumbnail(imageView, imageUrls);
					}
				});
	}

	/**
	 * Loads an image into the {@link android.widget.ImageView} by attempting each URL in the {@link java.util.Queue}
	 * until one is successful. Applies fit/center crop and will load a {@link android.support.v7.graphics.Palette}.
	 */
	private static void safelyLoadImage(final ImageView imageView, final Queue<String> imageUrls, final Callback callback) {
		String savedUrl = (String) imageView.getTag(R.id.url);
		String url = null;
		if (!TextUtils.isEmpty(savedUrl)) {
			if (imageUrls.contains(savedUrl)) {
				url = savedUrl;
			} else {
				imageView.setTag(R.id.url, null);
			}
		}
		if (TextUtils.isEmpty(url)) {
			url = imageUrls.poll();
		}
		if (TextUtils.isEmpty(url)) {
			if (callback != null) callback.onFailedImageLoad();
			return;
		}
		final String imageUrl = url;
		Picasso.with(imageView.getContext())
				.load(HttpUtils.ensureScheme(imageUrl))
				.transform(PaletteTransformation.instance())
				.into(imageView, new com.squareup.picasso.Callback() {
					@Override
					public void onSuccess() {
						imageView.setTag(R.id.url, imageUrl);
						if (callback != null) {
							Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
							Palette palette = PaletteTransformation.getPalette(bitmap);
							callback.onSuccessfulImageLoad(palette);
						}
					}

					@Override
					public void onError() {
						safelyLoadImage(imageView, imageUrls, callback);
					}
				});
	}
}
