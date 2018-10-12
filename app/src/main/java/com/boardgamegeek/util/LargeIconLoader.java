package com.boardgamegeek.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Picasso.LoadedFrom;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;

import java.util.LinkedList;
import java.util.Queue;

import androidx.annotation.MainThread;
import androidx.annotation.WorkerThread;
import timber.log.Timber;

public class LargeIconLoader implements Target {

	private static final int WEARABLE_ICON_SIZE = 400;

	private final Context context;
	private final Callback callback;
	private final Queue<String> imageUrls;
	private String currentImageUrl;

	public LargeIconLoader(Context context, String imageUrl, String thumbnailUrl, String heroImageUrl, Callback callback) {
		this.context = context;
		this.imageUrls = new LinkedList<>();
		this.callback = callback;
		imageUrls.add(heroImageUrl);
		imageUrls.add(thumbnailUrl);
		imageUrls.add(imageUrl);
	}

	@WorkerThread
	public void executeInBackground() {
		if (imageUrls.size() == 0) {
			if (callback != null) callback.onFailedIconLoad();
			return;
		}
		currentImageUrl = imageUrls.poll();
		if (TextUtils.isEmpty(currentImageUrl)) {
			executeInBackground();
		} else {
			try {
				final Bitmap bitmap = getRequestCreator().get();
				onBitmapLoaded(bitmap, null);
			} catch (Exception e) {
				Timber.i("Didn't find an image at %s", currentImageUrl);
				executeInBackground();
			}
		}
	}

	@MainThread
	public void executeOnMainThread() {
		if (imageUrls.size() == 0) {
			if (callback != null) callback.onFailedIconLoad();
			return;
		}
		currentImageUrl = imageUrls.poll();
		if (TextUtils.isEmpty(currentImageUrl)) {
			executeOnMainThread();
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
		if (callback != null) callback.onSuccessfulIconLoad(bitmap);
	}

	@Override
	public void onBitmapFailed(Drawable errorDrawable) {
		Timber.i("Didn't find an image at %s", currentImageUrl);
		if (callback != null) callback.onFailedIconLoad();
	}

	@Override
	public void onPrepareLoad(Drawable placeHolderDrawable) {
	}

	public interface Callback {
		void onSuccessfulIconLoad(Bitmap bitmap);

		void onFailedIconLoad();
	}
}
