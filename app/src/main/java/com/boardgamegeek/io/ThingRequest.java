package com.boardgamegeek.io;

import com.boardgamegeek.model.ThingResponse;

public class ThingRequest extends RetryableRequest<ThingResponse> {
	private final String ids;

	public ThingRequest(BggService service, String ids) {
		super(service);
		this.ids = ids;
	}

	@Override
	protected ThingResponse request() {
		return bggService.thing(ids, 1);
	}

	@Override
	protected long getMinWaitTime() {
		return 500L;
	}

	@Override
	protected int getMaxRetries() {
		return 5;
	}
}
