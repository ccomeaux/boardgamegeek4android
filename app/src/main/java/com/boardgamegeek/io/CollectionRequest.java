package com.boardgamegeek.io;

import android.support.v4.util.ArrayMap;

import com.boardgamegeek.model.CollectionResponse;

public class CollectionRequest extends RetryableRequest<CollectionResponse> {
	private final String username;
	private final ArrayMap<String, String> options;

	public CollectionRequest(BggService service, String username, ArrayMap<String, String> options) {
		super(service);
		this.username = username;
		this.options = options;
	}

	@Override
	protected CollectionResponse request() {
		return mService.collection(username, options);
	}

	@Override
	protected long getMinWaitTime() {
		return 500L;
	}

	@Override
	protected long getMaxWaitTime() {
		return 120000L;
	}
}
