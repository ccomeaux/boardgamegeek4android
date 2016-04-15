package com.boardgamegeek.io;

import android.support.v4.util.ArrayMap;

import com.boardgamegeek.model.CollectionResponse;

public class CollectionRequest  {
	private final BggService bggService;
	private final String username;
	private final ArrayMap<String, String> options;

	public CollectionRequest(BggService service, String username, ArrayMap<String, String> options) {
		this.bggService = service;
		this.username = username;
		this.options = options;
	}

	public CollectionResponse execute() {
		return bggService.collection(username, options);
	}
}
