package com.boardgamegeek.io;

import com.boardgamegeek.model.User;

public class UserRequest {
	private final BggService bggService;
	private final String name;

	public UserRequest(BggService service, String name) {
		this.bggService = service;
		this.name = name;
	}

	public User execute() {
		return bggService.user(name);
	}
}
