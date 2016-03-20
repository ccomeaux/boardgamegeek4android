package com.boardgamegeek.io;

import com.boardgamegeek.model.User;

public class UserRequest extends RetryableRequest<User> {
	private final String name;

	public UserRequest(BggService service, String name) {
		super(service);
		this.name = name;
	}

	@Override
	protected User request() {
		return bggService.user(name);
	}

	@Override
	protected long getMinWaitTime() {
		return 5000L;
	}

	@Override
	protected int getMaxRetries() {
		return 4;
	}
}
