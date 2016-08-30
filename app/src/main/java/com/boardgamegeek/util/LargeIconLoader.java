package com.boardgamegeek.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Picasso.LoadedFrom;
import com.squareup.picasso.Target;

import java.util.LinkedList;
import java.util.Queue;

import timber.log.Timber;

public class LargeIconLoader implements Target {
	public interface Callback {
		void onSuccessfulImageLoad(Bitmap bitmap);

		void onFailedImageLoad();
	}

	private final Context context;
	private final Callback callback;
	private final Queue<String> imageUrls;
	private String currentImageUrl;

	public LargeIconLoader(Context context, String imageUrl, String thumbnailUrl, Callback callback) {
		this.context = context;
		this.imageUrls = new LinkedList<>();
		this.callback = callback;
		imageUrls.add(ImageUtils.appendImageUrl(imageUrl, ImageUtils.SUFFIX_MEDIUM));
		imageUrls.add(imageUrl);
		imageUrls.add(thumbnailUrl);
		imageUrls.add(ImageUtils.appendImageUrl(imageUrl, ImageUtils.SUFFIX_MEDIUM));
	}

	public void execute() {
		loadNextPath();
	}

	private void loadNextPath() {
		currentImageUrl = imageUrls.poll();
		if (TextUtils.isEmpty(currentImageUrl)) {
			if (callback != null) {
				callback.onFailedImageLoad();
			}
		}
		Picasso.with(context.getApplicationContext())
			.load(HttpUtils.ensureScheme(currentImageUrl))
			.networkPolicy(NetworkPolicy.NO_STORE)
			.resize(400, 400) // recommended size for wearables
			.centerCrop()
			.into(this);
	}

	@Override
	public void onBitmapLoaded(Bitmap bitmap, LoadedFrom from) {
		Timber.i("Found an image at " + currentImageUrl);
		if (callback != null) {
			callback.onSuccessfulImageLoad(bitmap);
		}
	}

	@Override
	public void onBitmapFailed(Drawable errorDrawable) {
		Timber.i("Didn't find an image at " + currentImageUrl);
		loadNextPath();
	}

	@Override
	public void onPrepareLoad(Drawable placeHolderDrawable) {
	}
}
