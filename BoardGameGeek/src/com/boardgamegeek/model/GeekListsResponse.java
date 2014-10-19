package com.boardgamegeek.model;

import java.util.List;

public class GeekListsResponse {
	public static final int PAGE_SIZE = 12;

	private List<GeekListEntry> lists;

	public List<GeekListEntry> getGeekListEntries() {
		return lists;
	}

	public int getTotalCount() {
		return 100;
	}
}
