package com.boardgamegeek.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Picasso.LoadedFrom;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import timber.log.Timber;

public class LargeIconLoader implements Target {

	private static final int WEARABLE_ICON_SIZE = 400;

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

	public void executeInBackground() {
		currentImageUrl = imageUrls.poll();
		if (TextUtils.isEmpty(currentImageUrl)) {
			if (callback != null) {
				callback.onFailedIconLoad();
			}
		} else {
			try {
				final Bitmap bitmap = getRequestCreator().get();
				onBitmapLoaded(bitmap, null);
			} catch (IOException e) {
				Timber.i("Didn't find an image at %s", currentImageUrl);
				executeInBackground();
			}
		}
	}

	public void executeOnMainThread() {
		currentImageUrl = imageUrls.poll();
		if (TextUtils.isEmpty(currentImageUrl)) {
			if (callback != null) {
				callback.onFailedIconLoad();
			}
		} else {
			getRequestCreator().into(this);
		}
	}

	private RequestCreator getRequestCreator() {
		return Picasso.with(context.getApplicationContext())
			.load(HttpUtils.ensureScheme(currentImageUrl))
			.resize(WEARABLE_ICON_SIZE, WEARABLE_ICON_SIZE)
			.centerCrop();
	}

	@Override
	public void onBitmapLoaded(Bitmap bitmap, LoadedFrom from) {
		Timber.i("Found an image at %s", currentImageUrl);
		if (callback != null) {
			callback.onSuccessfulIconLoad(bitmap);
		}
	}

	@Override
	public void onBitmapFailed(Drawable errorDrawable) {
		Timber.i("Didn't find an image at %s", currentImageUrl);
		executeOnMainThread();
	}

	@Override
	public void onPrepareLoad(Drawable placeHolderDrawable) {
	}

	public interface Callback {
		void onSuccessfulIconLoad(Bitmap bitmap);

		void onFailedIconLoad();
	}
}
