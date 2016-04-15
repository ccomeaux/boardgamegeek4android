package com.boardgamegeek.io;

import com.boardgamegeek.model.ThingResponse;

public class ThingRequest  {
	private final BggService bggService;
	private final String ids;

	public ThingRequest(BggService service, String ids) {
		this.bggService = service;
		this.ids = ids;
	}

	public ThingResponse execute() {
		return bggService.thing(ids, 1);
	}
}
