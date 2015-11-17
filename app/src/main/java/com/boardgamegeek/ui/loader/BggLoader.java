package com.boardgamegeek.ui.loader;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

public abstract class BggLoader<D> extends AsyncTaskLoader<D> {
	private D data;

	public BggLoader(Context context) {
		super(context);
	}

	@Override
	protected void onStartLoading() {
		// deliver the data if we have it
		if (data != null) {
			deliverResult(data);
		}
		// ask for data if it has changed or is missing
		if (takeContentChanged() || data == null) {
			forceLoad();
		}
	}

	@Override
	public void deliverResult(D data) {
		// loader has been reset so doesn't need to deliver any data
		if (isReset()) {
			return;
		}
		// save the data to deliver later
		this.data = data;
		// loader is started, so the data should be delivered
		if (isStarted()) {
			super.deliverResult(data);
		}
	}

	@Override
	protected void onReset() {
		super.onReset();
		onStopLoading();
		data = null;
	}

	@Override
	protected void onStopLoading() {
		// we're being asked to stop, so cancel the task
		cancelLoad();
	}
}
