package com.boardgamegeek.ui;


import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

public abstract class BggLoader<D> extends AsyncTaskLoader<D> {
	private D mData;

	public BggLoader(Context context) {
		super(context);
	}

	@Override
	public void deliverResult(D data) {
		if (isReset()) {
			return;
		}
		mData = data;
		if (isStarted()) {
			super.deliverResult(data);
		}
	}

	@Override
	protected void onStartLoading() {
		if (mData != null) {
			deliverResult(mData);
		}
		if (takeContentChanged() || mData == null) {
			forceLoad();
		}
	}

	@Override
	protected void onStopLoading() {
		cancelLoad();
	}

	@Override
	protected void onReset() {
		super.onReset();
		onStopLoading();
		mData = null;
	}
}
