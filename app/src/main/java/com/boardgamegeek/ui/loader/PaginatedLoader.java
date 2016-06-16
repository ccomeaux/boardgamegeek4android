package com.boardgamegeek.ui.loader;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import hugo.weaving.DebugLog;

public abstract class PaginatedLoader<T> extends AsyncTaskLoader<PaginatedData<T>> {
	private PaginatedData<T> data;
	private boolean isLoading;

	@DebugLog
	public PaginatedLoader(Context context) {
		super(context);
		isLoading = true;
		data = null;
	}

	@DebugLog
	@Override
	protected void onStartLoading() {
		if (data != null) {
			deliverResult(data);
		}
		if (takeContentChanged() || data == null) {
			forceLoad();
		}
	}

	@DebugLog
	@Override
	public PaginatedData<T> loadInBackground() {
		isLoading = true;
		return fetchPage(getNextPageNumber());
	}

	@DebugLog
	protected int getNextPageNumber() {
		return data == null ? 1 : data.getNextPageNumber();
	}

	@DebugLog
	@Override
	public void deliverResult(PaginatedData<T> data) {
		isLoading = false;
		if (data != null) {
			if (this.data == null) {
				this.data = data;
			} else if (data.getCurrentPageNumber() == this.data.getNextPageNumber()) {
				this.data.addPage(data.getItems());
			}
		}
		if (isStarted()) {
			super.deliverResult(new PaginatedData<>(this.data));
		}
	}

	@DebugLog
	@Override
	protected void onStopLoading() {
		isLoading = false;
		cancelLoad();
	}

	@DebugLog
	@Override
	protected void onReset() {
		super.onReset();
		onStopLoading();
		data = null;
	}

	@DebugLog
	public boolean isLoading() {
		return isLoading;
	}

	@DebugLog
	public boolean hasMoreResults() {
		return data != null && data.hasMoreResults();
	}

	protected abstract PaginatedData<T> fetchPage(int pageNumber);
}
