package com.boardgamegeek.io;

import com.boardgamegeek.model.CollectionResponse;

import java.util.HashMap;

public class BuddyCollectionRequest extends RetryableRequest<CollectionResponse> {
	private final String username;
	private final HashMap<String, String> options;

	public BuddyCollectionRequest(BggService service, String username, HashMap<String, String> options) {
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
		return 200L;
	}

	@Override
	protected int getMaxRetries() {
		return 5;
	}
}
