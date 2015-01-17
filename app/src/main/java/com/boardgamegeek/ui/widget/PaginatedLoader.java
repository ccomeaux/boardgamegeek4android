package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

public abstract class PaginatedLoader<T> extends AsyncTaskLoader<PaginatedData<T>> {
	private PaginatedData<T> mData;
	private boolean mIsLoading;

	public PaginatedLoader(Context context) {
		super(context);
		mIsLoading = true;
		mData = null;
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
	public PaginatedData<T> loadInBackground() {
		mIsLoading = true;
		return null;
	}

	protected int getNextPage() {
		return (mData == null ? 1 : mData.getNextPage());
	}

	@Override
	public void deliverResult(PaginatedData<T> data) {
		mIsLoading = false;
		if (data != null) {
			if (mData == null) {
				mData = data;
			} else if (data.getCurrentPage() == mData.getNextPage()) {
				mData.addAll(data.getData());
			}
		}
		if (isStarted()) {
			super.deliverResult(new PaginatedData<T>(mData));
		}
	}

	@Override
	protected void onStopLoading() {
		mIsLoading = false;
		cancelLoad();
	}

	@Override
	protected void onReset() {
		super.onReset();
		onStopLoading();
		mData = null;
	}

	public boolean isLoading() {
		return mIsLoading;
	}

	public boolean hasMoreResults() {
		return mData == null || mData.hasMoreResults();
	}
}
